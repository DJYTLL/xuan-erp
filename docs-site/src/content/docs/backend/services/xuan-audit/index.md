---
title: "xuan-audit"
---

xuan-audit 是 Xuan ERP 的审计服务，主要负责：操作审计、登录审计、接口耗时、SQL 耗时、异常日志索引。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-audit` |
| 职责 | 操作审计、登录审计、接口耗时、SQL 耗时、异常日志索引 |
| 权限前缀 | `audit` |
| 数据库/Schema | `xuan_audit` |
| 事件 Topic | `xuan-audit-event` |
| Java 包名 | `com.xuan.erp.audit` |

## 限界上下文

审计观测上下文。它接收各服务审计事件并提供查询，不参与业务决策。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | AuditLog, LoginLog, ApiMetric | 本服务内部一致性边界 |
| 值对象 | TraceId, OperatorId, ClientIp | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | AuditRetentionPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_audit` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
