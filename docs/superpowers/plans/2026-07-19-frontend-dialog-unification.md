# Frontend Dialog Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace page-level form dialogs with the component-center `DynamicFormDialog`, and make permission-assignment dialogs use the same dialog shell without touching confirmation dialogs.

**Architecture:** Extend `DynamicFormDialog` so it can host either ordinary form fields or a custom body slot. Then migrate each target page to that one shell: simple CRUD dialogs keep using field definitions, while permission-assignment dialogs render `MenuPermissionAssignment` inside the same wrapper. Keep `ApprovalConfirmDialog`, `BatchConfirmDialog`, and `ElMessageBox.confirm` unchanged.

**Tech Stack:** Vue 3, Element Plus, TypeScript, Vite verification scripts.

---

### Task 1: Extend the shared dialog shell

**Files:**
- Modify: `frontend/src/framework/components/DynamicFormDialog.vue`
- Modify: `frontend/scripts/verify-dialog-system.mjs`

- [ ] **Step 1: Write the failing test**

Add a verification rule that the shared dialog shell supports both the normal form path and a custom body slot.

```js
assert(componentSource.includes('slot name="body"'), 'DynamicFormDialog should expose a body slot for embedded complex content.');
assert(componentSource.includes('renderForm'), 'DynamicFormDialog should support hiding the built-in form area when used as a generic dialog shell.');
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run verify:dialog-system`
Expected: FAIL because `DynamicFormDialog.vue` does not yet expose the body slot or the form toggle.

- [ ] **Step 3: Write minimal implementation**

Update `DynamicFormDialog.vue` so the existing form rendering stays intact, but a `renderForm` prop can suppress it and a named `body` slot can render custom content before the footer.

```vue
<DynamicFormDialog
  v-model="grantVisible"
  title="角色授权"
  :render-form="false"
  width="1080px"
>
  <template #body>
    <MenuPermissionAssignment ... />
  </template>
</DynamicFormDialog>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm run verify:dialog-system`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/framework/components/DynamicFormDialog.vue frontend/scripts/verify-dialog-system.mjs
git commit -m "feat: extend shared dialog shell for custom body content"
```

### Task 2: Migrate CRUD pages to the shared form dialog

**Files:**
- Modify: `frontend/src/views/IamMenuManagementView.vue`
- Modify: `frontend/src/views/IamPermissionManagementView.vue`
- Modify: `frontend/src/views/IamTenantInitTemplateManagementView.vue`
- Modify: `frontend/src/views/TenantPlanManagementView.vue`
- Modify: `frontend/src/views/TenantManagementView.vue`
- Modify: `frontend/scripts/verify-tenant-management-page.mjs`

- [ ] **Step 1: Write the failing test**

Add page-coverage checks that these pages no longer contain raw `<el-dialog>` for CRUD forms, and that tenant management no longer uses `ElMessageBox.prompt`.

```js
assert(!viewSource.includes('<el-dialog v-model="createDialogVisible"'), 'Tenant management create form should use DynamicFormDialog.');
assert(!viewSource.includes('ElMessageBox.prompt'), 'Tenant action reasons should use the shared dialog shell instead of prompt.');
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run verify:tenant-management-page`
Expected: FAIL against the current raw dialog and prompt usage.

- [ ] **Step 3: Write minimal implementation**

Replace each CRUD dialog with `DynamicFormDialog` field/section definitions, keeping the existing form models and save handlers. For tenant management, move create/edit/plan-adjust/retry/outbox reason entry into the shared dialog shell and keep the provision drawer unchanged.

```vue
<DynamicFormDialog
  v-model="createDialogVisible"
  title="创建租户"
  :fields="createTenantFields"
  :model="createForm"
  width="760px"
  label-position="top"
  :confirm-permission="'tenant:create'"
  @submit="submitCreateTenant"
/>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm run verify:tenant-management-page`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/IamMenuManagementView.vue frontend/src/views/IamPermissionManagementView.vue frontend/src/views/IamTenantInitTemplateManagementView.vue frontend/src/views/TenantPlanManagementView.vue frontend/src/views/TenantManagementView.vue frontend/scripts/verify-tenant-management-page.mjs
git commit -m "feat: move page form dialogs to shared shell"
```

