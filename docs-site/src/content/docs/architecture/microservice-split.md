---
title: "Xuan ERP 微服务拆分与权限架构说明"
---

## 1. 背景

当前项目是汽配仓储 / 进销存 / ERP 系统，业务覆盖系统管理、租户、权限、商品、客户、供应商、仓库、销售、采购、库存、组装拆分、财务、打印、审计和报表查询。

如果按生产环境中的领域边界拆分，不应按 Controller 或数据库表机械拆分，而应按业务能力、数据所有权、事务边界、发布独立性和团队职责拆分。

完整拆分后，推荐形成 14 个后端微服务子项目，以及 1 个前端项目。

## 2. 推荐服务清单

| 序号  | 服务名                         | 职责                                  |
| --- | --------------------------- | ----------------------------------- |
| 1   | `xuan-gateway`       | 统一入口、路由、认证入口、限流、跨域、Trace 透传         |
| 2   | `xuan-iam`           | 登录、用户、角色、权限、菜单、列权限、授权快照             |
| 3   | `xuan-tenant`        | 租户、租户启停、租户设置、租户业务配置                 |
| 4   | `xuan-product`       | 商品、商品分类、单位、商品价格、车型适配、车型品牌 / 车系 / 车型 |
| 5   | `xuan-party`         | 客户、供应商、客户类别、供应商类型、往来主体、联系人、导入批次     |
| 6   | `xuan-warehouse`     | 仓库、库位、仓储基础档案                        |
| 7   | `xuan-sales`         | 销售单、销售退货、草稿、审核、红冲、复制、销售历史           |
| 8   | `xuan-procurement`   | 采购单、采购退货、草稿、审核、红冲、采购历史              |
| 9   | `xuan-inventory`     | 库存余额、库存流水、盘点、初始库存、移库、库存预警、库存占用      |
| 10  | `xuan-manufacturing` | 组装单、拆分单、组装模板、成品与子件关系                |
| 11  | `xuan-finance`       | 应收、应付、收款、付款、核销、客户欠款、供应商欠款、往来财务      |
| 12  | `xuan-document`      | 打印模板、打印日志、单据打印快照、QZ Tray 签名、本地打印集成  |
| 13  | `xuan-audit`         | 审计日志、删除原因、接口耗时、SQL 耗时、跨租户审计         |
| 14  | `xuan-query`         | 页面聚合查询、只读宽表、报表、首页统计、前端读模型           |

前端项目：

| 项目                   | 职责                        |
| -------------------- | ------------------------- |
| `xuan-admin-web` | Vue 管理端，通过 Gateway 访问后端服务 |

## 3. 数据所有权

微服务化后，每个服务应拥有自己的数据库或独立 schema。业务服务之间不直接跨库 JOIN。

| 服务                          | 数据所有权                   |
| --------------------------- | ----------------------- |
| `xuan-iam`           | 用户、角色、权限、菜单、角色权限、列权限    |
| `xuan-tenant`        | 租户、租户设置、租户业务配置          |
| `xuan-product`       | 商品、分类、单位、价格、车型适配        |
| `xuan-party`         | 客户、供应商、往来主体、客户 / 供应商分类  |
| `xuan-warehouse`     | 仓库、库位                   |
| `xuan-sales`         | 销售单、销售退货                |
| `xuan-procurement`   | 采购单、采购退货                |
| `xuan-inventory`     | 库存余额、库存流水、盘点、初始库存、移库、预警 |
| `xuan-manufacturing` | 组装单、拆分单、组装模板            |
| `xuan-finance`       | 应收、应付、收款、付款、核销          |
| `xuan-document`      | 打印模板、打印日志、打印快照          |
| `xuan-audit`         | 审计日志、接口耗时、SQL 耗时        |
| `xuan-query`         | 只读宽表、报表表、页面聚合读模型        |

## 4. 权限体系设计

权限不应拆成 14 套。生产环境中应采用：

```text
IAM 集中管理权限数据
Gateway 做入口认证
业务服务做本服务接口和资源校验
Service Mesh / NetworkPolicy 做服务间访问控制
```

### 4.1 IAM 负责

`xuan-iam` 统一负责：

- 登录与 Token 签发。
- 用户、角色、权限、菜单。
- 角色权限授权。
- 列权限授权。
- 当前用户权限快照。
- super_admin / tenant_admin 判断。
- 前端菜单树、按钮权限、列权限返回。
- 接收或维护各业务服务声明的权限定义。

运行时的最终权限数据只存在 IAM 中。

### 4.2 业务服务维护权限清单的含义

每个业务服务可以维护一份“本服务需要哪些权限点”的清单，但这不是授权数据，也不是独立权限表。

例如 `xuan-sales` 声明：

```yaml
service: xuan-sales
permissions:
  - code: erp-sale-draft:view
    name: 销售草稿查看
    menuCode: erp-sale-draft
  - code: erp-sale-draft:add
    name: 销售草稿新增
    menuCode: erp-sale-draft
  - code: erp-sale-approved:red-flush
    name: 已审核销售单红冲
    menuCode: erp-sale-approved
```

这份清单的作用是告诉 IAM：销售服务有哪些权限点需要被纳入统一权限中心。

最终仍然由 IAM 保存：

```text
permission
menu
role_permission
role_column_permission
tenant_menu
```

### 4.3 权限同步方式

权限同步统一采用 CI/CD 实现。

各业务服务在自己的代码仓库或模块中维护权限清单，CI/CD 在构建阶段统一收集所有服务的权限、菜单和列权限定义，执行重复 code 校验、命名规范校验、菜单归属校验、权限树完整性校验，并生成 IAM 的 migration 或 seed 变更。

