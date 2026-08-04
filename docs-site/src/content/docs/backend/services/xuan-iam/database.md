---
title: "xuan-iam 数据库结构"
---

本文记录 `xuan-iam` 的数据库结构、表字段、索引、初始化数据和 Flyway migration 对应关系。

## 数据库归属

| 项 | 内容 |
| --- | --- |
| 数据库 | `xuan_iam` |
| migration 目录 | `xuan-iam/src/main/resources/db/migration/` |
| 当前 migration 最新版本 | `V26__add_tenant_init_template_binding.sql` |
| 本次新增 migration | `V26__add_tenant_init_template_binding.sql` |
| 数据所有权 | 只允许 `xuan-iam` 直接写入用户、角色、权限、菜单和列权限授权数据 |

## migration 规则

涉及数据库结构、字段、索引、约束、初始化数据或 Flyway 脚本时，必须先扫描本服务 `db/migration` 目录，确认当前最高版本号，再追加新 migration。禁止跳号、复用版本号、修改历史 migration。

本次扫描结果：本服务最高版本为 `V25__add_tenant_permission_entitlements.sql`，本次按顺序追加 `V26__add_tenant_init_template_binding.sql`。当前未发现版本冲突；后续如涉及表、字段、索引、约束、初始化数据或函数调整，应从 `V27__*.sql` 顺序追加。

## 设计说明

- 本服务使用独立数据库 `xuan_iam`，服务启动时 Flyway 连接该数据库执行 migration。
- migration 只管理当前连接数据库内的表、索引、约束和初始化数据；不包含 `CREATE DATABASE`，也不创建 PostgreSQL schema。
- 跨服务只保存外部 ID、编码和必要快照字段，不建立跨服务数据库外键。
- 数据库保留表结构、主键、查询索引和关键唯一索引；状态枚举等业务规则由应用逻辑校验。
- IAM 是账号、权限、菜单和授权关系事实源；账号名、角色编码、权限编码、菜单编码、授权关系等关键唯一性由数据库唯一索引兜底，状态枚举等业务规则由应用逻辑校验。
- IAM 是权限事实源，业务服务只维护权限清单，最终权限和菜单 seed 落到本服务。
- 采用租户内账号模型，`iam_user.tenant_id + username` 是账号唯一边界；`iam_user_tenant` 记录账号在当前租户内的成员身份和租户管理员状态。
- 用户、角色、用户租户关系和授权快照等租户内数据包含 `tenant_id`；菜单和权限定义是全局定义，不带 `tenant_id`。
- 生产租户菜单授权由 `xuan-tenant` 开通流程传入真实 `tenantId` 后触发 `bootstrap_iam_tenant(p_tenant_id bigint, p_requested_by varchar)` 完成。V1 不再硬编码 `tenant_id = 1`。
- V2 新增 `iam_outbox_event`，并扩展 `bootstrap_iam_tenant` 在本地事务内写入 `IamTenantBootstrapped` outbox 事件；重复触发时通过确定性 `event_id` 保持事件幂等。
- V3 扩展租户初始化入口，支持创建租户管理员账号，并初始化平台 `super_admin`。
- V4 将旧的 `read/manage` 权限目录对齐到统一的 `view/create/update/delete` 模板，并刷新 `super_admin` 为 `* + 当前全部有效权限`。
- V15 注册全服务基础权限，补齐 Gateway、IAM、Tenant、Audit、Product、Party、Warehouse、Inventory、Sales、Procurement、Finance、Document、Manufacturing、Query 等服务的基础权限，并把 `full` 初始化模板扩展到全量权限。
- V16 新增租户套餐权限 `tenant-plan:view`、`tenant-plan:manage`、`tenant-plan:assign`，并将套餐管理菜单绑定到 `tenant-plan:view`。
- V17 支持平台管理员账号别名 `superadmin`，让 `super_admin` 和 `superadmin` 都能拿到平台管理员授权快照。
- V18 正式废弃历史 `tenant:lifecycle` 授权，迁移到 `tenant:enable` / `tenant:disable`；历史码只作为迁移兼容来源。
- V19 将 `bootstrap_iam_tenant` 主流程改为初始化模板驱动，支持 `iamInitTemplateCode` 决定租户初始化权限、菜单、管理员、角色权限、授权快照和 outbox 事件，同时保留旧 2 参数 / 7 参数兼容入口。
- V20 在 V19 基础上继续收敛模板语义：初始化时会按模板同步 `tenant_admin` 角色权限和租户菜单，模板切换时会软删除旧模板多余授权。
- V21 新增 `iam_tenant_init_role_template`，让套餐的 `iamInitTemplateCode` 先选中 IAM 初始化模板，再由初始化角色模板生成租户内角色、角色权限、管理员角色绑定和授权快照；模板切换时会收敛旧模板角色绑定和多余授权。
- V22 退役未落地页面旧占位菜单，避免无页面菜单进入当前用户菜单树。
- V23 修复导航分组父级关系，确保菜单层级与前端路由一致。
- V24 恢复已落地业务导航菜单，并刷新授权快照中的菜单码。
- V25 新增 `iam_tenant_permission_entitlement` 租户权限池；初始化模板先同步为租户权限上限，角色授权只能在权限池内选择，权限池减少时会级联裁剪所有越界角色权限。
- V26 新增 `iam_tenant_init_template_binding` 稳定记录租户当前归属的初始化模板；模板修改时按绑定表、历史 bootstrap 事件和权限池明细三路查找受影响租户，避免历史租户或权限池明细缺失导致不同步。

