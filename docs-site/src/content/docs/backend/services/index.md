---
title: "后端服务说明"
---

本目录用于沉淀每个后端微服务的独立说明。服务文档应说明服务定位、数据所有权、接口、权限、事件、配置、迁移和测试重点。

## 当前服务清单

| 服务 | 职责 |
| --- | --- |
| [`xuan-gateway`](./xuan-gateway/) | 统一入口、认证入口、路由、限流 |
| [`xuan-iam`](./xuan-iam/) | 登录、用户、角色、权限、菜单、列权限 |
| [`xuan-tenant`](./xuan-tenant/) | 租户、租户启停、租户配置 |
| [`xuan-product`](./xuan-product/) | 商品、分类、单位、价格、车型适配 |
| [`xuan-party`](./xuan-party/) | 客户、供应商、联系人、往来主体 |
| [`xuan-warehouse`](./xuan-warehouse/) | 仓库、库位、仓储基础档案 |
| [`xuan-sales`](./xuan-sales/) | 销售单、销售退货、审核、红冲 |
| [`xuan-procurement`](./xuan-procurement/) | 采购单、采购退货、审核、红冲 |
| [`xuan-inventory`](./xuan-inventory/) | 库存余额、库存流水、盘点、移库 |
| [`xuan-manufacturing`](./xuan-manufacturing/) | 组装、拆分、组装模板 |
| [`xuan-finance`](./xuan-finance/) | 应收、应付、收款、付款、核销 |
| [`xuan-document`](./xuan-document/) | 打印模板、打印日志、打印快照 |
| [`xuan-audit`](./xuan-audit/) | 审计日志、接口耗时、SQL 耗时 |
| [`xuan-query`](./xuan-query/) | 页面聚合查询、报表、首页统计 |

## 每个服务必须维护的文档

每个服务目录固定包含 5 类文档：

| 文档 | 说明 |
| --- | --- |
| `index.md` | 服务定位、限界上下文、领域模型、数据所有权 |
| `api.md` | 前端接口、内部接口、权限码、幂等与审计要求 |
| `database.md` | 数据库归属、核心表、索引、约束、migration 规则 |
| `events.md` | 发布事件、订阅事件、Outbox、幂等消费 |
| `permissions.md` | 菜单 code、pageKey、接口权限、列权限、IAM 同步 |

后续每个服务应按 [服务文档模板](/backend/service-doc-template/) 持续补齐，不能只写接口，不写数据归属、事件和权限。