最终落库仍由 `xuan-iam` 执行，运行时的权限数据仍然只存在 IAM 中。这样可以保证权限定义跟业务代码同步演进，同时保留数据库迁移的审计、回滚和测试能力。

## 5. 服务间调用权限

需要区分两类权限：

| 类型   | 说明                  | 示例                                     |
| ---- | ------------------- | -------------------------------------- |
| 用户权限 | 当前用户能不能执行某个业务动作     | `erp-product:view`、`erp-stock:view`    |
| 服务权限 | 某个服务能不能调用另一个服务的内部接口 | `product-service -> inventory-service` |

例如商品服务调用库存服务：

```text
前端 -> Gateway -> product-service -> inventory-service
```

库存服务应做两层判断：

1. `product-service` 是否允许调用库存内部查询接口。
2. 当前用户是否允许查看库存数量、成本价、库存金额等数据。

伪逻辑：

```java
if (!serviceAuth.canCall("xuan-product", "inventory.stock.read")) {
    throw new ForbiddenException();
}

if (!iam.hasPermission(userId, tenantId, "erp-stock:view")) {
    return hideStockFields();
}

return stockService.queryStock(productId, tenantId);
```

## 6. K8s、Service Mesh 与白名单

如果拆成 14 个服务，建议使用 K8s 作为生产底座。

推荐职责划分：

| 组件           | 职责                                            |
| ------------ | --------------------------------------------- |
| K8s          | 部署、服务发现、扩缩容、健康检查、ServiceAccount、NetworkPolicy |
| Service Mesh | 服务间 mTLS、服务身份、访问策略、流量治理、链路观测                  |
| IAM          | 用户权限、角色权限、租户权限、菜单权限、列权限                       |
| 白名单          | 服务间允许调用关系的策略表达                                |

Service Mesh 和白名单不是二选一。成熟做法是用 Service Mesh 承载服务间白名单策略。

例如：

```text
xuan-product -> xuan-inventory: GET /internal/stocks/**
xuan-sales -> xuan-inventory: POST /internal/stock-deductions
xuan-sales -> xuan-finance: POST /internal/receivables
xuan-procurement -> xuan-inventory: POST /internal/stock-inbounds
```

但 Service Mesh 不能替代 IAM。Mesh 判断“服务能不能调服务”，IAM 判断“用户能不能看或操作业务数据”。

## 7. Query Service 如何查询数据

`xuan-query` 不直接 JOIN 各业务服务数据库，也不把实时跨服务聚合作为主要查询方式。它维护自己的只读宽表数据库，所有复杂列表、报表、首页统计和页面首屏聚合数据都优先从自己的读模型表查询。

`xuan-query` 拥有自己的数据库，例如：

```text
xuan_query_db
```

里面保存面向查询的只读宽表，例如：

```text
product_stock_view
sale_order_list_view
customer_debt_view
inventory_summary_view
finance_summary_view
```

数据来源于各服务发布的事件：

```text
product-service 发布 ProductUpdated
inventory-service 发布 StockChanged
sales-service 发布 SaleApproved / SaleCancelled
finance-service 发布 ReceivableChanged
query-service 消费事件并更新自己的读模型表
```

前端查询时：

```text
前端 -> query-service -> xuan_query_db
```

这种方式牺牲一部分实时性，换取查询性能、稳定性和跨服务解耦。对必须实时校验的数据，例如审核前库存校验、付款核销、单据状态变更，仍然由对应领域服务直接查询自己的主库处理，不走 `xuan-query`。

## 8. 典型业务链路

### 8.1 商品详情查询

推荐链路：

```text
前端 -> Gateway -> query-service -> query_db
```

如需实时库存，可由 query-service 调用 inventory-service 的轻量接口。

### 8.2 销售单审核

推荐链路：

```text
前端 -> Gateway -> sales-service
sales-service 保存销售单状态
sales-service 发布 SaleApproved 事件
inventory-service 消费事件扣减库存
finance-service 消费事件生成应收
audit-service 消费事件写审计
query-service 消费事件更新销售列表读模型
```

库存强一致校验可以由 `sales-service` 同步调用 `inventory-service` 完成，但不建议把所有后续动作都放在一个同步 HTTP 链路里。

## 9. 全量拆分的主要风险

全量拆成 14 个服务后，主要风险包括：

- 查询复杂，不能再跨库 JOIN。
- 写事务复杂，需要 Saga、Outbox、幂等、补偿和对账。
- 权限体系复杂，必须统一由 IAM 管理。
- 运维复杂，需要 K8s、配置中心、日志、监控、链路追踪、告警。
- 发布复杂，一个需求可能影响多个服务。
- 故障面变大，需要超时、重试、熔断、降级。
- 数据一致性从强一致变为大量最终一致。

## 10. 最终建议

如果目标是生产级微服务，而不是简单拆目录，建议采用如下原则：

```text
权限中心化：IAM 统一管理用户、角色、权限、菜单、列权限。
服务自治化：每个业务服务拥有自己的数据和领域逻辑。
查询模型化：复杂查询走 query-service 的读模型，不跨库 JOIN。
服务治理平台化：K8s + Service Mesh + NetworkPolicy + Trace + Metrics。
事务事件化：跨服务写操作通过事件、Outbox、Saga、幂等消费和对账保证。
```

最终服务清单建议固定为：

```text
xuan-gateway
xuan-iam
xuan-tenant
xuan-product
xuan-party
xuan-warehouse
xuan-sales
xuan-procurement
xuan-inventory
xuan-manufacturing
xuan-finance
xuan-document
xuan-audit
xuan-query
```





