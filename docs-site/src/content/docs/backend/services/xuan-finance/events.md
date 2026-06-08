---
title: "xuan-finance 事件文档"
---

本文记录 `xuan-finance` 发布和订阅的领域事件。事件只表达已经发生的业务事实，不承载远程过程调用语义。

## 发布事件

| 事件 | Topic | 触发时机 | 主要消费者 | 说明 |
| --- | --- | --- | --- | --- |
| ReceivableCreated, PayableCreated, ReceiptConfirmed, PaymentConfirmed | `xuan-finance-event` | 应收应付生成、收付款确认、核销完成事件 | 按业务协作需要订阅 | 事件结构开发时补齐 |

## 订阅事件

| 来源服务 | 事件 | 处理目的 | 幂等键 |
| --- | --- | --- | --- |
| 按协作关系确定 | 销售审核事件、采购审核事件、销售退货审核事件、采购退货审核事件 | 构建本服务状态或读模型 | `eventId` |

## 事件要求

- 每个事件必须包含 `eventId`、`tenantId`、`occurredAt`、`traceId`、`sourceService`。
- 事件发布使用 Outbox Pattern，避免本地事务成功但消息丢失。
- 消费端必须按 `eventId` 做幂等。
- 失败事件进入重试和死信队列，并提供人工补偿入口。
