---
title: "监控"
---

本文记录 Xuan ERP 的接口耗时、链路追踪、SQL 耗时和告警方案。

## 目标

监控系统必须能回答四个问题：

- 哪个接口慢。
- 一次请求慢在哪个服务、哪个外部调用或哪条 SQL。
- 哪些 SQL 在全库范围内总耗时最高、平均耗时最高或调用次数最多。
- 慢接口、慢 SQL 和错误是否能被持续发现，而不是只靠人工排查。

## 当前选型

Xuan ERP 当前采用：

```text
SkyWalking + Elasticsearch + PostgreSQL pg_stat_statements
```

职责划分：

| 组件 | 职责 | 说明 |
| --- | --- | --- |
| SkyWalking Java Agent | 应用侧自动埋点 | 采集接口、服务间调用、数据库访问、Redis、MQ、ES 等调用耗时 |
| SkyWalking OAP | APM 后端 | 接收 agent 数据，聚合链路、指标和拓扑 |
| Elasticsearch | SkyWalking 存储 | 存储 trace、指标、拓扑、告警等 APM 数据 |
| SkyWalking UI | 查询入口 | 查看接口耗时、调用链、服务拓扑、慢 SQL span |
| PostgreSQL `pg_stat_statements` | 数据库侧 SQL 统计 | 统计全库 SQL 调用次数、总耗时、平均耗时、最慢耗时等 |

## 监控分层

### 1. 接口与调用链监控

接口耗时优先通过 SkyWalking 采集。

需要关注：

- HTTP 接口平均耗时、P95、P99。
- 错误率和异常堆栈。
- Gateway 到业务服务的调用耗时。
- Feign / HTTP 服务间调用耗时。
- Redis、RocketMQ、Elasticsearch 等中间件调用耗时。
- Controller、Service、Mapper、数据库访问之间的耗时分布。

排查慢请求时，优先看 SkyWalking trace：

```text
Gateway -> xuan-sales -> xuan-product -> PostgreSQL
                         -> Redis
                         -> RocketMQ
```

如果 trace 显示 SQL span 慢，再进入 PostgreSQL 侧看全局 SQL 统计和执行计划。

### 2. SQL 耗时监控

SQL 耗时分两层看：

| 层级 | 工具 | 用途 |
| --- | --- | --- |
| 单次请求内 SQL | SkyWalking | 看某一次请求中具体哪条 SQL 慢 |
| 全库 SQL 排名 | `pg_stat_statements` | 看长期最耗时、最频繁、最慢的 SQL |

`pg_stat_statements` 适合做：

- 总耗时最高 SQL 排名。
- 平均耗时最高 SQL 排名。
- 调用次数最高 SQL 排名。
- 最大耗时异常 SQL 排名。
- SQL 调优前后对比。

示例查询方向：

```sql
select
  query,
  calls,
  total_exec_time,
  mean_exec_time,
  max_exec_time,
  rows
from pg_stat_statements
order by total_exec_time desc
limit 20;
```

注意：`pg_stat_statements` 是数据库侧统计，能看到 SQL 形态和耗时，但不知道它来自哪个业务请求；SkyWalking 能看到一次请求里的 SQL span，但不适合替代全库 SQL 排名。两者需要一起使用。

### 3. 业务动作耗时

ERP 中部分业务动作需要单独观察：

- 销售单保存耗时。
- 销售单审核耗时。
- 库存扣减耗时。
- 采购入库耗时。
- 打印生成耗时。
- 导入解析耗时。
- 批量同步耗时。

这类动作建议在 Service 层增加业务日志或业务指标，记录：

```text
tenantId
userId
businessType
businessId
action
costMs
success
traceId
```

业务日志必须带 `traceId`，方便从日志跳回 SkyWalking trace。

## 接入要求

### 后端服务

所有后端微服务默认接入 SkyWalking Java Agent。业务代码不应该为了接入 APM 大量侵入式改造。

启动参数示例：

