---
title: "xuan-gateway 数据库结构"
---

本文记录 `xuan-gateway` 的数据库结构、表字段、普通索引、初始化数据和 Flyway migration 对应关系。

## 数据库归属

| 项 | 内容 |
| --- | --- |
| 数据库 | `xuan_gateway` |
| migration 目录 | `xuan-gateway/src/main/resources/db/migration/` |
| 当前 migration 最新版本 | `V1` |
| 本次新增 migration | `V1__init_gateway_database.sql` |
| 数据所有权 | 网关服务当前不拥有业务数据；如后续确需持久化运行数据，使用 `xuan_gateway` 独立数据库并只允许 `xuan-gateway` 写入 |

## migration 规则

涉及数据库结构、字段、索引、约束、初始化数据或 Flyway 脚本时，必须先扫描本服务 `db/migration` 目录，确认当前最高版本号，再追加新 migration。禁止跳号、复用版本号、修改历史 migration。

本次扫描结果：本服务历史最高版本为“无”，新增 `V1`，不存在版本冲突。

## 设计说明

- 本服务使用独立数据库 `xuan_gateway`，服务启动时 Flyway 连接该数据库执行 migration。
- migration 只管理当前连接数据库内的表、索引、约束和初始化数据；不包含 `CREATE DATABASE`，也不创建 PostgreSQL schema。
- 跨服务只保存外部 ID、编码和必要快照字段，不建立跨服务数据库外键。
- 数据库只保留表结构、主键和普通查询索引；外键、唯一性、状态枚举、金额非负等业务约束全部由应用逻辑校验。
- 网关服务当前不建业务表，路由、限流和跨域配置优先放在 Nacos / Gateway 配置中。
- 保留 `xuan_gateway` 独立数据库命名是为了 Flyway 版本基线统一，后续如确需持久化网关运行数据，只能追加新 migration。
- Flyway 连接到 `xuan_gateway` 数据库后执行 migration，脚本内不创建 schema、不使用 schema 限定名。

## 核心表清单

| 表名 | 说明 | 是否包含 tenant_id |
| --- | --- | --- |
| 暂无业务表 | 网关服务当前不创建业务表，路由和限流配置优先由 Nacos / Gateway 配置管理 | 否 |
| `undo_log` | Seata AT 模式回滚日志表 | 否 |

## 初始化数据

V1 不写入业务初始化数据；本服务的业务数据由对应业务流程、事件同步或外部配置产生。

## `undo_log`

Seata AT 模式回滚日志表，用于全局事务回滚。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `branch_id` | `bigint` | 是 | `` | Seata 分支事务 ID |
| `xid` | `varchar(128)` | 是 | `` | Seata 全局事务 ID |
| `context` | `varchar(128)` | 是 | `` | 上下文 |
| `rollback_info` | `bytea` | 是 | `` | 回滚信息 |
| `log_status` | `integer` | 是 | `` | 日志状态 |
| `log_created` | `timestamp(6)` | 是 | `` | 创建时间 |
| `log_modified` | `timestamp(6)` | 是 | `` | 更新时间 |

### 主键与普通索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_undo_log` | `branch_id, xid` | primary | 主键 |
| `idx_undo_log_log_created` | `log_created` | normal | 按创建时间清理和排查回滚日志 |