## 核心表清单

| 表名 | 说明 | 是否包含 tenant_id |
| --- | --- | --- |
| `iam_user` | 用户账号表。 | 是 |
| `iam_user_tenant` | 用户租户关系表，租户内账号模型下记录账号在当前租户内的成员身份。 | 是 |
| `iam_role` | 角色表。 | 是 |
| `iam_permission` | 权限定义表，业务服务权限清单最终同步到本表。 | 否 |
| `iam_menu` | 菜单表。 | 否 |
| `iam_user_role` | 用户角色关联表。 | 是 |
| `iam_role_permission` | 角色权限关联表。 | 是 |
| `iam_tenant_init_template_binding` | 租户初始化模板绑定表，稳定记录租户当前归属哪个模板。 | 是 |
| `iam_tenant_permission_entitlement` | 租户权限池表，保存租户内角色可被分配权限的上限。 | 是 |
| `iam_authorization_snapshot` | 用户授权快照表，保存 Gateway 和业务服务可缓存使用的权限、菜单和列权限快照。 | 是 |
| `iam_tenant_menu` | 租户菜单授权表。 | 是 |
| `iam_tenant_init_role_template` | 租户 IAM 初始化角色模板表，定义每个初始化模板要生成哪些角色、角色权限，以及哪些角色自动授予租户管理员。 | 否 |
| `iam_tenant_column_setting` | 租户列权限默认配置。 | 是 |
| `iam_role_column_setting` | 角色列权限配置。 | 是 |
| `iam_user_table_setting` | 用户表格个性化配置。 | 是 |
| `iam_refresh_token` | 刷新令牌表。 | 是 |
| `iam_tenant_bootstrap_task` | 租户 IAM 初始化任务记录，记录 xuan-tenant 开通流程传入真实 tenantId 后的菜单授权初始化结果。 | 是 |
| `iam_outbox_event` | IAM 服务 Outbox 事件表。 | 是 |
| `undo_log` | Seata AT 模式回滚日志表 | 否 |

## 初始化数据

