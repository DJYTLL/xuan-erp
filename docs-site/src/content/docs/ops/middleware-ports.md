---
title: "duaoyunxuan.com 中间件端口"
---

本文记录 `duaoyunxuan.com` 服务器上的 Xuan ERP 中间件对外端口约定。

本页只描述 `duaoyunxuan.com` 这一台服务器，不覆盖 NAS 服务器和其它远程服务器已有端口记录。不同服务器可以有不同端口映射；查端口时必须先确认目标服务器。

端口分层规则：

- `902x` 主要用于控制台、Dashboard、Web 管理入口。
- `903x` 用于辅助管理入口。
- `904x` 主要用于 API、服务调用、数据库、缓存、消息中间件等程序访问。
- `905x` 用于 APM、链路采集和监控后端程序访问。
- `9848` 是 Nacos gRPC 通信端口。

应用配置里要优先使用服务调用端口，不要把控制台端口写成业务服务连接地址。

## 端口总览

| 对外端口 | 协议 / 类型 | 用途 | 容器 / 服务 |
| --- | --- | --- | --- |
| `9020` | HTTP | Nacos Console | `xuan-nacos` |
| `9021` | HTTP | Seata Namingserver / Console | `xuan-erp-seata` |
| `9022` | HTTP | Sentinel Dashboard | `xuan-erp-sentinel` |
| `9024` | HTTP | Kibana | `xuan-erp-kibana` |
| `9025` | HTTP | RocketMQ Dashboard | `xuan-erp-rocketmq-dashboard` |
| `9026` | HTTP | SkyWalking UI | `xuan-erp-skywalking-ui` |
| `9031` | HTTP | Portainer | `xuan-erp-portainer` |
| `9032` | HTTP | DDNS-GO | `xuan-erp-ddns-go` |
| `9040` | HTTP | Elasticsearch API / SkyWalking APM 存储访问 | `xuan-erp-es` |
| `9041` | HTTP/API | Nacos API / 客户端连接 | `xuan-nacos` |
| `9042` | PostgreSQL | PostgreSQL / `pg_stat_statements` SQL 统计 | `xuan-pgsql` |
| `9043` | SSH | SSH Remote Access | `systemd:ssh.socket` |
| `9045` | TCP | Seata TC 事务服务 | `xuan-erp-seata` |
| `9046` | Redis | Redis | `xuan-erp-redis` |
| `9047` | RocketMQ | RocketMQ NameServer | `xuan-erp-rocketmq-namesrv` |
| `9048` | RocketMQ | RocketMQ Broker | `xuan-erp-rocketmq-broker` |
| `9049` | RocketMQ HA | RocketMQ Broker HA | `xuan-erp-rocketmq-broker` |
| `9050` | gRPC | SkyWalking OAP gRPC / Java Agent 上报 | `xuan-erp-skywalking-oap` |
| `9051` | HTTP | SkyWalking OAP HTTP | `xuan-erp-skywalking-oap` |
| `9848` | gRPC | Nacos gRPC 通信 | `xuan-nacos` |

## SkyWalking 端口

SkyWalking 已纳入 Xuan ERP 性能监控方案，`duaoyunxuan.com` 对外端口约定如下：

| 组件 | 容器内端口 | 对外端口 | 用途 |
| --- | --- | --- | --- |
| SkyWalking UI | `8080` | `9026` | APM 控制台 |
| SkyWalking OAP gRPC | `11800` | `9050` | Java Agent 上报 trace、指标和调用链 |
| SkyWalking OAP HTTP | `12800` | `9051` | OAP HTTP 接入 / 查询 |

容器内服务优先使用 Docker 网络内地址，例如 `xuan-erp-skywalking-oap:11800`。从服务器外部接入时，Java Agent 使用 `duaoyunxuan.com:9050`。

## 常用访问地址

控制台入口：

```text
Nacos Console:         http://duaoyunxuan.com:9020
Seata Console:         http://duaoyunxuan.com:9021
Sentinel Dashboard:    http://duaoyunxuan.com:9022
Kibana:                http://duaoyunxuan.com:9024
RocketMQ Dashboard:    http://duaoyunxuan.com:9025
SkyWalking UI:         http://duaoyunxuan.com:9026
Portainer:             http://duaoyunxuan.com:9031
DDNS-GO:               http://duaoyunxuan.com:9032
```

程序连接地址：

