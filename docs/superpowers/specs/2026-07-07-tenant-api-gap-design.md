# xuan-tenant 9 个接口契约缺口设计

## 目标

只补齐 `xuan-tenant` 当前与以下 9 个接口相关的契约缺口，不扩到其他租户周边接口：

- `GET /api/tenants`
- `GET /api/tenants/{id}`
- `POST /api/tenants`
- `PUT /api/tenants/{id}`
- `POST /api/tenants/{id}/enable`
- `POST /api/tenants/{id}/disable`
- `DELETE /api/tenants/{id}`
- `GET /api/tenants/{id}/configs`
- `PUT /api/tenants/{id}/configs/{key}`

## 本次范围

- 将租户列表收口为分页查询。
- 将租户详情收口为聚合详情，补入当前套餐、主域名、生命周期摘要。
- 为租户写接口补入幂等入口字段和最小幂等处理。
- 将租户配置改为租户子资源路由，不再以 `/api/tenant-configs` 作为主契约。
- 顺手补齐权限注解和控制器契约测试。

## 明确不做

- 不新增或修改任何 Flyway migration。
- 不扩到 `status-history`、`provision-tasks`、内部接口或套餐/域名/联系人完整 CRUD。
- 不引入新的基础设施中间件。
- 不做前端代码联动。

## 设计决策

### 1. 租户主档接口

- `GET /api/tenants` 返回 `ApiResponse<PageResult<TenantResponse>>`，支持 `pageNum`、`pageSize`。
- `GET /api/tenants/{id}` 返回聚合详情 DTO，包含：
  - 主档基本信息
  - 当前套餐摘要
  - 主域名摘要
  - 生命周期摘要
- `POST/PUT/enable/disable/DELETE` 保持现有主档服务入口，但统一补 `idempotencyKey`。

### 2. 幂等处理

- 使用现有 `tenant_provision_task` 仓储接口做最小幂等记录。
- 若未传 `idempotencyKey`，按现有行为执行。
- 若传入 `idempotencyKey`：
  - 同一 `taskKey + idempotencyKey` 已成功：返回已处理结果。
  - 同一 `taskKey + idempotencyKey` 处理中：拒绝重复提交。

### 3. 配置子资源路由

- `GET /api/tenants/{id}/configs`：按租户分页返回配置。
- `PUT /api/tenants/{id}/configs/{key}`：按 `tenantId + configKey` 定位更新。
- 配置列表继续遵守敏感值脱敏规则。
- 旧 `/api/tenant-configs` 代码若已存在，可保留底层服务能力，但主控制器契约切到租户子资源路由。

### 4. 权限注解

- 统一沿用仓库已有的 `@PreAuthorize("hasAuthority('...')")` 风格。
- 计划绑定：
  - `tenant:view`
  - `tenant:create`
  - `tenant:update`
  - `tenant:lifecycle`
  - `tenant:delete`
  - `tenant-config:view`
  - `tenant-config:manage`

### 5. 测试策略

- 先复用并扩展现有 `TenantApplicationServiceCrudTest`、`TenantConfigApplicationServiceCrudTest`、`TenantRequestValidationTest`。
- 新增或补齐控制器契约测试，覆盖：
  - 路由绑定
  - 分页参数
  - 请求体校验
  - 权限注解存在
  - 配置子资源路径参数绑定
