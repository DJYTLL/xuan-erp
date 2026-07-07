---
title: "域名与反向代理待办"
---

本文记录 `duaoyunxuan.com` 后续接入二级域名和反向代理的方案。当前阶段先作为部署待办记录，未完成前仍以端口访问为准。

## 目标

后续将浏览器访问类入口统一收口到二级域名：

```text
docs.duaoyunxuan.com
nacos.duaoyunxuan.com
skywalking.duaoyunxuan.com
kibana.duaoyunxuan.com
api.duaoyunxuan.com
```

目标是让外部访问只经过 `80 / 443`，由 Nginx、Caddy 或其它反向代理按 Host 转发到内部端口，减少直接暴露后台端口。

## DNS 方案

当前 `duaoyunxuan.com` 由 DDNS-GO 自动更新公网 IP。

推荐做法：

```text
duaoyunxuan.com              A      当前公网 IP，由 DDNS-GO 自动更新
docs.duaoyunxuan.com         CNAME  duaoyunxuan.com
nacos.duaoyunxuan.com        CNAME  duaoyunxuan.com
skywalking.duaoyunxuan.com   CNAME  duaoyunxuan.com
kibana.duaoyunxuan.com       CNAME  duaoyunxuan.com
api.duaoyunxuan.com          CNAME  duaoyunxuan.com
```

这样 DDNS-GO 只需要维护主域名 `duaoyunxuan.com`，二级域名通过 CNAME 间接跟随主域名的公网 IP。

前期不建议直接使用泛解析 `*.duaoyunxuan.com`，先显式维护需要的二级域名，避免误暴露未规划入口。

## 反向代理原理

DNS 只负责把域名解析到服务器。请求到达服务器后，Nginx / Caddy 根据 HTTP 请求头中的 `Host` 判断要转发到哪个服务。

示例：

```text
skywalking.duaoyunxuan.com
  -> DNS 解析到服务器公网 IP
  -> 请求进入服务器 80 / 443
  -> 反向代理读取 Host: skywalking.duaoyunxuan.com
  -> 转发到 127.0.0.1:9026
```

## 推荐映射

| 二级域名 | 转发目标 | 说明 |
| --- | --- | --- |
| `docs.duaoyunxuan.com` | `127.0.0.1:3000` 或静态文档目录 | 文档站 |
| `nacos.duaoyunxuan.com` | `127.0.0.1:9020` | Nacos Console |
| `skywalking.duaoyunxuan.com` | `127.0.0.1:9026` | SkyWalking UI |
| `kibana.duaoyunxuan.com` | `127.0.0.1:9024` | Kibana |
| `api.duaoyunxuan.com` | `xuan-gateway` 对外端口 | 业务 API 网关 |

程序调用端口不建议都改成二级域名。下面这些仍优先使用内网、VPN、IP 白名单或当前端口记录：

```text
Nacos API: duaoyunxuan.com:9041
PostgreSQL: duaoyunxuan.com:9042
Redis: duaoyunxuan.com:9046
RocketMQ NameServer: duaoyunxuan.com:9047
SkyWalking OAP gRPC: duaoyunxuan.com:9050
```

## Nginx 示例

```nginx
server {
    listen 80;
    server_name skywalking.duaoyunxuan.com;

    location / {
        proxy_pass http://127.0.0.1:9026;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name nacos.duaoyunxuan.com;

    location / {
        proxy_pass http://127.0.0.1:9020;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## HTTPS

后续建议统一启用 HTTPS。

可选方案：

- 单独申请每个二级域名证书。
- 申请通配符证书 `*.duaoyunxuan.com`。
- 使用 Caddy 自动申请和续期证书。
- 使用 Nginx + Certbot / acme.sh 管理证书。

如果使用通配符证书，通常需要 DNS API 权限完成 DNS-01 验证。

## 安全边界

- 控制台入口不要裸奔公网，至少加 IP 白名单、Basic Auth、VPN 或其它访问控制。
- Nacos、Kibana、SkyWalking、RocketMQ Dashboard 等后台入口不建议长期无保护开放。
- 数据库、Redis、MQ、APM 上报端口不建议通过二级域名暴露给浏览器访问。
- 对外只优先开放 `80 / 443`，其它端口逐步收紧来源 IP。

## 后续待办

- [ ] 确认 DNS 服务商和 DDNS-GO 当前更新的记录类型。
- [ ] 添加 `docs / nacos / skywalking / kibana / api` CNAME 记录。
- [ ] 选择反向代理方案：Nginx 或 Caddy。
- [ ] 编写反向代理配置。
- [ ] 确认是否使用 HTTPS，以及证书申请方式。
- [ ] 为控制台入口增加访问控制。
- [ ] 验证每个二级域名能正确转发。
- [ ] 逐步收紧 `902x / 904x / 905x` 直接公网访问。
