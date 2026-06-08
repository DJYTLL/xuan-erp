---
title: "xuan-sales DDD 示例"
---

本文以销售单为例，说明 `xuan-sales` 如何按 DDD 方式落地。它不是最终代码，而是后续实现销售服务时的建模样板。

## 限界上下文

`xuan-sales` 属于销售交易上下文，负责销售单、销售退货、审核、红冲、复制和销售历史。

它不直接拥有：

| 外部能力 | 归属服务 | 协作方式 |
| --- | --- | --- |
| 商品档案、价格规则 | `xuan-product` | 查询商品快照、订阅价格变更事件 |
| 客户资料、信用额度 | `xuan-party` | 查询客户快照、订阅客户状态事件 |
| 库存余额、预占、扣减 | `xuan-inventory` | 内部接口或事件协作 |
| 应收账款 | `xuan-finance` | 发布销售审核事件，由财务生成应收 |
| 打印快照 | `xuan-document` | 发布审核事件后生成打印快照 |

## 聚合设计

销售单聚合：

```text
SalesOrder
  SalesOrderLine
  SalesOrderStatus
  SalesAmount
  CustomerSnapshot
  ProductSnapshot
```

销售退货聚合：

```text
SalesReturnOrder
  SalesReturnLine
  ReturnReason
  OriginalSalesOrderRef
```

聚合内部负责维护状态和明细一致性，例如：

- 草稿才能修改。
- 已审核单据不能直接删除。
- 审核必须有至少一行明细。
- 明细数量必须大于 0。
- 单据金额必须由明细计算得出，不能由外部直接覆盖。

## 值对象

| 值对象 | 说明 |
| --- | --- |
| `SalesOrderNo` | 销售单号 |
| `Money` | 金额，明确币种和精度 |
| `Quantity` | 数量，明确小数位 |
| `CustomerSnapshot` | 下单时客户快照 |
| `ProductSnapshot` | 下单时商品快照 |
| `Auditor` | 审核人 |
| `TenantId` | 当前租户 |

快照对象用于保留交易发生时的业务事实。商品或客户后续修改，不应自动改变历史销售单。

## 应用服务流程

销售审核流程：

```text
SalesOrderApplicationService.audit
  -> 检查 sales:audit 权限
  -> 读取 TenantContext
  -> 加载 SalesOrder 聚合
  -> 查询客户状态和信用信息
  -> 请求库存服务做库存预占或校验
  -> 调用 SalesOrder.audit(...)
  -> 保存聚合
  -> 写 Outbox 事件 SalesOrderAuditedEvent
```

应用服务负责编排事务、权限、外部服务调用和事件发布。销售单状态变化、能否审核、金额校验等规则由领域模型表达。

## 领域方法示例

```java
public class SalesOrder {
    private SalesOrderStatus status;
    private List<SalesOrderLine> lines;
    private List<DomainEvent> events;

    public void audit(Auditor auditor, StockCheckResult stockCheckResult) {
        if (!status.canAudit()) {
            throw new DomainException("当前状态不允许审核");
        }
        if (lines.isEmpty()) {
            throw new DomainException("销售单明细不能为空");
        }
        if (!stockCheckResult.isEnough()) {
            throw new DomainException("库存不足");
        }

        this.status = SalesOrderStatus.AUDITED;
        this.events.add(new SalesOrderAuditedEvent(this.id, this.tenantId));
    }
}
```

## 仓储接口

仓储接口定义在领域层：

```java
public interface SalesOrderRepository {
    Optional<SalesOrder> findById(TenantId tenantId, SalesOrderId id);
    void save(SalesOrder salesOrder);
}
```

MyBatis、JPA、SQL、DO 转换都放在 `infrastructure`，不能让领域层依赖数据库表结构。

## 领域事件

| 事件 | 触发时机 | 消费者 |
| --- | --- | --- |
| `SalesOrderCreatedEvent` | 销售单创建后 | `xuan-query` |
| `SalesOrderAuditedEvent` | 销售单审核后 | `xuan-inventory`、`xuan-finance`、`xuan-document`、`xuan-query` |
| `SalesOrderCancelledEvent` | 销售单取消后 | `xuan-inventory`、`xuan-query` |
| `SalesReturnAuditedEvent` | 销售退货审核后 | `xuan-inventory`、`xuan-finance`、`xuan-query` |

事件必须包含：

```text
eventId
tenantId
salesOrderId
occurredAt
traceId
sourceService
```

## 一致性策略

销售审核涉及销售、库存、财务，多服务强事务成本高。推荐：

- 销售服务本地事务保存销售单状态和 Outbox 事件。
- 库存服务消费事件或接收命令后扣减库存。
- 财务服务消费审核事件生成应收。
- 失败时通过重试、死信队列和补偿任务处理。
- 页面通过 `xuan-query` 展示最终状态，必要时显示处理中状态。

Seata 只在确实需要同步强一致、参与服务少、链路短时评估使用。

## 测试重点

- `SalesOrder.audit` 状态流转测试。
- 库存不足时不能审核。
- 明细为空时不能审核。
- 非当前租户不能读取或审核销售单。
- 审核成功后必须写出 `SalesOrderAuditedEvent`。
- 重复审核请求必须幂等。
- 无 `sales:audit` 权限不能调用审核接口。