| 表名 | 初始化条目 | 说明 |
| --- | --- | --- |
| `iam_permission` | `tenant:view/create/update/delete/enable/disable`, `tenant-config:view/manage`, `tenant-plan:view/manage/assign`, `iam-menu:*`, `iam-permission:*`, `iam-role:*`, `iam-user:*`, `iam-init-template:*`, `component-center:view`, 各业务服务 `view/create/update/delete/enable/disable/audit/import/export` 等标准权限 | 全局权限清单，业务服务后续可通过同步机制继续补充。V15 补齐全服务基础权限，V16 补齐租户套餐权限，V18 后租户生命周期正式使用 `tenant:enable` / `tenant:disable`，V31 将系统设置 IAM 管理页从历史 `iam:*` 拆分为页面级权限。 |
| `iam_menu` | `workbench`, `product`, `party`, `warehouse`, `inventory`, `sales`, `procurement`, `finance`, `report`, `system` 以及后台管理菜单 | 全局菜单目录，权限和租户授权可在此基础上扩展。V4 后菜单入口权限使用对应服务的 `*:view`，V16 将套餐管理菜单绑定到 `tenant-plan:view`。 |
| `iam_tenant_init_role_template` | `basic/standard/full` 初始化角色模板 | V21 让 `iamInitTemplateCode` 真正驱动租户初始化角色和授权：`basic` 默认授予 `tenant_readonly`，`standard` 默认授予 `tenant_admin`，`full` 默认授予 `tenant_owner`。 |
| `iam_tenant_init_template_binding` | V26 从历史 `IamTenantBootstrapped` outbox 和当前租户权限池回填；后续由权限池同步维护 | 模板修改时的受影响租户查询来源，不能再依赖权限池明细是否刚好存在。 |
| `iam_tenant_permission_entitlement` | V25 从历史角色权限和初始化模板回填；后续由模板同步维护 | 租户权限池是角色授权上限。模板降级或套餐切换会先替换权限池，再删除所有超出权限池的角色权限。 |
| `iam_tenant_menu` | V19-V25 的 `bootstrap_iam_tenant(...)` | 生产租户菜单授权由 `xuan-tenant` 开通流程传入真实 `tenantId` 和 `iamInitTemplateCode` 后幂等初始化；V25 会按初始化模板同步租户权限池、菜单、角色、角色权限、管理员角色绑定和授权快照，收敛旧模板多余授权。 |
| `iam_tenant_bootstrap_task` | `iam:tenant:{tenantId}:bootstrap:v1` | 记录租户 IAM 初始化结果，支持重复触发时按幂等键识别。 |
| `iam_outbox_event` | `IamTenantBootstrapped` | V2 由 `bootstrap_iam_tenant` 写入，事件 Topic 为 `iam.tenant.bootstrapped`。 |

## `iam_user`

用户账号表。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `username` | `varchar(100)` | 是 | `` | 用户名 |
| `password_hash` | `varchar(255)` | 是 | `` | 密码哈希 |
| `display_name` | `varchar(200)` | 否 | `` | 显示名 |
| `email` | `varchar(200)` | 否 | `` | 邮箱 |
| `phone` | `varchar(50)` | 否 | `` | 手机号 |
| `avatar_url` | `varchar(500)` | 否 | `` | 头像 URL |
| `is_enabled` | `boolean` | 是 | `true` | 是否启用 |
| `account_non_expired` | `boolean` | 是 | `true` | 账号是否未过期 |
| `account_non_locked` | `boolean` | 是 | `true` | 账号是否未锁定 |
| `credentials_non_expired` | `boolean` | 是 | `true` | 凭证是否未过期 |
| `last_login_at` | `timestamptz` | 否 | `` | 最近登录时间 |
| `password_changed_at` | `timestamptz` | 否 | `` | 最近密码修改时间 |
| `failed_login_count` | `integer` | 是 | `0` | 连续登录失败次数 |
| `last_failed_login_at` | `timestamptz` | 否 | `` | 最近登录失败时间 |
| `locked_until` | `timestamptz` | 否 | `` | 锁定截止时间 |
| `mfa_enabled` | `boolean` | 是 | `false` | 是否启用多因素认证 |
| `auth_version` | `bigint` | 是 | `0` | 权限版本 |
| `remark` | `varchar(500)` | 否 | `` | 备注 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_user` | `id` | primary | 主键 |
| `idx_iam_user_username_active` | `tenant_id, username; WHERE deleted_at IS NULL` | unique | 同一租户用户名唯一 |
| `idx_iam_user_tenant_deleted` | `tenant_id, deleted_at` | normal | 租户活动数据过滤；唯一性由应用逻辑保证 |
| `idx_iam_user_phone` | `tenant_id, phone` | normal | 手机号查询；唯一性由应用逻辑保证 |

### 逻辑约束

- 应用层校验 `auth_version >= 0`。

### 跨服务引用

- `tenant_id` 引用 `xuan-tenant.tenant.id`，不建数据库外键。

## `iam_user_tenant`

用户租户关系表，租户内账号模型下记录账号在当前租户内的成员身份。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `user_id` | `bigint` | 是 | `` | 用户 ID |
| `user_type` | `varchar(40)` | 是 | `'TENANT_USER'` | 用户类型：TENANT_USER/PLATFORM_ADMIN |
| `membership_status` | `varchar(30)` | 是 | `'ACTIVE'` | 成员状态：ACTIVE/SUSPENDED/LEFT |
| `is_tenant_admin` | `boolean` | 是 | `false` | 是否租户管理员 |
| `joined_at` | `timestamptz` | 是 | `now()` | 加入租户时间 |
| `last_selected_at` | `timestamptz` | 否 | `` | 最近选择该租户时间 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_user_tenant` | `id` | primary | 主键 |
| `idx_iam_user_tenant_user_active` | `tenant_id, user_id; WHERE deleted_at IS NULL` | unique | 用户租户关系活动态唯一 |
| `idx_iam_user_tenant_user` | `user_id, deleted_at` | normal | 按用户查询可进入租户；唯一性由应用逻辑保证 |

