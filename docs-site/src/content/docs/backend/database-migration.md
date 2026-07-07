---
title: "数据库迁移规范"
---

微服务拆分后，每个服务拥有自己的独立数据库。服务之间不直接跨库 JOIN，不直接修改其它服务的数据表。

## 基本原则

- 每个服务独立维护自己的 Flyway migration。
- 所有表结构、字段、索引、约束、初始化数据变更，必须走 Flyway migration。
- 只允许追加新 migration，不修改、覆盖或重排历史 migration。
- migration 版本号不能跳号、不能复用。
- 涉及表、字段、索引、约束、初始化数据时，必须先扫描当前服务 migration 目录，确认最高版本号。
- 开始修改前要说明当前最高版本、拟新增文件名、是否存在版本冲突。
- migration 命名统一为 `V{版本号}__{英文描述}.sql`。
- 结构变更尽量使用 `IF NOT EXISTS`。
- 数据回填必须带明确 `WHERE` 条件，避免误更新全表。

## 推荐目录

```text
xuan-product/
  src/main/resources/db/migration/
    V1__init_product_database.sql
    V2__add_product_price.sql
```

## 表设计基线

- 租户业务表默认包含 `tenant_id`。
- 业务删除默认走逻辑删除，使用 `deleted_at`。
- 数据库只保留表结构、主键和普通查询索引；外键、唯一性、状态枚举、金额非负等业务约束全部由应用逻辑校验。
- 有唯一性要求的业务表，使用应用层校验活动态数据，数据库只补普通索引辅助查询，避免已删除数据占用逻辑唯一键。
- 租户级高频查询表建议补 `(tenant_id, deleted_at)` 组合索引。
- 审计日志、库存流水、打印日志等追加型流水表可作为例外，不走业务恢复模型。

示例：

```sql
CREATE INDEX IF NOT EXISTS idx_product_code_active
    ON product (tenant_id, code)
    WHERE deleted_at IS NULL;
```

## 服务间数据关系

跨服务只保存必要外部 ID 和快照字段。例如销售单可以保存 `customerId`、`customerNameSnapshot`，但不直接 JOIN 客户服务数据库。

## 当前 V1 设计约定

当前数据库设计稿按 14 个服务独立数据库生成，各服务只有自己的 `V1__init_*_database.sql`。在正式落库前，V1 可以作为初始化草稿修正；一旦某环境执行过对应 V1，后续必须新增 V2，不再改写历史 V1。

当前 V1 约束口径：

- 不创建 PostgreSQL schema，不使用 `xuan_xxx.table` 这种限定名。
- `delivery_method` 归属 `xuan-party`，不放在 `xuan-sales`。
- `status` 字段只记录业务状态，合法状态由应用逻辑校验，不加数据库 CHECK。
- 金额、数量、行号、导入计数、财务分摊、应收应付等字段的非负或关系约束由应用逻辑校验。
- 库存策略只保留结构字段和普通索引，不在数据库层强制上下限关系。
- 单据号由业务自己控制，当前不新增单据号序列表。
- 退货类型、结算模式等业务枚举也不加数据库 CHECK。

## V1 初始化数据口径

V1 可以包含基础配置和基础字典，但不包含真实业务单据。已约定的 V1 seed 范围：

| 服务 | 初始化数据 |
| --- | --- |
| `xuan-tenant` | 默认租户、默认联系人、默认套餐、默认域名、默认套餐绑定、生命周期历史、初始化任务、初始化步骤、租户配置 |
| `xuan-iam` | 基础权限、一级菜单、默认租户菜单授权 |
| `xuan-product` | 默认分类、常用单位 |
| `xuan-party` | 客户类别、供应商类型、送货方式 |
| `xuan-warehouse` | 默认仓库、默认库位 |
| `xuan-finance` | 结算方式、收款方式、付款方式 |
| `xuan-document` | 默认打印模板 |
| 其它服务 | V1 不写业务初始化数据 |

初始化数据必须能被静态校验和 PostgreSQL 事务回滚验证覆盖。远端数据库只允许先创建数据库和授权，表结构、约束和 seed 仍由 Flyway 执行。

## 查询场景

复杂列表、首页统计和报表查询优先进入 `xuan-query`，通过事件、同步任务或只读读模型构建宽表，不让前端跨服务拼接大量数据。
