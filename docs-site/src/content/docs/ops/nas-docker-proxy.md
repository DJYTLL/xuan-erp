---
title: "NAS Docker 代理配置"
---

本文记录群晖 NAS 上 Docker 拉取镜像时如何走本机 mihomo 代理，以及如何取消代理配置。当前目标是解决 `docker pull`、Container Manager 拉取镜像、登录镜像仓库等外部访问较慢或失败的问题。

## 当前结构

当前 NAS 代理相关服务分成两部分：

| 组件 | 地址或端口 | 作用 |
| --- | --- | --- |
| MetaCubeXD 控制台 | `http://192.168.2.41:9030/` | 查看代理连接、切换节点、查看规则命中 |
| 代理管理页 | `http://192.168.2.41:9030/admin` | 控制 NAS 上 mihomo 代理核心和 Docker 拉镜像代理 |
| mihomo mixed 代理入口 | `127.0.0.1:7890` | 给 Docker daemon 或其他程序使用的 HTTP/SOCKS 代理入口 |
| mihomo controller | `127.0.0.1:9090` | 给控制台读取状态和控制代理核心 |

这里的 Docker 代理配置只影响 Docker daemon 本身，例如拉镜像、登录 registry、访问镜像仓库。它不会自动让已经运行的业务容器全部走代理。

## 通过 9030/admin 管理

当前 `http://192.168.2.41:9030/admin` 已经提供两个管理区域：

| 区域 | 能力 |
| --- | --- |
| 代理核心 | 启动、重启、关闭 `nas-mihomo-native.service` |
| Docker 拉镜像代理 | 查看 Docker daemon 代理状态、启用并重启 Docker、禁用并重启 Docker、单独重启 Docker |

Docker 代理区域显示这些状态：

- Docker 代理配置是否存在。
- Docker daemon 是否 `active`。
- 当前 `HTTP_PROXY` / `HTTPS_PROXY`。
- 当前 `NO_PROXY`。

点击“启用并重启 Docker”会写入固定的 systemd drop-in 配置，并重启 `pkg-ContainerManager-dockerd.service`。点击“禁用并重启 Docker”会删除固定配置文件，并重启 Docker daemon。

网页端没有保存 sudo 密码，也没有开放任意命令。9030 后端只允许通过 sudoers 执行固定的 root-owned 脚本：

```text
/usr/local/bin/nas-docker-proxy-enable
/usr/local/bin/nas-docker-proxy-disable
/usr/local/bin/nas-docker-proxy-restart
```

## 手动开启 Docker 拉镜像代理

先确认 `9030/admin` 中的 NAS 代理核心处于开启状态。如果 mihomo 没有运行，Docker daemon 即使配置了代理，也无法通过 `127.0.0.1:7890` 出网。

在 NAS 上执行：

```bash
sudo mkdir -p /etc/systemd/system/pkg-ContainerManager-dockerd.service.d
```

创建或覆盖文件：

```bash
sudo vi /etc/systemd/system/pkg-ContainerManager-dockerd.service.d/proxy.conf
```

写入：

```ini
[Service]
Environment="HTTP_PROXY=http://127.0.0.1:7890"
Environment="HTTPS_PROXY=http://127.0.0.1:7890"
Environment="NO_PROXY=localhost,127.0.0.1,::1,192.168.0.0/16,172.16.0.0/12,10.0.0.0/8,*.local"
```

然后重载 systemd 并重启 Docker daemon：

```bash
sudo systemctl daemon-reload
sudo systemctl restart pkg-ContainerManager-dockerd.service
```

重启 Docker daemon 时，群晖 Container Manager 可能会短暂不可用，部分容器也可能受到短暂影响。群晖恢复有时需要等待几分钟，看到服务重新进入 `active/running` 后再继续操作。

## 验证是否生效

查看 Docker daemon 当前代理配置：