### 逻辑约束

- 应用层校验 `user_id` 必须引用有效的 `iam_user.id`。
- 应用层校验 `user_type IN ('TENANT_USER','PLATFORM_ADMIN')`。
- 应用层校验 `membership_status IN ('ACTIVE','SUSPENDED','LEFT')`。

## `iam_role`

角色表。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `code` | `varchar(100)` | 是 | `` | 角色编码 |
| `name` | `varchar(200)` | 是 | `` | 角色名称 |
| `description` | `varchar(500)` | 否 | `` | 角色描述 |
| `is_enabled` | `boolean` | 是 | `true` | 是否启用 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_role` | `id` | primary | 主键 |
| `idx_iam_role_code_active` | `tenant_id, code; WHERE deleted_at IS NULL` | unique | 同一租户角色编码唯一 |
| `idx_iam_role_tenant_deleted` | `tenant_id, deleted_at` | normal | 租户活动数据过滤；唯一性由应用逻辑保证 |

## `iam_permission`

权限定义表，业务服务权限清单最终同步到本表。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `code` | `varchar(150)` | 是 | `` | 权限编码 |
| `name` | `varchar(200)` | 是 | `` | 权限名称 |
| `service_name` | `varchar(80)` | 是 | `` | 来源服务 |
| `menu_code` | `varchar(100)` | 否 | `` | 所属菜单编码 |
| `description` | `varchar(500)` | 否 | `` | 权限描述 |
| `is_enabled` | `boolean` | 是 | `true` | 是否启用 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_permission` | `id` | primary | 主键 |
| `idx_iam_permission_code_active` | `code; WHERE deleted_at IS NULL` | unique | 权限编码全局唯一 |
| `idx_iam_permission_service` | `service_name, deleted_at` | normal | 按服务查询权限；唯一性由应用逻辑保证 |

## `iam_menu`

菜单表。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `code` | `varchar(100)` | 是 | `` | 菜单编码 |
| `parent_id` | `bigint` | 否 | `` | 父菜单 ID |
| `title` | `varchar(200)` | 是 | `` | 菜单标题 |
| `i18n_key` | `varchar(120)` | 否 | `` | 国际化键 |
| `path` | `varchar(200)` | 否 | `` | 前端路由路径 |
| `icon` | `varchar(100)` | 否 | `` | 图标 |
| `permission_code` | `varchar(150)` | 否 | `` | 进入菜单所需权限 |
| `sort_no` | `integer` | 是 | `0` | 排序 |
| `is_enabled` | `boolean` | 是 | `true` | 是否启用 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_menu` | `id` | primary | 主键 |
| `idx_iam_menu_code_active` | `code; WHERE deleted_at IS NULL` | unique | 菜单编码全局唯一 |
| `idx_iam_menu_parent` | `parent_id, sort_no` | normal | 菜单树排序；唯一性由应用逻辑保证 |

### 逻辑约束

- 应用层校验 `parent_id` 必须引用有效的 `iam_menu.id`。
- 应用层校验 `sort_no >= 0`。

## `iam_user_role`

用户角色关联表。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `user_id` | `bigint` | 是 | `` | 用户 ID |
| `role_id` | `bigint` | 是 | `` | 角色 ID |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_user_role` | `id` | primary | 主键 |
| `idx_iam_user_role_key_active` | `tenant_id, user_id, role_id; WHERE deleted_at IS NULL` | unique | 用户角色活动态唯一 |
| `idx_iam_user_role_user` | `tenant_id, user_id, deleted_at` | normal | 用户授权查询；唯一性由应用逻辑保证 |

