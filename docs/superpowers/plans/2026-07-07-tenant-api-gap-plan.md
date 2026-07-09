# xuan-tenant 9 个接口契约缺口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐 `xuan-tenant` 的 9 个租户接口契约缺口，并补上权限注解和控制器契约测试。

**Architecture:** 继续沿用现有 `TenantController`、`TenantApplicationService`、`TenantConfigApplicationService` 分层，主接口走专用控制器和应用服务，配置接口收口到租户子资源路由。幂等处理复用 `tenant_provision_task` 现有模型做最小实现，不新增数据库迁移。

**Tech Stack:** Spring Boot, Spring MVC, Spring Security, Jakarta Validation, JUnit 5, Maven

---

### Task 1: 锁定当前红绿状态

**Files:**
- Test: `D:\xuan-erp\backend\xuan-tenant\src\test\java\com\xuan\erp\tenant\TenantApplicationServiceCrudTest.java`
- Test: `D:\xuan-erp\backend\xuan-tenant\src\test\java\com\xuan\erp\tenant\TenantConfigApplicationServiceCrudTest.java`
- Test: `D:\xuan-erp\backend\xuan-tenant\src\test\java\com\xuan\erp\tenant\TenantRequestValidationTest.java`

- [ ] 运行现有定向测试，确认哪些已经覆盖，哪些仍然失败。
- [ ] 记录当前缺口集中在分页、详情聚合、配置子资源路由还是权限/契约测试。

### Task 2: 补齐租户主档应用服务

**Files:**
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\main\java\com\xuan\erp\tenant\application\service\TenantApplicationService.java`
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\main\java\com\xuan\erp\tenant\domain\repository\TenantRepository.java`
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\test\java\com\xuan\erp\tenant\TenantApplicationServiceCrudTest.java`

- [ ] 先补失败测试，覆盖分页列表、详情聚合和幂等写接口行为。
- [ ] 最小修改应用服务和仓储接口，使测试转绿。

### Task 3: 收口租户配置子资源接口

**Files:**
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\main\java\com\xuan\erp\tenant\interfaces\controller\TenantConfigController.java`
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\main\java\com\xuan\erp\tenant\application\service\TenantConfigApplicationService.java`
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\main\java\com\xuan\erp\tenant\interfaces\assembler\TenantConfigAssembler.java`
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\main\java\com\xuan\erp\tenant\interfaces\dto\TenantConfigRequest.java`
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\test\java\com\xuan\erp\tenant\TenantConfigApplicationServiceCrudTest.java`

- [ ] 先补失败测试，明确配置分页和按 `tenantId + configKey` 更新的契约。
- [ ] 最小修改控制器与应用服务，使配置主契约切到 `/api/tenants/{id}/configs`。

### Task 4: 补齐权限注解和控制器契约测试

**Files:**
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\main\java\com\xuan\erp\tenant\interfaces\controller\TenantController.java`
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\main\java\com\xuan\erp\tenant\interfaces\controller\TenantConfigController.java`
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\test\java\com\xuan\erp\tenant\TenantOpenApiDocumentationTest.java`
- Modify: `D:\xuan-erp\backend\xuan-tenant\src\test\java\com\xuan\erp\tenant\TenantRequestValidationTest.java`
- Create or Modify: `D:\xuan-erp\backend\xuan-tenant\src\test\java\com\xuan\erp\tenant\TenantControllerContractTest.java`

- [ ] 先补失败测试，覆盖权限注解、分页参数和配置子资源路由绑定。
- [ ] 最小添加 `@PreAuthorize` 与参数校验，保持控制器只做协议适配。

### Task 5: 回归验证

**Files:**
- Verify only

- [ ] 运行 `xuan-tenant` 定向测试集。
- [ ] 若需要，再运行 `mvn -pl xuan-tenant -am test` 做模块级回归。
- [ ] 汇总仍未覆盖或暂缓的点，明确说明没有碰 migration。