```bash
sudo docker info | grep -Ei 'HTTP Proxy|HTTPS Proxy|No Proxy'
```

正常情况下可以看到类似内容：

```text
HTTP Proxy: http://127.0.0.1:7890
HTTPS Proxy: http://127.0.0.1:7890
No Proxy: localhost,127.0.0.1,::1,192.168.0.0/16,172.16.0.0/12,10.0.0.0/8,*.local
```

再拉取一个测试镜像：

```bash
sudo docker pull busybox:uclibc
```

同时打开 `http://192.168.2.41:9030/` 的连接页面，观察是否出现 Docker、registry、镜像源或相关域名连接。如果能看到相关连接，说明 Docker 拉镜像请求已经进入代理路径。

## 手动取消 Docker 拉镜像代理

删除 systemd drop-in 配置：

```bash
sudo rm -f /etc/systemd/system/pkg-ContainerManager-dockerd.service.d/proxy.conf
```

重载并重启 Docker daemon：

```bash
sudo systemctl daemon-reload
sudo systemctl restart pkg-ContainerManager-dockerd.service
```

再次检查：

```bash
sudo docker info | grep -Ei 'HTTP Proxy|HTTPS Proxy|No Proxy'
```

如果不再显示 `HTTP Proxy`、`HTTPS Proxy`，说明 Docker daemon 已恢复为不走代理。

## 不建议直接改 dockerd.json

这台群晖当前更稳妥的方式是 systemd drop-in 环境变量，也就是 `/etc/systemd/system/pkg-ContainerManager-dockerd.service.d/proxy.conf`。

不建议优先修改这些文件来配置代理：

- `/etc/docker/daemon.json`
- `/var/packages/ContainerManager/etc/dockerd.json`

原因是群晖 Container Manager 对 Docker daemon 的启动参数和配置文件有自己的管理方式。直接在 `dockerd.json` 中写 `proxies` 字段，可能导致 Docker daemon 启动变慢、卡在启动中，甚至影响 Container Manager 恢复。

## 代理关闭后的影响

如果 Docker daemon 代理配置仍然存在，但 `9030/admin` 中关闭了 mihomo 代理核心，那么 Docker 拉镜像时仍会尝试连接 `127.0.0.1:7890`。

这种情况下：

| 场景 | 结果 |
| --- | --- |
| Docker 拉外网镜像 | 大概率失败，因为本机代理入口不可用 |
| Docker 拉内网镜像 | 如果命中 `NO_PROXY`，不受影响 |
| 已经运行的业务容器 | 通常不受这个 Docker daemon 代理配置影响 |
| 容器内程序访问外网 | 取决于容器自己的环境变量、应用配置或网络配置 |

也就是说，这个设置主要影响“Docker 管理端怎么访问镜像仓库”，不是 NAS 全局透明代理。

## 9030/admin 的安全边界

`9030/admin` 当前只做固定动作，不做通用 NAS 管理终端：

- 只读取固定的 Docker systemd drop-in 配置文件。
- 只写入或删除 `/etc/systemd/system/pkg-ContainerManager-dockerd.service.d/proxy.conf`。
- 只允许重启 `pkg-ContainerManager-dockerd.service`。
- 不允许用户输入任意命令。
- 不允许用户输入任意文件路径。
- 不在网页端保存或传输 sudo 密码。

如果后续要加“拉取测试镜像”按钮，也应继续使用固定脚本，并限制默认测试镜像，避免变成任意命令执行入口。

## 推荐日常用法

平时保持：

1. `9030/admin` 中 mihomo 代理核心开启。
2. Docker daemon 代理开启。
3. 拉镜像或更新容器后，通过 `9030` 连接页面确认是否有 registry 相关连接。

如果之后不想让 Docker 拉镜像走代理，就只取消 Docker daemon 的代理配置，不需要关闭整个 mihomo。这样 9030 控制台和其他可能依赖代理的程序仍然可以继续使用。
