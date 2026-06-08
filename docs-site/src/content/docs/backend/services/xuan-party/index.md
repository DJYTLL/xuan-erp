---
title: "xuan-party"
---

xuan-party 是 Xuan ERP 的往来主体服务，主要负责：客户、供应商、联系人、结算信息、往来主体标签。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-party` |
| 职责 | 客户、供应商、联系人、结算信息、往来主体标签 |
| 权限前缀 | `party` |
| 数据库/Schema | `xuan_party` |
| 事件 Topic | `xuan-party-event` |
| Java 包名 | `com.xuan.erp.party` |

## 限界上下文

往来主体上下文。它统一管理客户和供应商资料，不拥有销售单、采购单、应收应付余额。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | Customer, Supplier, Contact | 本服务内部一致性边界 |
| 值对象 | PartyCode, ContactPhone, TaxNo | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | PartyMergePolicy, CreditLimitPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_party` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