### 逻辑约束

- 应用层校验 `user_id` 必须引用有效的 `iam_user.id`。
- 应用层校验 `role_id` 必须引用有效的 `iam_role.id`。

## `iam_role_permission`

角色权限关联表。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `role_id` | `bigint` | 是 | `` | 角色 ID |
| `permission_id` | `bigint` | 是 | `` | 权限 ID |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_role_permission` | `id` | primary | 主键 |
| `idx_iam_role_permission_key_active` | `tenant_id, role_id, permission_id; WHERE deleted_at IS NULL` | unique | 角色权限活动态唯一 |
| `idx_iam_role_permission_role` | `tenant_id, role_id, deleted_at` | normal | 角色权限查询；唯一性由应用逻辑保证 |

### 逻辑约束

- 应用层校验 `role_id` 必须引用有效的 `iam_role.id`。
- 应用层校验 `permission_id` 必须引用有效的 `iam_permission.id`。

## `iam_authorization_snapshot`

用户授权快照表，保存 Gateway 和业务服务可缓存使用的权限、菜单和列权限快照。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `user_id` | `bigint` | 是 | `` | 用户 ID |
| `auth_version` | `bigint` | 是 | `0` | 权限版本 |
| `role_ids` | `jsonb` | 是 | `'[]'::jsonb` | 角色 ID 列表 |
| `permission_codes` | `jsonb` | 是 | `'[]'::jsonb` | 权限编码列表 |
| `menu_codes` | `jsonb` | 是 | `'[]'::jsonb` | 菜单编码列表 |
| `column_settings` | `jsonb` | 是 | `'{}'::jsonb` | 列权限配置快照 |
| `snapshot_hash` | `varchar(128)` | 否 | `` | 快照哈希 |
| `expires_at` | `timestamptz` | 否 | `` | 快照过期时间 |
| `built_at` | `timestamptz` | 是 | `now()` | 快照生成时间 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_authorization_snapshot` | `id` | primary | 主键 |
| `idx_iam_authorization_snapshot_user` | `tenant_id, user_id` | unique | 用户授权快照唯一 |
| `idx_iam_authorization_snapshot_expires` | `expires_at` | normal | 快照过期清理；唯一性由应用逻辑保证 |

### 逻辑约束

- 应用层校验 `user_id` 必须引用有效的 `iam_user.id`。
- 应用层校验 `auth_version >= 0`。
- 当前授权快照只持久化角色、权限、菜单和 `column_settings`。接口响应中的 `fieldPermissions`、`dataScopes`、`stateActionRules` 当前仍是预留契约，默认返回空 `Map` / 空 `List` / 空 `Map`，尚未落到独立表或真实规则引擎。

## `iam_tenant_menu`

租户菜单授权表。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `menu_id` | `bigint` | 是 | `` | 菜单 ID |
| `is_enabled` | `boolean` | 是 | `true` | 是否启用 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_tenant_menu` | `id` | primary | 主键 |
| `idx_iam_tenant_menu_key_active` | `tenant_id, menu_id; WHERE deleted_at IS NULL` | unique | 租户菜单活动态唯一 |
| `idx_iam_tenant_menu_tenant` | `tenant_id, deleted_at` | normal | 租户菜单查询；唯一性由应用逻辑保证 |

### 逻辑约束

- 应用层校验 `menu_id` 必须引用有效的 `iam_menu.id`。

## `iam_tenant_init_template_binding`

租户初始化模板绑定表。它解决“模板修改后找不到归属租户”的问题：租户属于哪个初始化模板必须独立记录，不能依赖 `iam_tenant_permission_entitlement` 明细行是否存在。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `init_template_code` | `varchar(64)` | 是 | `` | 当前绑定的 IAM 初始化模板编码 |
| `last_entitlement_version` | `bigint` | 是 | `1` | 最近一次同步到租户权限池的版本号 |
| `is_enabled` | `boolean` | 是 | `true` | 是否启用 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_tenant_init_template_binding` | `id` | primary | 主键 |
| `uk_iam_tenant_init_template_binding_tenant_active` | `tenant_id; WHERE deleted_at IS NULL` | unique | 同一租户活动态只能绑定一个初始化模板 |
| `idx_iam_tenant_init_template_binding_template` | `init_template_code, tenant_id; WHERE deleted_at IS NULL` | normal | 按模板查找受影响租户 |

