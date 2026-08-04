# Column Permission Tenant Assignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete platform column-permission templates, tenant template assignment, and role binding restricted to assigned tenant templates.

**Architecture:** IAM owns platform templates, tenant-template assignment records, role-template bindings, and authorization snapshot refresh. The tenant frontend page opens a tenant template assignment dialog; the role frontend page only lists templates assigned to the role tenant.

**Tech Stack:** Spring Boot, MyBatis, Flyway, PostgreSQL, Vue 3, Element Plus, Vite verification scripts.

---

### Task 1: Database Contract

**Files:**
- Create: `backend/xuan-iam/src/main/resources/db/migration/V36__add_tenant_column_permission_template_assignments.sql`
- Modify: `frontend/scripts/verify-column-permission-management.mjs`

- [ ] Write verifier assertions for the V36 migration and tenant assignment API names.
- [ ] Run `npm run verify:column-permission-management` from `frontend` and confirm it fails because V36/API/frontend hooks do not exist yet.
- [ ] Add V36 with `iam_tenant_column_permission_template_assignment`, active unique index `(tenant_id, template_id)`, and Chinese comments.

### Task 2: IAM Backend

**Files:**
- Modify/Create IAM command, query, domain, repository, mapper, controller, DTO, and contract test files around `IamColumnPermissionApplicationService`.

- [ ] Add tenant assignment request/response/query types.
- [ ] Add repository methods to list, replace, and check tenant assigned templates.
- [ ] Add controller endpoints under `/api/iam/column-permissions/tenants/{tenantId}/templates`.
- [ ] Ensure role binding only accepts templates assigned to that tenant, while platform templates remain managed with `tenantId = 0`.
- [ ] Refresh affected tenant authorization snapshots after assignment or role binding changes.

### Task 3: Frontend

**Files:**
- Modify: `frontend/src/api/iamAdmin.ts`
- Modify: `frontend/src/types/iamAdmin.ts`
- Modify: `frontend/src/views/TenantManagementView.vue`
- Modify: `frontend/src/views/IamRoleManagementView.vue`
- Modify: `frontend/scripts/verify-column-permission-management.mjs`
- Modify: `frontend/scripts/verify-tenant-management-page.mjs`

- [ ] Add tenant column template assignment API functions and types.
- [ ] Add a tenant-management table action for assigning column permission templates.
- [ ] Build the assignment dialog with platform template choices and assigned/default state.
- [ ] Update role column-permission binding to list only templates assigned to the role tenant.

### Task 4: Verification

- [ ] Run `npm run verify:column-permission-management` from `frontend`.
- [ ] Run `npm run verify:tenant-management-page` from `frontend`.
- [ ] Run `npm run build` from `frontend`.
- [ ] Run targeted IAM Maven tests from repo root.
- [ ] Run scoped `git diff --check` on touched files.
