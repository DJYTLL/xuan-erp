---
title: "xuan-query"
---

xuan-query 是 Xuan ERP 的聚合查询服务，主要负责：页面聚合查询、报表、首页统计、读模型缓存和搜索索引。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-query` |
| 职责 | 页面聚合查询、报表、首页统计、读模型缓存、搜索索引 |
| 权限前缀 | `query` |
| 数据库 | `xuan_query` |
| 事件 Topic | `xuan-query-event` |
| Java 包名 | `com.xuan.erp.query` |

## 限界上下文

查询读模型上下文。它为前端复杂页面、报表和搜索接口提供聚合读模型，不拥有核心写模型。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | ReportView, DashboardMetric, QuerySnapshot | 本服务内部一致性边界 |
| 值对象 | DateRange, MetricCode | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | ReportRefreshPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务连接并只直接读写独立数据库 `xuan_query` 中属于自身的数据表，并负责维护自身 Redis 缓存和 Elasticsearch 搜索索引。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

## Redis 与 Elasticsearch

`xuan-query` 是 Redis 缓存和 Elasticsearch 搜索索引的主要封装服务。

- Redis 用于首页统计、筛选项、热点基础档案快照和轻量读模型缓存。
- Elasticsearch 用于商品、客户、供应商、单据和审计日志等搜索型读模型。
- 前端不直接访问 Redis 或 Elasticsearch。
- 强一致业务校验仍由对应业务服务主库完成。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
