---
title: "xuan-iam 接口文档"
---

本文记录 `xuan-iam` 的认证、当前用户授权、管理接口和内部接口口径。实际开发时，每个接口必须继续补齐请求参数、响应结构、错误码、幂等规则和审计要求。

## 当前认证与授权接口

| 方法 | 路径 | 调用方 | 权限码 | 说明 |
| --- | --- | --- | --- | --- |
| POST | `/api/iam/auth/login` | Vue 管理端 | 无 | 使用 `tenantCode + username + password` 登录，返回 access token、refresh token 和当前用户摘要；`tenantId` 仅保留兼容旧调用 |
| POST | `/api/iam/auth/refresh` | Vue 管理端 | 无 | 使用 refresh token 轮换，返回新的 access token 和新的 refresh token |
| POST | `/api/iam/auth/logout` | Vue 管理端 | 无 | 撤销 refresh token；access token 不进 Redis 黑名单，等待自然过期 |
| GET | `/api/iam/auth/current-user` | Vue 管理端 | 登录态 | 查询当前用户 |
| GET | `/api/iam/menus/current` | Vue 管理端 | 登录态 | 查询当前用户可见菜单树 |
| GET | `/api/iam/permissions/current` | Vue 管理端 | 登录态 | 查询当前用户权限快照 |
| GET | `/.well-known/jwks.json` | Gateway / 业务服务 | 无 | 发布 JWT 验签公钥 |

当前 logout 策略是只撤销 refresh token：同一个 refresh token 再用于刷新会失败并进入审计；已签发的 access token 不会立刻失效，只能等短有效期自然过期。当前阶段不引入 Redis access-token 黑名单。

登录入口面向用户使用租户编码：Vue 管理端提交 `tenantCode`，IAM 先通过 `xuan-tenant` 的 `/internal/tenants/by-code/{tenantCode}/status` 解析租户并校验状态，再使用解析出的内部 `tenantId` 查询用户、签发 token 和加载权限快照。平台超级管理员可使用 `platform` 或 `0` 作为平台租户编码；系统内部仍以 `tenantId` 作为权限、角色、审计和 token 上下文的稳定标识。

当前权限快照中已用于前端接入的是 `menus`、`routePermissions`、`buttonPermissions`、`columnPermissions` 和 `authVersion`。其中 `columnPermissions` 来自 `column_settings`，没有配置时为空对象；`fieldPermissions`、`dataScopes`、`stateActionRules` 当前仍是预留契约，只返回空结构。

## 管理接口清单

| 方法 | 路径 | 调用方 | 权限码 | 说明 |
| --- | --- | --- | --- | --- |
| GET | `/api/iam/users` | Vue 管理端 | `iam-user:view` | 用户列表查询 |
| GET | `/api/iam/users/{id}` | Vue 管理端 | `iam-user:view` | 用户详情查询 |
| POST | `/api/iam/users` | Vue 管理端 | `iam-user:create` | 创建用户 |
| PUT | `/api/iam/users/{userId}` | Vue 管理端 | `iam-user:update` | 修改用户显示名、联系方式、启停状态和备注 |
| POST | `/api/iam/users/{userId}/reset-password` | Vue 管理端 | `iam-user:reset-password` | 重置用户密码 |
| POST | `/api/iam/users/{userId}/disable` | Vue 管理端 | `iam-user:delete` | 停用用户 |
| GET | `/api/iam/users/{userId}/roles` | Vue 管理端 | `iam-user:view` | 查询用户角色 |
| PUT | `/api/iam/users/{userId}/roles` | Vue 管理端 | `iam-user:update` | 保存用户角色 |
| GET | `/api/iam/roles` | Vue 管理端 | `iam-role:view` | 角色列表查询 |
| POST | `/api/iam/roles` | Vue 管理端 | `iam-role:create` | 创建角色 |
| PUT | `/api/iam/roles/{roleId}` | Vue 管理端 | `iam-role:update` | 更新角色 |
| GET | `/api/iam/roles/{roleId}/permissions` | Vue 管理端 | `iam-role:view` | 查询角色权限，并返回当前租户可授权权限池编码和权限展示元数据 |
| PUT | `/api/iam/roles/{roleId}/permissions` | Vue 管理端 | `iam-role:update` | 保存角色权限 |
| GET | `/api/iam/permissions` | Vue 管理端 | `iam-permission:view` | 权限目录查询 |
| GET | `/api/iam/menus` | Vue 管理端 | `iam-menu:view` | 菜单目录查询 |
| GET | `/api/iam/authorization-snapshots` | Vue 管理端 | `iam:view` | 查询授权快照 |
| POST | `/api/iam/authorization-snapshots/rebuild` | Vue 管理端 | `iam:update` | 重建授权快照 |
| GET | `/api/iam/tenant-init-templates` | Vue 管理端 | `iam-init-template:view` | 查询初始化模板 |
| PUT | `/api/iam/tenant-init-templates/{templateId}/permissions` | Vue 管理端 | `iam-init-template:update` | 保存初始化模板权限 |

## 内部接口

| 方法 | 路径 | 调用服务 | 说明 |
| --- | --- | --- | --- |
| POST | `/internal/iam/tenant-bootstrap/{tenantId}` | `xuan-tenant` | 消费租户开通流程，按初始化模板创建租户管理员、角色权限、菜单授权和授权快照 |

## 接口要求

- Controller 只做协议适配和 DTO 转换。
- 写接口必须说明幂等键、重复提交处理和事务边界。
- 需要租户上下文的接口必须校验 `tenantId`。
- 每个前端接口必须绑定权限码，super admin 只能旁路鉴权，不能省略权限定义。
- 角色授权页必须使用 `/api/iam/roles/{roleId}/permissions` 返回的 `availablePermissionCodes` 与 `availablePermissions` 展示可分配权限；不得为了角色授权调用 `/api/iam/permissions` 或 `/api/iam/menus`，避免租户管理员被迫拥有权限目录或菜单目录维护权限。
- 内部接口必须使用服务间 token、来源服务白名单和 TraceId。
- logout、refresh 成功、refresh 失败和 refresh token 重放命中必须写审计记录。