### Task 3: Migrate permission assignment dialogs to the shared shell

**Files:**
- Modify: `frontend/src/views/IamRoleManagementView.vue`
- Modify: `frontend/src/views/IamUserManagementView.vue`
- Modify: `frontend/src/views/IamTenantInitTemplateManagementView.vue`
- Modify: `frontend/src/views/ComponentCenterView.vue`
- Modify: `frontend/scripts/verify-role-grant-guard.mjs`
- Modify: `frontend/scripts/verify-user-role-grant.mjs`
- Modify: `frontend/scripts/verify-menu-permission-assignment.mjs`

- [ ] **Step 1: Write the failing test**

Add assertions that the role/user/template grant dialogs no longer use raw `<el-dialog>`, and that the shared shell body hosts `MenuPermissionAssignment`.

```js
assert(roleViewSource.includes(':render-form="false"'), 'Role grant dialog should use the shared dialog shell body mode.');
assert(!roleViewSource.includes('<el-dialog v-model="grantVisible"'), 'Role grant dialog should not use raw el-dialog.');
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run verify:role-grant-guard && npm run verify:user-role-grant && npm run verify:menu-permission-assignment`
Expected: FAIL against the current raw grant dialogs.

- [ ] **Step 3: Write minimal implementation**

Keep the permission tree component intact, but render it inside `DynamicFormDialog` through the new body slot. Update the component-center preview so it demonstrates the same structure the pages now use.

```vue
<DynamicFormDialog v-model="grantVisible" title="角色授权" :render-form="false" width="1080px">
  <template #body>
    <MenuPermissionAssignment ... />
  </template>
</DynamicFormDialog>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm run verify:role-grant-guard && npm run verify:user-role-grant && npm run verify:menu-permission-assignment`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/IamRoleManagementView.vue frontend/src/views/IamUserManagementView.vue frontend/src/views/IamTenantInitTemplateManagementView.vue frontend/src/views/ComponentCenterView.vue frontend/scripts/verify-role-grant-guard.mjs frontend/scripts/verify-user-role-grant.mjs frontend/scripts/verify-menu-permission-assignment.mjs
git commit -m "feat: unify permission assignment dialogs"
```

### Task 4: Verify the whole frontend dialog surface

**Files:**
- Modify: `frontend/scripts/verify-dialog-system.mjs`
- Modify: `frontend/scripts/verify-tenant-management-page.mjs`
- Modify: `frontend/scripts/verify-role-grant-guard.mjs`
- Modify: `frontend/scripts/verify-user-role-grant.mjs`
- Modify: `frontend/scripts/verify-menu-permission-assignment.mjs`
- Modify: `frontend/package.json`

- [ ] **Step 1: Write the failing test**

Add a single dialog-surface check script that covers the target views and refuses raw `el-dialog` usage where the shared shell should now be used.

```js
assert(!source.includes('<el-dialog'), 'Target page should use DynamicFormDialog instead of raw el-dialog.');
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run verify:dialog-system`
Expected: FAIL until every target page is migrated.

- [ ] **Step 3: Write minimal implementation**

Leave confirmation dialogs alone, but make the target form/permission pages pass the new shell checks and keep the existing build clean.

```bash
npm run build
```

- [ ] **Step 4: Run test to verify it passes**

Run:
- `npm run verify:dialog-system`
- `npm run verify:tenant-management-page`
- `npm run verify:role-grant-guard`
- `npm run verify:user-role-grant`
- `npm run verify:menu-permission-assignment`
- `npm run build`

Expected: all commands exit 0.

- [ ] **Step 5: Commit**

```bash
git add frontend/package.json frontend/scripts/verify-dialog-system.mjs frontend/scripts/verify-tenant-management-page.mjs frontend/scripts/verify-role-grant-guard.mjs frontend/scripts/verify-user-role-grant.mjs frontend/scripts/verify-menu-permission-assignment.mjs
git commit -m "feat: verify shared dialog usage across frontend"
```

