---
title: "xuan-query 数据库结构"
---

本文记录 `xuan-query` 的数据库结构、表字段、索引、约束、初始化数据和 Flyway migration 对应关系。

## 数据库归属

| 项 | 内容 |
| --- | --- |
| 数据库/Schema | `xuan_query` |
| migration 目录 | `xuan-query/src/main/resources/db/migration/` |
| 数据所有权 | 只允许 `xuan-query` 直接写入 |

## 表设计要求

- 业务表默认包含 `tenant_id`，系统全局表必须在表说明中明确标注“全局表”。
- 租户内唯一约束必须把 `tenant_id` 放入组合唯一索引。
- 所有写模型表必须包含创建人、创建时间、更新人、更新时间和逻辑删除字段。
- 金额、数量、成本等字段必须明确精度。
- 历史表、流水表、审计表不允许物理删除。

## migration 规则

涉及数据库结构、字段、索引、约束、初始化数据或 Flyway 脚本时，必须先扫描本服务 `db/migration` 目录，确认当前最高版本号，再追加新 migration。禁止跳号、复用版本号、修改历史 migration。

## 核心表清单

| 表名 | 说明 | 是否包含 tenant_id |
| --- | --- | --- |
| 后续实现时补齐 | 根据实际业务模型补齐 | 是 |