### 逻辑约束

- 模板权限修改时，受影响租户优先从本表查询，并用历史 `IamTenantBootstrapped` outbox 和当前权限池明细兜底。
- `iam_tenant_permission_entitlement` 写入或更新时会通过触发器维护本表，避免后续模板修改查不到租户。
- 本表只记录“租户归属哪个初始化模板”，不表达具体权限清单；具体权限上限仍以 `iam_tenant_permission_entitlement` 为准。

## `iam_tenant_permission_entitlement`

租户权限池表。它是“套餐与权限矩阵”的权限上限落点：套餐选择 `iamInitTemplateCode`，IAM 将初始化模板权限同步为当前租户权限池；租户管理员只能在该权限池内给本租户角色分配权限。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `init_template_code` | `varchar(64)` | 否 | `` | 来源初始化模板编码 |
| `permission_id` | `bigint` | 是 | `` | 权限 ID |
| `entitlement_version` | `bigint` | 是 | `1` | 租户权限池版本 |
| `is_enabled` | `boolean` | 是 | `true` | 是否启用 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_tenant_permission_entitlement` | `id` | primary | 主键 |
| `uk_iam_tenant_permission_entitlement_active` | `tenant_id, permission_id; WHERE deleted_at IS NULL` | unique | 同一租户同一权限活动态唯一 |
| `idx_iam_tenant_permission_entitlement_template` | `init_template_code, tenant_id; WHERE deleted_at IS NULL` | normal | 按初始化模板同步受影响租户权限池 |

### 逻辑约束

- `permission_id` 必须引用有效且启用的 `iam_permission.id`。
- 角色授权写入前必须校验请求权限码都存在于当前租户权限池。
- 权限池减少时，所有租户角色中超出权限池的 `iam_role_permission` 必须被软删除，删除原因统一为 `tenant permission entitlement reduced`。
- 平台 `super_admin` / `superadmin` 是平台级旁路，不受普通租户权限池限制。

## `iam_tenant_init_role_template`

租户 IAM 初始化角色模板表。它是“套餐与权限矩阵”的权威落点之一：套餐中的 `feature_flags.iamInitTemplateCode` 只负责选择初始化模板，实际进入租户的角色和权限由本表决定。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigserial` | 是 | `` | 主键 |
| `init_template_code` | `varchar(64)` | 是 | `` | 初始化模板编码，对应套餐功能开关中的 `iamInitTemplateCode` |
| `role_code` | `varchar(100)` | 是 | `` | 要在租户内生成或同步的角色编码 |
| `role_name` | `varchar(200)` | 是 | `` | 角色名称 |
| `role_description` | `text` | 否 | `` | 角色说明 |
| `permission_codes` | `jsonb` | 是 | `'[]'::jsonb` | 该角色模板声明的权限码清单，最终还会与初始化模板权限边界取交集 |
| `assign_to_admin` | `boolean` | 是 | `false` | 是否自动授予租户管理员账号 |
| `sort_order` | `integer` | 是 | `0` | 同一模板内排序 |
| `is_enabled` | `boolean` | 是 | `true` | 是否启用 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |
| `deleted_by` | `varchar(100)` | 否 | `` | 删除人 |
| `delete_reason` | `varchar(500)` | 否 | `` | 删除原因 |
| `deleted_at` | `timestamptz` | 否 | `` | 逻辑删除时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_tenant_init_role_template` | `id` | primary | 主键 |
| `uk_iam_tenant_init_role_template_active` | `init_template_code, role_code; WHERE deleted_at IS NULL` | unique | 同一初始化模板下角色编码唯一 |
| `idx_iam_tenant_init_role_template_code` | `init_template_code, is_enabled, sort_order; WHERE deleted_at IS NULL` | normal | 按初始化模板加载活动角色规则 |

### 逻辑约束

- `init_template_code` 必须存在于 `iam_tenant_init_permission_template.code`。
- `permission_codes` 只能声明 `iam_permission` 中存在且未删除的权限码。
- 初始化时最终角色权限 = `iam_tenant_init_role_template.permission_codes` 与 `iam_tenant_init_permission_template.permission_codes` 的交集，避免角色模板越过套餐边界。
- `bootstrap_iam_tenant` 只自动收敛本表中声明的受管角色，不会删除租户后续手工创建的业务角色。

## `iam_tenant_column_setting`

租户列权限默认配置。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `page_key` | `varchar(120)` | 是 | `` | 页面标识 |
| `visible_columns` | `jsonb` | 是 | `` | 可见列清单 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_tenant_column_setting` | `id` | primary | 主键 |
| `idx_iam_tenant_column_page` | `tenant_id, page_key` | unique | 租户页面列配置唯一 |

