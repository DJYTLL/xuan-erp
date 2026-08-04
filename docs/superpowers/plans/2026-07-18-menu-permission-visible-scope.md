# Menu Permission Visible Scope Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep page dependency permissions visible in the detail panel while preventing dependency-only menus from appearing in the assignment tree.

**Architecture:** `MenuPermissionAssignment.vue` will maintain two separate concepts: visible menu permissions, driven only by direct permissions under a menu and its visible descendants, and selectable permission codes, which still include page dependencies for visible pages. Verification stays in the existing frontend script that guards the shared assignment component contract.

**Tech Stack:** Vue 3 Composition API, Vite verification scripts, Element Plus UI.

---

### Task 1: Guard Dependency-Only Menu Visibility

**Files:**
- Modify: `frontend/scripts/verify-menu-permission-assignment.mjs`
- Modify: `frontend/src/framework/components/MenuPermissionAssignment.vue`

- [ ] **Step 1: Write the failing verification**

Add source-contract assertions that require a separate visible permission state and filtering helper:

```js
for (const marker of [
  'visiblePermissionCodes',
  'pruneInvisiblePermissionNodes',
  'node.visiblePermissionCodes.length || node.children.length',
]) {
  assert(componentSource.includes(marker), `MenuPermissionAssignment should separate menu visibility with ${marker}.`);
}
```

- [ ] **Step 2: Run verification to confirm RED**

Run: `npm --prefix frontend run verify:menu-permission-assignment`

Expected: failure mentioning `visiblePermissionCodes`.

- [ ] **Step 3: Implement visible-vs-selectable state**

Add `visiblePermissionCodes` to `PermissionTreeNode`. Compute it from direct permissions and visible child nodes only. Keep `permissionCodes` as direct + page dependencies + visible child codes. Prune nodes with no direct visible permissions and no visible children.

- [ ] **Step 4: Run targeted verification to confirm GREEN**

Run: `npm --prefix frontend run verify:menu-permission-assignment`

Expected: success.

### Task 2: Regression Verification

**Files:**
- Verify only.

- [ ] **Step 1: Run permission tree and role guard checks**

Run:

```bash
npm --prefix frontend run verify:permission-menu-tree
npm --prefix frontend run verify:role-grant-guard
```

Expected: both succeed.

- [ ] **Step 2: Run frontend build**

Run: `npm --prefix frontend run build`

Expected: typecheck and Vite build succeed.
