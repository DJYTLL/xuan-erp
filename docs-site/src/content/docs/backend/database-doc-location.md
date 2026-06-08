---
title: "数据库结构文档规范"
---

数据库结构文档用于说明每个服务拥有的数据表、字段、索引、唯一约束、租户隔离字段和初始化数据。它和 Flyway migration 互相对应，但职责不同：migration 负责执行变更，数据库结构文档负责解释当前结构和设计意图。

## 存放位置

数据库结构文档按服务放在：

```text
docs-site/src/content/docs/backend/services/<service-name>/database.md
```

示例：

```text
docs-site/src/content/docs/backend/services/xuan-product/database.md
docs-site/src/content/docs/backend/services/xuan-sales/database.md
docs-site/src/content/docs/backend/services/xuan-inventory/database.md
```

服务自己的 Flyway migration 放在业务工程内：

```text
xuan-product/src/main/resources/db/migration/
```

## 推荐记录内容

每个服务的 `database.md` 至少包含：

- 数据库或 schema 名称。
- 当前 migration 最新版本。
- 表清单。
- 每张表的字段说明。
- 主键、唯一约束、普通索引。
- tenantId 隔离规则。
- 逻辑删除规则。
- 初始化数据说明。
- 与其它服务的外部 ID 或快照字段。

## 表结构格式

```md
## product

商品主档表。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | bigint | 是 |  | 主键 |
| tenant_id | bigint | 是 |  | 租户 ID |
| code | varchar(64) | 是 |  | 商品编码 |
| name | varchar(128) | 是 |  | 商品名称 |
| deleted | tinyint | 是 | 0 | 逻辑删除 |
| created_at | datetime | 是 |  | 创建时间 |
| updated_at | datetime | 是 |  | 更新时间 |

### 索引

| 名称 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- |
| uk_product_tenant_code | tenant_id, code | unique | 同一租户商品编码唯一 |
| idx_product_name | tenant_id, name | normal | 商品名称搜索 |

### 关联说明

- 不直接关联销售、采购、库存服务数据库。
- 其它服务只保存 `productId` 和必要商品快照字段。
```

## 与 Flyway 的关系

- 新增或修改表结构时，先新增 Flyway migration，再同步更新对应服务的 `database.md`。
- `database.md` 必须记录当前服务已知最新 migration 版本。
- 文档不替代 migration，不能只改文档不写迁移。
- migration 不解释业务语义，复杂设计意图要写入 `database.md`。

## 多租户要求

业务数据表默认必须包含 `tenant_id`。平台级配置、全局字典、系统级权限定义可以不带 `tenant_id`，但必须在文档中明确说明原因。

## 跨服务数据规则

微服务之间不直接跨库 JOIN。跨服务引用只能保存：

- 对方服务的业务 ID。
- 必要的快照字段。
- 事件同步过来的只读字段。

复杂报表和首页统计进入 `xuan-query` 的读模型，不让业务服务之间互相读取数据库。
