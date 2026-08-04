# Tenant Init Role Templates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete tenant plan -> IAM init template -> role template -> IAM role -> user permission chain.

**Architecture:** IAM remains the authority for roles and permissions. Tenant plans keep selecting an IAM init template through `feature_flags.iamInitTemplateCode`; IAM role templates define which tenant roles are created, which permissions they receive, and which roles are assigned to the initial administrator.

**Tech Stack:** PostgreSQL/Flyway, PL/pgSQL bootstrap function, Java contract tests, Vue permission snapshot consumption, docs-site Markdown, Obsidian project notes.

---

### Task 1: IAM Migration Contract

**Files:**
- Modify: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamMigrationContractTest.java`
- Create: `backend/xuan-iam/src/main/resources/db/migration/V21__add_tenant_init_role_templates.sql`

- [ ] Add a failing migration contract test that requires V21 to create `iam_tenant_init_role_template`, seed `tenant_readonly`, `tenant_admin`, and `tenant_owner`, and replace `bootstrap_iam_tenant`.
- [ ] Run `mvn -pl backend/xuan-iam -Dtest=IamMigrationContractTest test` and verify the new test fails because V21 is missing.
- [ ] Add V21 migration with the role template table, seeds, and bootstrap function replacement.
- [ ] Re-run the migration contract test and verify it passes.

### Task 2: Bootstrap Role Template Rules

**Files:**
- Modify: `backend/xuan-iam/src/main/resources/db/migration/V21__add_tenant_init_role_templates.sql`
- Modify: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamMigrationContractTest.java`

- [ ] Ensure basic creates `tenant_readonly` and assigns it to the initial admin.
- [ ] Ensure standard creates `tenant_admin` and also keeps `tenant_readonly` available without default assignment.
- [ ] Ensure full creates `tenant_owner`, `tenant_admin`, and `tenant_readonly`, assigning only `tenant_owner` to the initial admin.
- [ ] Ensure role permissions are constrained by the selected init template permissions.
- [ ] Ensure old default-role bindings are soft-deleted when switching templates.

### Task 3: Documentation

**Files:**
- Modify: `docs-site/src/content/docs/backend/services/xuan-iam/database.md`
- Modify: `docs-site/src/content/docs/backend/services/xuan-iam/permissions.md`
- Modify: `docs-site/src/content/docs/backend/services/xuan-tenant/permissions.md`
- Modify: `docs-site/src/content/docs/security/iam.md`

- [ ] Document the new role template table.
- [ ] Document the matrix: plan -> init template -> role templates -> assigned admin role.
- [ ] Document that plans define capability boundaries, roles define actual user permissions, and front end only consumes permission snapshots.

### Task 4: Obsidian Record

**Files:**
- Modify: `E:\备份\Obsidian\XuanObsidian\开发\xuan-erp\02-当前看板\项目开发看板.md`
- Modify or create an appropriate IAM service progress note under `E:\备份\Obsidian\XuanObsidian\开发\xuan-erp\05-服务推进\`.

- [ ] Record the completed business rule and the added IAM V21 migration.
- [ ] Record the new default role matrix.

### Task 5: Verification

**Commands:**
- `mvn -pl backend/xuan-iam test`
- `npm run verify:product-management-permissions`
- `npm run verify:navigation-permission-contract`
- `npm run build`

- [ ] Run all commands fresh.
- [ ] Report exact pass/fail status and any residual risk.
