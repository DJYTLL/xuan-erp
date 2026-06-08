---
title: "xuan-finance"
---

xuan-finance 是 Xuan ERP 的财务服务，主要负责：应收、应付、收款、付款、核销、资金流水。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-finance` |
| 职责 | 应收、应付、收款、付款、核销、资金流水 |
| 权限前缀 | `finance` |
| 数据库/Schema | `xuan_finance` |
| 事件 Topic | `xuan-finance-event` |
| Java 包名 | `com.xuan.erp.finance` |

## 限界上下文

财务结算上下文。财务服务拥有应收应付和收付款记录，不拥有销售单、采购单和库存流水。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | Receivable, Payable, Receipt, Payment, Settlement | 本服务内部一致性边界 |
| 值对象 | Money, AccountNo, SettlementNo | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | ReceivableGeneratePolicy, SettlementPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_finance` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
