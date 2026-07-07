---
title: "Redis 与 Elasticsearch 准备"
---

本文记录 Xuan ERP 接入 Redis 和 Elasticsearch 的标准边界。

当前基础设施栈固定为：

```text
Nacos + Sentinel + Seata + RocketMQ + Redis + Elasticsearch
```

Redis 和 Elasticsearch 都不是业务主库。Redis 负责缓存、短期状态和高频读优化；Elasticsearch 负责全文检索、模糊搜索和搜索型读模型。核心写入、事务校验、库存扣减、财务核销等业务正确性仍以各业务服务主库和领域规则为准。

## Redis 定位

Redis 适合用于：

- 权限、菜单、租户状态等短期快照缓存。
- 商品、客户、供应商、仓库、单位、字典等热点基础档案缓存。
- `xuan-query` 的首页统计、筛选项、轻量读模型缓存。
- 幂等键、防重复提交、短期操作锁。
- 登录态扩展、Token 黑名单、验证码、临时授权状态。

Redis 不适合用于：

- 替代数据库保存业务事实。
- 保存没有过期策略的大对象。
- 作为库存、财务、权限授权关系的唯一判断来源。
- 绕过业务服务直接给前端拼接数据。

## Elasticsearch 定位

Elasticsearch 适合用于：

- 商品名称、编码、规格、车型适配等复合搜索。
- 客户、供应商、联系人等模糊搜索。
- 销售单、采购单、库存流水等单据综合搜索。
- 审计日志、打印日志、接口耗时日志等检索型数据。
- `xuan-query` 维护的搜索型读模型。

Elasticsearch 不适合用于：

- 替代事务数据库。
- 处理审核前库存校验、付款核销、单据状态变更等强一致判断。
- 让前端直接查询 ES。
- 在索引里保存无法脱敏的敏感原文。

## 接入原则

- 业务主库仍然是事实来源。
- 业务服务发布领域事件，`xuan-query` 消费事件后更新 Redis 缓存和 Elasticsearch 索引。
- 前端只访问 Gateway 后面的业务接口或 `xuan-query` 接口，不直接访问 Redis 或 Elasticsearch。
- 搜索索引必须带 `tenantId`、`deletedAt`、业务状态、权限过滤所需字段。
- 索引更新要支持幂等，事件必须包含 `eventId`、`tenantId`、`occurredAt`、`sourceService`、`traceId`。
- 索引允许最终一致，页面需要能接受短暂延迟。
- 强一致校验继续走对应业务服务主库，不走 ES。

## 数据流

```text
业务服务主库写入
  -> 写 Outbox 事件
  -> RocketMQ 投递事件
  -> xuan-query 消费事件
  -> 更新 xuan_query 读模型表
  -> 更新 Redis 缓存
  -> 更新 Elasticsearch 索引
  -> 前端通过 xuan-query 查询
```

## Maven 依赖

需要使用 Redis 的服务引入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

需要使用 Elasticsearch 的服务引入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

依赖版本由 Spring Boot BOM 统一管理，业务服务不单独指定版本。

## Nacos 配置示例

Redis 和 Elasticsearch 的连接位置可以放入 Nacos，密码和证书不建议明文放入 Nacos。

```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379
      database: 0
      timeout: 3s
    elasticsearch:
      repositories:
        enabled: true
  elasticsearch:
    uris:
      - http://elasticsearch:9200

xuan:
  cache:
    product-ttl: 10m
    permission-ttl: 5m
  search:
    index-prefix: xuan
    refresh-policy: async
```

`duaoyunxuan.com` 服务器当前对外端口约定中：

```text
Redis:             duaoyunxuan.com:9046
Elasticsearch API: http://duaoyunxuan.com:9040
Kibana Console:    http://duaoyunxuan.com:9024
```

如果业务服务和中间件在同一 Docker 网络内，优先使用服务名和容器内部端口，例如 `xuan-erp-redis:6379`、`xuan-erp-es:9200`。

NAS 或其它远程服务器上的 Redis / Elasticsearch 端口以各自部署记录为准，不套用本页的 `duaoyunxuan.com` 对外端口。

## 索引命名

索引命名建议：

```text
xuan-{env}-{domain}-{model}
```

示例：

```text
xuan-prod-product-search
xuan-prod-party-search
xuan-prod-sales-order-search
xuan-prod-audit-log
```

索引字段必须包含：

| 字段 | 说明 |
| --- | --- |
| `tenantId` | 租户隔离 |
| `businessId` | 业务主键 |
| `sourceService` | 来源服务 |
| `deletedAt` | 逻辑删除过滤 |
| `status` | 业务状态过滤 |
| `updatedAt` | 增量同步和排障 |
| `permissionScope` | 权限过滤所需字段，按业务需要定义 |

## Xuan ERP 推荐边界

| 场景 | 推荐方式 |
| --- | --- |
| 商品下拉、客户下拉、仓库下拉 | Redis 缓存 + 批量查询接口 |
| 商品复杂搜索、车型适配搜索 | Elasticsearch |
| 审计日志、打印日志检索 | Elasticsearch |
| 首页统计和常用筛选项 | `xuan-query` 读模型 + Redis |
| 销售单列表、采购单列表 | `xuan-query` 读模型，复杂搜索时结合 ES |
| 审核前库存校验 | 业务服务主库 |
| 财务核销、付款状态判断 | 业务服务主库 |

## 验证方式

接入 Redis 和 Elasticsearch 后至少验证：

- Redis key 命名包含环境、服务和业务含义。
- Redis key 有明确 TTL，永久 key 必须有设计说明。
- Elasticsearch 索引包含 `tenantId`、`deletedAt` 和业务状态字段。
- 索引更新消费支持幂等。
- 删除、停用、权限变化能同步影响搜索结果。
- 前端不能直接访问 Redis 或 Elasticsearch。
- `xuan-query` 搜索接口能区分查主库、查读模型、查 ES 的边界。
