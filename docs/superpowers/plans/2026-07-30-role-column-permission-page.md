# Role Column Permission Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract role column permissions into an independent page where each role configures field access rules inside the tenant-assigned column permission scope.

**Architecture:** Platform column templates define reusable field rules. Tenant assignment defines the maximum column scope for a tenant. Role column permission rules are persisted independently per tenant, role, and resource column; authorization snapshots merge role rules directly instead of using role-template binding as the final grant.

**Tech Stack:** Spring Boot, MyBatis, Flyway, PostgreSQL, Vue 3, Element Plus, Vite verification scripts.

---

### Task 1: Contract Tests

- [ ] Add backend migration/controller/service contract assertions for V37 role column rules and `/roles/{roleId}/column-permissions`.
- [ ] Add frontend verifier assertions for `/system/iam/role-column-permissions`, `IamRoleColumnPermissionManagementView.vue`, and removal of the old role template select flow.
- [ ] Run targeted frontend/backend checks and confirm they fail before implementation.

### Task 2: Backend Role Rules

- [ ] Add V37 `iam_role_column_permission_rule` with `tenant_id`, `role_id`, `resource_column_id`, and `access_mode`.
- [ ] Add command/domain/DTO/repository/mapper methods to query and replace role column rules.
- [ ] Enforce role rules only use resource columns included in the tenant assigned template pool.
- [ ] Change runtime column permission merge to read role rules directly.
- [ ] Refresh affected user authorization snapshots after role column rules change.

### Task 3: Frontend Independent Page

- [ ] Add route/menu/i18n for `角色列权限`.
- [ ] Create `IamRoleColumnPermissionManagementView.vue`.
- [ ] The page selects tenant, role, and page/menu, then configures field access in the same style as the column permission workbench.
- [ ] Remove the old role table `列权限` template-binding action from `IamRoleManagementView.vue`.

### Task 4: Verification

- [ ] Run frontend column permission and navigation verifiers.
- [ ] Run frontend build.
- [ ] Run targeted IAM Maven tests with `-am`.
- [ ] Run scoped whitespace checks.
