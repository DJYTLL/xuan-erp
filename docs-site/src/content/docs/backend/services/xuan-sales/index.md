---
title: "xuan-sales"
---

xuan-sales 是 Xuan ERP 的销售服务，主要负责：销售单、销售退货、草稿、审核、红冲、复制、销售历史。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-sales` |
| 职责 | 销售单、销售退货、草稿、审核、红冲、复制、销售历史 |
| 权限前缀 | `sales` |
| 数据库/Schema | `xuan_sales` |
| 事件 Topic | `xuan-sales-event` |
| Java 包名 | `com.xuan.erp.sales` |

## 限界上下文

销售交易上下文。销售服务拥有销售单据和销售审核规则，不直接维护库存余额和应收余额，通过库存服务与财务服务协作完成后续处理。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | SalesOrder, SalesReturnOrder | 本服务内部一致性边界 |
| 值对象 | SalesOrderNo, SalesAmount, Quantity, Auditor | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | SalesAuditPolicy, SalesReturnPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_sales` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
