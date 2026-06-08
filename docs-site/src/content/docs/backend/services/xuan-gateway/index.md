---
title: "xuan-gateway"
---

xuan-gateway 是 Xuan ERP 的网关服务，主要负责：统一入口、认证入口、动态路由、入口限流、灰度路由、请求上下文注入。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-gateway` |
| 职责 | 统一入口、认证入口、动态路由、入口限流、灰度路由、请求上下文注入 |
| 权限前缀 | `gateway` |
| 数据库/Schema | `无业务库，必要时使用 xuan_gateway` |
| 事件 Topic | `无默认领域事件` |
| Java 包名 | `com.xuan.erp.gateway` |

## 限界上下文

系统入口上下文。它不拥有业务数据，只负责把外部请求转换成可信的内部请求，并把用户、租户、TraceId 等上下文传给后端服务。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | RouteDefinition, GatewayRateLimitRule | 本服务内部一致性边界 |
| 值对象 | RouteId, ServiceName, RequestTraceId | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | RouteMatchPolicy, TenantContextPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `无业务库，必要时使用 xuan_gateway` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
