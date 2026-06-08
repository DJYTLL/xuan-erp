---
title: "xuan-query"
---

xuan-query 是 Xuan ERP 的聚合查询服务，主要负责：页面聚合查询、报表、首页统计、读模型缓存。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-query` |
| 职责 | 页面聚合查询、报表、首页统计、读模型缓存 |
| 权限前缀 | `query` |
| 数据库/Schema | `xuan_query` |
| 事件 Topic | `xuan-query-event` |
| Java 包名 | `com.xuan.erp.query` |

## 限界上下文

查询读模型上下文。它为前端复杂页面和报表提供聚合读模型，不拥有核心写模型。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | ReportView, DashboardMetric, QuerySnapshot | 本服务内部一致性边界 |
| 值对象 | DateRange, MetricCode | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | ReportRefreshPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_query` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
