---
title: "服务文档模板"
---

每个后端微服务都应有一份独立服务文档，用于说明职责边界、DDD 领域模型、数据所有权、接口、事件、权限和部署配置。服务文档建议放在 `backend/services` 目录下。

## 模板

```md
---
title: "xuan-product"
---

## 服务定位

说明这个服务负责什么、不负责什么。

## 限界上下文

说明该服务所属的业务上下文、上游/下游上下文，以及与其它服务的边界。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | Product | 商品主档 |
| 值对象 | ProductCode | 商品编码 |
| 领域服务 | ProductPricingService | 商品价格规则 |

## 聚合规则

- 聚合根负责维护自身业务一致性。
- 外部服务不能直接修改聚合内部状态。
- 复杂规则优先放在领域对象或领域服务中，不放在 Controller、Mapper 或工具类中。

## 数据所有权

| 表/聚合 | 说明 | 是否租户隔离 |
| --- | --- | --- |
| product | 商品主档 | 是 |

## 核心接口

详细接口文档放在：

```text
backend/services/xuan-product/api.md
```

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | /api/products | product:view | 商品列表 |

## 权限清单

| 权限码 | 类型 | 说明 |
| --- | --- | --- |
| product:view | API | 查询商品 |
| product:create | API | 新增商品 |

## 发布事件

| 事件 | Topic | 触发时机 | 消费方 |
| --- | --- | --- | --- |
| ProductCreatedEvent | xuan-product-event | 商品创建后 | query-service |

## 订阅事件

| 事件 | 来源服务 | 处理逻辑 |
| --- | --- | --- |
| TenantDisabledEvent | tenant-service | 禁用租户相关商品操作 |

## 配置项

| 配置 | 说明 | 示例 |
| --- | --- | --- |
| spring.application.name | 服务名 | xuan-product |

## 数据库迁移

数据库结构文档放在：

```text
backend/services/xuan-product/database.md
```

记录当前服务 migration 目录、最新版本号和新增规则。

## 测试重点

- 权限校验。
- 租户隔离。
- 幂等与并发。
- 事件发布和消费。
```