## `iam_role_column_setting`

角色列权限配置。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `role_id` | `bigint` | 是 | `` | 角色 ID |
| `page_key` | `varchar(120)` | 是 | `` | 页面标识 |
| `visible_columns` | `jsonb` | 是 | `` | 可见列清单 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_role_column_setting` | `id` | primary | 主键 |
| `idx_iam_role_column_page` | `tenant_id, role_id, page_key` | unique | 角色页面列配置唯一 |

### 逻辑约束

- 应用层校验 `role_id` 必须引用有效的 `iam_role.id`。

## `iam_user_table_setting`

用户表格个性化配置。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `user_id` | `bigint` | 是 | `` | 用户 ID |
| `page_key` | `varchar(120)` | 是 | `` | 页面标识 |
| `config_json` | `jsonb` | 是 | `` | 表格配置 JSON |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_user_table_setting` | `id` | primary | 主键 |
| `idx_iam_user_table_page` | `tenant_id, user_id, page_key` | unique | 用户页面配置唯一 |

### 逻辑约束

- 应用层校验 `user_id` 必须引用有效的 `iam_user.id`。

## `iam_refresh_token`

刷新令牌表。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `user_id` | `bigint` | 是 | `` | 用户 ID |
| `token_hash` | `varchar(64)` | 是 | `` | 令牌哈希 |
| `token_family_id` | `varchar(64)` | 是 | `` | 令牌族 ID，用于 refresh token 轮换追踪 |
| `expires_at` | `timestamptz` | 是 | `` | 过期时间 |
| `revoked_at` | `timestamptz` | 否 | `` | 撤销时间 |
| `replaced_by_token_hash` | `varchar(64)` | 否 | `` | 被轮换后的新令牌哈希 |
| `last_used_at` | `timestamptz` | 否 | `` | 最近使用时间 |
| `audience_tenant_id` | `bigint` | 否 | `` | 目标租户 ID |
| `device_id` | `varchar(100)` | 否 | `` | 设备 ID |
| `ip_address` | `varchar(64)` | 否 | `` | 最近使用 IP |
| `user_agent` | `varchar(500)` | 否 | `` | 最近使用 User-Agent |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_refresh_token` | `id` | primary | 主键 |
| `idx_iam_refresh_token_hash` | `token_hash` | unique | 令牌哈希唯一 |
| `idx_iam_refresh_token_user` | `tenant_id, user_id, expires_at` | normal | 用户令牌查询；唯一性由应用逻辑保证 |
| `idx_iam_refresh_token_family` | `token_family_id, user_id` | normal | 令牌族查询；唯一性由应用逻辑保证 |

### 逻辑约束

- 应用层校验 `user_id` 必须引用有效的 `iam_user.id`。

### 跨服务引用

- `audience_tenant_id` 引用 `xuan-tenant.tenant.id`，不建数据库外键。

## `iam_tenant_bootstrap_task`

租户 IAM 初始化任务记录，记录 xuan-tenant 开通流程传入真实 tenantId 后的菜单授权初始化结果。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `task_key` | `varchar(160)` | 是 | `` | 初始化任务键 |
| `status` | `varchar(30)` | 是 | `'SUCCEEDED'` | 任务状态：SUCCEEDED/FAILED |
| `idempotency_key` | `varchar(200)` | 是 | `` | 幂等键 |
| `requested_by` | `varchar(100)` | 否 | `` | 触发人或来源服务 |
| `menu_grant_count` | `integer` | 是 | `0` | 本次新增菜单授权数量 |
| `started_at` | `timestamptz` | 是 | `now()` | 开始时间 |
| `finished_at` | `timestamptz` | 否 | `` | 完成时间 |
| `last_error_message` | `text` | 否 | `` | 最近错误信息 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_tenant_bootstrap_task` | `id` | primary | 主键 |
| `idx_iam_tenant_bootstrap_task_key` | `tenant_id, task_key` | unique | 租户初始化任务键唯一 |
| `idx_iam_tenant_bootstrap_idempotency` | `idempotency_key` | unique | 租户初始化幂等键唯一 |