```text
Nacos API:             duaoyunxuan.com:9041
Nacos gRPC:            duaoyunxuan.com:9848
PostgreSQL:            duaoyunxuan.com:9042
Seata TC:              duaoyunxuan.com:9045
Redis:                 duaoyunxuan.com:9046
Elasticsearch API:     http://duaoyunxuan.com:9040
RocketMQ NameServer:   duaoyunxuan.com:9047
RocketMQ Broker:       duaoyunxuan.com:9048
RocketMQ Broker HA:    duaoyunxuan.com:9049
SkyWalking OAP gRPC:   duaoyunxuan.com:9050
SkyWalking OAP HTTP:   http://duaoyunxuan.com:9051
```

## 配置建议

本地开发或从 `duaoyunxuan.com` 服务器外部访问这台服务器的中间件时，可以按对外端口连接：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: duaoyunxuan.com:9041
      config:
        server-addr: duaoyunxuan.com:9041
  data:
    redis:
      host: duaoyunxuan.com
      port: 9046
  elasticsearch:
    uris:
      - http://duaoyunxuan.com:9040
```

SkyWalking Java Agent 从 `duaoyunxuan.com` 外部连接这台服务器时使用：

```text
-Dskywalking.collector.backend_service=duaoyunxuan.com:9050
```

容器内服务优先使用 Docker 网络内的 OAP 服务名。

容器内服务之间调用时，优先使用 Docker 网络内的服务名和容器内部端口，避免绕到公网域名。

示例：

```text
duaoyunxuan.com:9041
xuan-erp-seata:8091
xuan-erp-redis:6379
xuan-erp-es:9200
xuan-erp-rocketmq-namesrv:9876
xuan-erp-skywalking-oap:11800
```

SkyWalking Docker Compose 片段：

```yaml
services:
  skywalking-oap:
    image: apache/skywalking-oap-server:10.1.0
    container_name: xuan-erp-skywalking-oap
    restart: unless-stopped
    environment:
      TZ: Asia/Shanghai
      SW_STORAGE: elasticsearch
      SW_STORAGE_ES_CLUSTER_NODES: xuan-erp-es:9200
      SW_HEALTH_CHECKER: default
      JAVA_OPTS: >-
        -Xms512m
        -Xmx1024m
    ports:
      - "9050:11800"
      - "9051:12800"
    networks:
      - xuan-erp

  skywalking-ui:
    image: apache/skywalking-ui:10.1.0
    container_name: xuan-erp-skywalking-ui
    restart: unless-stopped
    depends_on:
      - skywalking-oap
    environment:
      TZ: Asia/Shanghai
      SW_OAP_ADDRESS: http://skywalking-oap:12800
    ports:
      - "9026:8080"
    networks:
      - xuan-erp

networks:
  xuan-erp:
    name: xuan-erp
    external: true
```

## 注意事项

- 本页端口只适用于 `duaoyunxuan.com` 服务器。
- NAS 服务器、其它远程服务器的 Nacos / Seata / Redis / ES 端口以各自部署记录为准。
- `9020` 是 Nacos 控制台，不是 Nacos 客户端连接端口。
- `9041` 是 Nacos API / 客户端连接端口。
- `9848` 是 Nacos gRPC 通信端口，Nacos 3.x 客户端可能需要连通。
- `9021` 是 Seata Console，业务服务参与事务时不要连接这个端口。
- `9045` 是 Seata TC 事务服务端口。
- `9040` 是 Elasticsearch API，Kibana 控制台是 `9024`。
- `9040` 也可作为 SkyWalking 连接 Elasticsearch 存储的外部 API 端口，但生产部署中 SkyWalking OAP 优先走内网连接 ES。
- PostgreSQL `pg_stat_statements` 不单独占用端口，通过 PostgreSQL `9042` 连接后查询。
- `9026` 是 SkyWalking UI 控制台。
- `9050` 是 SkyWalking OAP gRPC 端口，Java Agent 上报优先使用这个端口。
- `9051` 是 SkyWalking OAP HTTP 端口。
- `9025` 是 RocketMQ Dashboard，业务服务发消息时使用 NameServer `9047`。
- `9048` / `9049` 是 RocketMQ Broker 相关端口，一般由 Broker 和集群内部使用。
- `9042`、`9043`、`9046`、`9047`、`9048`、`9049` 不建议对全网开放，生产应限制来源 IP 或走内网 / VPN。
