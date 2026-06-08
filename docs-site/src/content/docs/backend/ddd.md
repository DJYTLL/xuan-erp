---
title: "DDD 分层与领域建模"
---

Xuan ERP 后端微服务默认采用 DDD 方式设计。DDD 在本项目中的目标不是增加复杂度，而是让服务边界、业务规则、数据归属和代码分层保持清晰，避免把核心业务写成 Controller、Service、Mapper 的过程式脚本。

## 适用范围

DDD 主要用于承载业务复杂度的领域服务，例如商品、客户供应商、销售、采购、库存、财务、组装拆分、租户和 IAM。Gateway、文档、审计、查询服务可以按自身特点简化，但仍要遵守边界清晰、依赖方向清晰的原则。

## 分层结构

每个业务微服务建议采用下面的分层：

```text
xuan-sales/
  src/main/java/com/xuan/erp/sales/
    interfaces/
      controller/
      assembler/
      dto/
    application/
      command/
      query/
      service/
    domain/
      model/
      service/
      repository/
      event/
    infrastructure/
      persistence/
      mapper/
      client/
      messaging/
      config/
```

| 层 | 职责 |
| --- | --- |
| `interfaces` | 对外入口，包含 Controller、请求响应 DTO、DTO 与应用层命令的转换 |
| `application` | 编排业务用例、事务边界、权限校验、调用领域对象、发布事件 |
| `domain` | 聚合根、实体、值对象、领域服务、领域事件、仓储接口 |
| `infrastructure` | 数据库实现、MyBatis/JPA Mapper、Feign Client、MQ、Nacos、第三方适配 |

依赖方向必须是：

```text
interfaces -> application -> domain
infrastructure -> domain
application -> domain
```

`domain` 不依赖 Spring MVC、MyBatis、Feign、MQ、数据库表结构和外部接口。

## 领域对象规则

- 聚合根负责维护自身一致性，不允许外部随意改内部状态。
- 值对象必须表达业务含义，例如金额、数量、单号、租户 ID。
- 领域服务只放无法自然归属到某个实体或值对象的领域规则。
- 应用服务负责编排流程，不承载复杂业务规则。
- 仓储接口定义在 `domain`，实现放在 `infrastructure`。
- 领域事件从领域层产生，由应用层或基础设施层负责发布。

## 聚合边界示例

销售服务中，销售单可以作为聚合根：

```text
SalesOrder
  SalesOrderLine
  SalesOrderStatus
  SalesOrderAmount
```

销售审核规则应由 `SalesOrder` 或销售领域服务表达，而不是散落在 Controller 或 Mapper 里。

```java
public class SalesOrder {
    public void audit(Auditor auditor, StockCheckResult stockCheckResult) {
        if (!status.canAudit()) {
            throw new DomainException("当前状态不允许审核");
        }
        if (!stockCheckResult.isEnough()) {
            throw new DomainException("库存不足");
        }
        this.status = SalesOrderStatus.AUDITED;
        this.auditedBy = auditor.id();
        this.auditedAt = OffsetDateTime.now();
        this.events.add(new SalesOrderAuditedEvent(this.id));
    }
}
```

## DTO 与领域模型隔离

Controller 入参、Feign DTO、MQ 消息 DTO、数据库 DO 不应直接作为领域模型使用。

推荐转换路径：

```text
Request DTO -> Command -> Aggregate -> Persistence Object
Aggregate -> Response DTO
Aggregate -> Domain Event -> Message DTO
```

## 与微服务边界的关系

一个微服务通常对应一个或多个限界上下文。服务之间不能共享领域模型，也不能通过公共模块共享业务实体。跨服务协作优先使用：

- OpenFeign 查询必要信息。
- 领域事件传递业务事实。
- Outbox Pattern 保证事件可靠发布。
- Query Service 构建读模型。

## 落地要求

- 新增业务服务时，必须先写清楚限界上下文、聚合根、数据所有权和领域事件。
- 新增复杂业务流程时，优先在领域模型中表达核心规则。
- 单元测试优先覆盖领域对象和领域服务。
- 应用服务测试覆盖事务、权限、幂等、事件发布和跨服务编排。
- 不把业务规则写在 Mapper XML、Controller 或工具类里。
