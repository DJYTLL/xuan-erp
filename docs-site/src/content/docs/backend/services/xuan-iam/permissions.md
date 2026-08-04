---
title: "xuan-iam 权限文档"
---

本文记录 `xuan-iam` 的菜单、接口权限、列权限和权限同步规则。权限目录最终都落在 `xuan-iam`，业务服务只维护自己拥有的权限清单。

## 当前落地范围

当前 IAM 已经落地并用于前端运行态的是菜单权限、路由权限、按钮权限、角色权限授权和当前用户授权快照：

- `menus` 来自 `iam_menu`、`iam_tenant_menu` 和当前用户角色授权。
- `routePermissions` / `buttonPermissions` 来自 `iam_permission`、`iam_tenant_permission_entitlement` 与 `iam_role_permission`；角色授权必须先落在当前租户权限池内，当前用户权限接口优先按实时角色权限计算，避免初始化模板或角色授权变化后继续使用旧快照。
- `columnPermissions` 来自授权快照里的 `column_settings`；没有配置时返回空对象。
- `fieldPermissions` 当前仍是预留契约，返回空对象。
- `dataScopes` 当前仍是预留契约，返回空列表。
- `stateActionRules` 当前仍是预留契约，返回空对象。

也就是说，当前阶段不要把字段权限、数据范围权限、状态动作权限理解为已经真实配置完成；它们只是先把前后端响应结构占住，后续再补表结构、管理页面和业务服务执行逻辑。

## 菜单与 pageKey

| 项 | 值 |
| --- | --- |
| 菜单 code | `iam-menu-management` / `iam-permission-management` / `iam-role-management` / `iam-user-management` / `iam-init-template-management` |
| 默认 pageKey | 对应页面菜单 code |
| 权限前缀 | `iam-menu` / `iam-permission` / `iam-role` / `iam-user` / `iam-init-template` |

## 接口权限

| 权限码 | 动作 | 说明 |
| --- | --- | --- |
| `iam-menu:view/create/update` | 菜单管理 | 查看、新增、修改、启停菜单 |
| `iam-permission:view/create/update` | 权限管理 | 查看、新增、修改、启停权限定义 |
| `iam-role:view/create/update` | 角色授权 | 查看角色、新增角色、修改角色和保存角色权限 |
| `iam-user:view/create/update/reset-password/delete` | 用户授权 | 查看用户、新增用户、修改用户资料、保存用户角色、重置密码、停用用户 |
| `iam-init-template:view/create/update` | 初始化模板 | 查看、新增、修改模板和保存模板权限 |
| `component-center:view` | 组件中心 | 查看组件中心 |
| `iam:audit` | 审核 | 审核、反审核、红冲等强业务动作，按需启用 |
| `iam:export` | 导出 | 导出列表或明细数据，按需启用 |

`iam:view/create/update/delete` 仅作为历史迁移兼容来源保留；V31 会把已有旧授权等价补齐到页面级权限，新增页面和按钮不得继续共用 `iam:*`。

## 套餐初始化角色矩阵

租户套餐不直接给用户授权。套餐的 `feature_flags.iamInitTemplateCode` 只选择 IAM 初始化模板；IAM 会把租户和模板的关系稳定记录到 `iam_tenant_init_template_binding`，再按 `iam_tenant_init_permission_template` 生成租户权限池 `iam_tenant_permission_entitlement`，并按 `iam_tenant_init_role_template` 生成受管租户角色、角色权限、租户菜单、管理员角色绑定和授权快照。

| 初始化模板 | 受管角色 | 自动授予管理员 | 权限边界 |
| --- | --- | --- | --- |
| `basic` | `tenant_readonly` | 是 | 只读基础资料、库存和往来单位等查询权限 |
| `standard` | `tenant_readonly`, `tenant_admin` | `tenant_admin` | 标准进销存、单据、导入导出、审核等权限，不默认包含删除和系统级 IAM 管理 |
| `full` | `tenant_readonly`, `tenant_admin`, `tenant_owner` | `tenant_owner` | 全业务权限和租户内 IAM 管理权限，不包含平台租户管理和套餐管理权限 |

规则要求：

- 初始化模板权限会同步为租户权限池，租户内角色授权只能在该权限池内选择；`IAM_TENANT_PERMISSION_OUT_OF_SCOPE` 表示试图把权限池外的权限授给角色。
- `iam-menu:*` 和 `iam-permission:*` 是平台级目录维护权限；租户管理员做角色授权时只需要 `iam-role:view/update`，授权弹窗的可选权限必须来自角色授权接口返回的租户权限池元数据，不能依赖全局菜单目录或权限目录接口。
- 模板修改时，受影响租户必须从 `iam_tenant_init_template_binding` 查询，并用历史 bootstrap 事件和权限池明细兜底；不能只依赖权限池明细反查。
- 租户管理员可以维护本租户下的角色权限，但只能在当前租户权限池内增删；套餐升级只扩大可选范围，不自动授予所有角色新权限。
- 切换套餐或初始化模板时，`bootstrap_iam_tenant(...)` 必须重新同步租户权限池、租户菜单、受管角色、角色权限、管理员角色绑定和授权快照。
- 权限池减少时，所有租户角色中超出新权限池的 `iam_role_permission` 都必须被软删除，避免降级后仍能看到按钮或访问接口。
- `super_admin` / `superadmin` 是平台账号旁路；普通租户管理员只能拿到当前租户初始化模板授予的角色权限。
- 前端菜单和按钮必须消费 `GET /api/iam/menus/current` 与 `GET /api/iam/permissions/current`，本地权限判断必须 fail-closed：没有权限数据时只能展示无需权限的公共动作。

## 列权限

| pageKey | 字段范围 | 说明 |
| --- | --- | --- |
| `iam` | 用户手机号、邮箱、角色授权明细等敏感列 | 当前已有 `iam_tenant_column_setting`、`iam_role_column_setting` 和授权快照 `column_settings`；具体页面字段清单和管理入口后续按页面补齐 |

## 同步规则

- 权限清单在代码提交阶段由 CI 扫描和校验。
- 部署前由受控脚本或 migration 同步到 `xuan-iam`。
- 服务启动时只允许做本地清单自检和告警，不建议直接写 IAM 生产库。
- 新增页面必须同时补菜单、路由 meta、接口权限、列权限映射和回归测试。
- 字段权限、数据范围权限和状态动作权限在真实落地前，只能作为空结构返回，不应被前端或业务服务当成已授权事实。
