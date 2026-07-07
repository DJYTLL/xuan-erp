---
title: "部署流程"
---

本文记录微服务部署顺序、环境配置、数据库迁移和发布验证。

`duaoyunxuan.com` 服务器对外端口约定见：[duaoyunxuan.com 中间件端口](/ops/middleware-ports/)。NAS 服务器和其它远程服务器端口以各自部署记录为准，不互相覆盖。

二级域名、DDNS-GO、CNAME 和反向代理后续待办见：[域名与反向代理待办](/ops/domain-reverse-proxy/)。

备份脚本、每日 cron、PostgreSQL 恢复验证、Nacos / Redis / Elasticsearch / 配置文件备份边界见：[备份与恢复](/ops/backup-restore/)。

## 基础设施顺序

生产和测试环境建议先部署基础设施，再部署业务服务：

1. Nacos。
2. Seata Namingserver 和 Seata Server。
3. Sentinel Dashboard。
4. RocketMQ。
5. Redis。
6. Elasticsearch。
7. SkyWalking、PostgreSQL `pg_stat_statements`、日志和链路追踪。
8. `xuan-iam`、`xuan-gateway` 和业务服务。

## Redis 与 Elasticsearch 部署约定

Redis 和 Elasticsearch 都作为正式基础设施部署，不作为临时可选组件。

| 组件 | 用途 | 关键要求 |
| --- | --- | --- |
| Redis | 缓存、幂等键、短期状态、热点读优化 | 开启持久化、密码认证、内网访问，不直接暴露公网 |
| Elasticsearch | 搜索索引、全文检索、审计日志检索 | 独立数据目录、内网访问、按租户和删除状态建过滤字段 |

业务服务不直接把 Redis 或 Elasticsearch 暴露给前端。页面查询统一通过 Gateway 后面的业务服务或 `xuan-query`。

`duaoyunxuan.com` 服务器当前对外约定中，Redis 使用 `9046`，Elasticsearch API 使用 `9040`，Kibana 控制台使用 `9024`。其它服务器按各自端口记录配置。

详细接入方式见：[Redis 与 Elasticsearch 准备](/guide/cache-search-setup/)。

## 监控部署约定

接口耗时、服务间调用耗时和单次请求内 SQL 耗时通过 SkyWalking 观察；数据库侧 SQL 排名和长期统计通过 PostgreSQL `pg_stat_statements` 观察。

| 组件 | 用途 | 关键要求 |
| --- | --- | --- |
| SkyWalking OAP | 接收 Java Agent 上报的 trace、指标和拓扑数据 | 内网访问，连接独立的 Elasticsearch 存储 |
| SkyWalking UI | 查询接口耗时、链路拓扑、慢 SQL span | `duaoyunxuan.com` 对外端口 `9026` |
| Elasticsearch | SkyWalking APM 存储 | APM 索引与业务搜索索引命名隔离 |
| PostgreSQL `pg_stat_statements` | 数据库侧 SQL 统计 | PostgreSQL 启用扩展，定期查看总耗时、平均耗时和最大耗时排行 |

业务服务统一通过 SkyWalking Java Agent 接入，不要求在业务代码中大量手写埋点。

`duaoyunxuan.com` 服务器 SkyWalking 对外端口：

| 端口 | 用途 |
| --- | --- |
| `9026` | SkyWalking UI |
| `9050` | SkyWalking OAP gRPC，Java Agent 上报 |
| `9051` | SkyWalking OAP HTTP |

详细方案见：[监控](/ops/monitoring/)。

## PostgreSQL 数据库准备

业务服务采用每服务独立数据库。`duaoyunxuan.com` 服务器当前 PostgreSQL 对外连接端口是 `9042`，SSH 管理端口是 `9043`，不要混用。

当前远端已准备 14 个服务数据库：

```text
xuan_gateway
xuan_tenant
xuan_iam
xuan_audit
xuan_product
xuan_party
xuan_warehouse
xuan_inventory
xuan_sales
xuan_procurement
xuan_finance
xuan_document
xuan_manufacturing
xuan_query
```

部署流程中的数据库边界：

- 部署前可以先创建数据库和授权。
- 不在远端手工建业务表。
- 不直接执行散落 SQL 建表。
- 表结构、约束、索引、初始化数据统一由各服务 Flyway migration 执行。
- Nacos 中每个服务的 datasource 必须指向自己的数据库。
- 如果部署脚本、Compose、Nacos seed 仍指向旧库，例如 `wms_backend` 或旧 NAS PostgreSQL 地址，必须先修正再部署。

连接格式：

```text
jdbc:postgresql://duaoyunxuan.com:9042/xuan_sales
```

密码不写入正式文档和仓库。生产环境通过服务器本地 `.env`、密钥管理或 Nacos 加密能力注入。

## Nacos 初始化状态

`duaoyunxuan.com` 服务器上的 Nacos 已完成 Xuan ERP 初始化。

| 项 | 当前值 |
| --- | --- |
| 控制台端口 | `9020` |
| API / 客户端端口 | `9041` |
| 项目 namespace | `dev`、`prod` |
| 项目 group | `XUAN_ERP_GROUP` |
| 每个 namespace 配置数量 | 56 |

已初始化 `xuan-common.yaml` 和 14 个服务配置 dataId。配置正文只放非敏感运行配置和 datasource 骨架，密码、密钥、证书继续通过服务器 `.env`、密钥管理或加密能力注入。

已初始化 Sentinel 规则 dataId，普通业务服务包含 `flow`、`degrade`、`param-flow` 三类规则，`xuan-gateway` 包含 `gw-flow`、`gw-api-group` 两类规则。初始内容均为 `[]`。服务是否实际加载这些规则，取决于对应服务配置 dataId 是否已加入 `spring.cloud.sentinel.datasource`。

容器内业务服务访问 PostgreSQL 时使用 `postgres:5432`；本地开发机或外部客户端访问远端 PostgreSQL 时使用 `duaoyunxuan.com:9042`。

## Seata 部署约定

Seata 用于管理微服务之间的同步分布式事务。

Xuan ERP 约定：

| 项 | 值 |
| --- | --- |
| Seata Namingserver / Console 主机 | 与 Nacos 相同 |
| Seata Namingserver / Console 对外端口 | `9021` |
| Console 示例地址 | `duaoyunxuan.com:9021` |
| Seata Server 容器内事务端口 | `8091` |
| Seata Server 对外事务端口 | `9045` |
| Seata Server 内网服务名示例 | `xuan-seata:8091` |
| Nacos 注册服务名 | `seata-server` |
| group | `XUAN_ERP_GROUP` |

如果 Nacos 地址是 `duaoyunxuan.com:9041`，则 Seata Namingserver / Console 对外地址是同一主机的 `duaoyunxuan.com:9021`。

补充说明：

- 浏览器访问的是 `9021`，这是 Namingserver / Console 入口。
- 业务服务在容器内事务协调访问的是 `8091`，这是 Seata Server TC 服务端口。
- 外部客户端访问 `duaoyunxuan.com` 这台服务器上的 Seata TC 时使用 `duaoyunxuan.com:9045`。
- Seata 2.4 之后，控制台已迁移到 Namingserver，不再直接挂在 `seata-server` 上。

业务服务接入方式见：[Seata 准备](/guide/seata-setup/)。



