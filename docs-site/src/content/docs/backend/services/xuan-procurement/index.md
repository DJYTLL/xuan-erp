---
title: "xuan-procurement"
---

xuan-procurement 是 Xuan ERP 的采购服务，主要负责：采购单、采购退货、草稿、审核、红冲、采购历史。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-procurement` |
| 职责 | 采购单、采购退货、草稿、审核、红冲、采购历史 |
| 权限前缀 | `procurement` |
| 数据库/Schema | `xuan_procurement` |
| 事件 Topic | `xuan-procurement-event` |
| Java 包名 | `com.xuan.erp.procurement` |

## 限界上下文

采购交易上下文。采购服务拥有采购单据和采购审核规则，不直接维护库存余额和应付余额。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | PurchaseOrder, PurchaseReturnOrder | 本服务内部一致性边界 |
| 值对象 | PurchaseOrderNo, PurchaseAmount, SupplierSnapshot | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | PurchaseAuditPolicy, PurchaseReturnPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_procurement` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