### 逻辑约束

- 应用层校验 `status IN ('SUCCEEDED','FAILED')`。

## `iam_outbox_event`

IAM 服务 Outbox 事件表，在本地事务内记录待发布领域事件，避免业务写入成功但事件丢失。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint GENERATED BY DEFAULT AS IDENTITY` | 是 | `` | 主键 |
| `tenant_id` | `bigint` | 是 | `` | 租户 ID，来源于 xuan-tenant |
| `event_id` | `varchar(160)` | 是 | `` | 事件 ID |
| `aggregate_type` | `varchar(80)` | 是 | `'IAM_TENANT'` | 聚合类型 |
| `aggregate_id` | `varchar(120)` | 是 | `` | 聚合 ID |
| `event_type` | `varchar(120)` | 是 | `` | 事件类型 |
| `topic` | `varchar(160)` | 是 | `` | 事件主题 |
| `payload` | `jsonb` | 是 | `'{}'::jsonb` | 事件载荷 |
| `headers` | `jsonb` | 是 | `'{}'::jsonb` | 事件头 |
| `status` | `varchar(30)` | 是 | `'PENDING'` | 发布状态 |
| `retry_count` | `integer` | 是 | `0` | 已重试次数 |
| `max_retry_count` | `integer` | 是 | `10` | 最大重试次数 |
| `locked_by` | `varchar(100)` | 否 | `` | 当前发布锁持有者 |
| `locked_at` | `timestamptz` | 否 | `` | 锁定时间 |
| `lock_expires_at` | `timestamptz` | 否 | `` | 锁过期时间 |
| `next_retry_at` | `timestamptz` | 是 | `now()` | 下次重试时间 |
| `published_at` | `timestamptz` | 否 | `` | 发布时间 |
| `last_error_code` | `varchar(120)` | 否 | `` | 最近错误码 |
| `last_error_message` | `text` | 否 | `` | 最近错误信息 |
| `first_failed_at` | `timestamptz` | 否 | `` | 首次失败时间 |
| `dead_letter_at` | `timestamptz` | 否 | `` | 进入死信时间 |
| `created_by` | `varchar(100)` | 否 | `` | 创建人 |
| `created_at` | `timestamptz` | 是 | `now()` | 创建时间 |
| `updated_by` | `varchar(100)` | 否 | `` | 更新人 |
| `updated_at` | `timestamptz` | 是 | `now()` | 更新时间 |

### 主键与索引

| 名称 | 字段/表达式 | 类型 | 说明 |
| --- | --- | --- | --- |
| `pk_iam_outbox_event` | `id` | primary | 主键 |
| `idx_iam_outbox_event_id` | `event_id` | unique | 事件幂等键唯一 |
| `idx_iam_outbox_event_pending` | `status, next_retry_at, id` | normal | 待发布事件扫描 |
| `idx_iam_outbox_event_lock` | `status, lock_expires_at, id` | normal | 发布锁超时扫描 |
| `idx_iam_outbox_event_dead_letter` | `status, dead_letter_at, id` | normal | 死信事件扫描 |
| `idx_iam_outbox_event_aggregate` | `aggregate_type, aggregate_id, id` | normal | 聚合事件追踪 |

### 逻辑约束

- `bootstrap_iam_tenant` 写入 `IamTenantBootstrapped`，确定性事件 ID 为 `iam:tenant:{tenantId}:bootstrap:v1:event`。
- 应用层或发布器校验 `status IN ('PENDING','PUBLISHING','PUBLISHED','FAILED','DEAD_LETTERED')`。

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
