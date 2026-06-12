---
title: "数据库迁移规范"
---

微服务拆分后，每个服务拥有自己的数据库或独立 schema。服务之间不直接跨库 JOIN，不直接修改其它服务的数据表。

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
    V1__init_product_schema.sql
    V2__add_product_price.sql
```

## 表设计基线

- 租户业务表默认包含 `tenant_id`。
- 业务删除默认走逻辑删除，使用 `deleted_at`。
- 有唯一性约束的业务表，优先使用活动态唯一索引，避免已删除数据占用唯一键。
- 租户级高频查询表建议补 `(tenant_id, deleted_at)` 组合索引。
- 审计日志、库存流水、打印日志等追加型流水表可作为例外，不走业务恢复模型。

示例：

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uk_product_code_active
    ON product (tenant_id, code)
    WHERE deleted_at IS NULL;
```

## 服务间数据关系

跨服务只保存必要外部 ID 和快照字段。例如销售单可以保存 `customerId`、`customerNameSnapshot`，但不直接 JOIN 客户服务数据库。

## 查询场景

复杂列表、首页统计和报表查询优先进入 `xuan-query`，通过事件、同步任务或只读读模型构建宽表，不让前端跨服务拼接大量数据。