```bash
java \
  -javaagent:/opt/skywalking/agent/skywalking-agent.jar \
  -Dskywalking.agent.service_name=xuan-sales \
  -Dskywalking.collector.backend_service=xuan-erp-skywalking-oap:11800 \
  -jar xuan-sales.jar
```

服务名必须与 `spring.application.name` 保持一致，便于链路、日志和部署记录对齐。

`duaoyunxuan.com` 服务器对外端口约定：

| 入口 | 地址 |
| --- | --- |
| SkyWalking UI | `http://duaoyunxuan.com:9026` |
| SkyWalking OAP gRPC | `duaoyunxuan.com:9050` |
| SkyWalking OAP HTTP | `http://duaoyunxuan.com:9051` |

从服务器外部接入 `duaoyunxuan.com` 的 Java Agent 使用：

```bash
-Dskywalking.collector.backend_service=duaoyunxuan.com:9050
```

### PostgreSQL

PostgreSQL 需要启用 `pg_stat_statements`。

启用方式一般包括：

```text
shared_preload_libraries = 'pg_stat_statements'
```

并在目标数据库中创建扩展：

```sql
create extension if not exists pg_stat_statements;
```

如果后续把扩展启用纳入项目数据库变更，必须遵守 Flyway migration 规范：先扫描 migration 目录确认最高版本号，再顺序追加新 migration，禁止修改历史 migration。

## 慢请求排查流程

推荐流程：

1. 在 SkyWalking UI 中按服务、接口、时间范围定位慢请求。
2. 打开慢请求 trace，确认耗时集中在服务调用、SQL、Redis、MQ、ES 还是业务代码。
3. 如果是 SQL 慢，记录 SQL span 和 traceId。
4. 在 PostgreSQL 使用 `pg_stat_statements` 查看该类 SQL 的调用次数、总耗时、平均耗时和最大耗时。
5. 必要时使用 `explain analyze` 看执行计划。
6. 根据结果决定是补索引、改查询、批量聚合、缓存、异步化，还是拆分接口。

## N+1 问题治理

监控发现明细行、关联 ID 或字典项数量增长导致请求耗时线性上升时，默认按 N+1 问题处理。

当前页面首屏或当前业务动作必须展示的数据，应由后端一次性聚合返回；禁止因为明细行、关联 ID 或字典项数量增长，造成前端或后端 N+1 次请求。

例外边界：

- 历史轨迹、审计日志、附件列表等重数据，可以点开时懒加载。
- 实时库存可用量等敏感数据可以单独刷新，但仍要支持批量查询。
- 超大明细不能无限一次性返回，必须分页或设置数量上限。
- 不相关的写操作不能为了“少请求”硬塞进一个接口。

## 告警建议

第一阶段先设置少量关键告警：

| 类型 | 建议 |
| --- | --- |
| 接口耗时 | 核心接口 P95 超过阈值告警 |
| 错误率 | 5xx 或业务异常突增告警 |
| SQL 耗时 | `pg_stat_statements` 中平均耗时或最大耗时异常的 SQL 进入优化清单 |
| JVM | 内存、GC、线程池、连接池异常告警 |
| 基础设施 | SkyWalking OAP、Elasticsearch、PostgreSQL 不可用告警 |

不要一开始配置过多告警。告警必须能推动处理，否则会变成噪音。

## 与其它组件的关系

- Sentinel 负责限流、熔断和慢调用保护，不负责链路追踪。
- SkyWalking 负责应用链路和单次请求耗时分析。
- Elasticsearch 在这里作为 SkyWalking 的 APM 存储，同时项目中也用于业务搜索索引；两类索引必须命名隔离。
- PostgreSQL `pg_stat_statements` 负责数据库内部 SQL 统计。
- 日志系统负责保留可检索的业务日志、异常日志和审计日志。

## 后续待补充

- SkyWalking NAS 部署参数。
- Spring Boot 服务统一 Java Agent 启动模板。
- 慢 SQL 看板和周度 SQL 优化清单。
