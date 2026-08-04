# Schema Column-Aware Browse Table Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a schema-driven `XuanBrowseTable` that supports `VISIBLE / MASKED / HIDDEN` column permissions and migrate the Component Center browse-table demo onto that protocol.

**Architecture:** Keep `XuanBrowseTable` as the shared rendering surface, add a schema/permission layer beside the existing preference layer, and migrate the Component Center demo first while preserving compatibility for current non-schema callers. Column permission resolution stays inside the table component, but permission data loading remains outside the component.

**Tech Stack:** Vue 3, TypeScript, Element Plus, existing framework preference adapter, Node-based verification scripts, `vue-tsc`.

---

### Task 1: Write the Schema Contract and Red Test

**Files:**
- Create: `D:\xuan-erp\frontend\src\framework\components\browseTableSchema.ts`
- Create: `D:\xuan-erp\frontend\scripts\verify-browse-table-schema.mjs`
- Modify: `D:\xuan-erp\frontend\package.json`

- [ ] **Step 1: Add a failing verification script for schema browse table markers**

Create a new verifier that checks for:
- exported schema helpers in `browseTableSchema.ts`
- `schema` input support in `XuanBrowseTable.vue`
- permission-aware markers such as `MASKED`, `HIDDEN`, `toolbar-action`, `row-action`
- Component Center consuming a `browseTableSchema`

- [ ] **Step 2: Run the verifier and confirm it fails**

Run: `npm run verify:browse-table-schema`

Expected: fail because the schema file, component API, and Component Center schema usage do not exist yet.

- [ ] **Step 3: Add the initial schema type file**

Create typed contracts for:
- column access mode
- permission lookup key helpers
- column schema
- toolbar action schema
- row action schema
- top-level table schema

- [ ] **Step 4: Register the verifier in package.json**

Add:

```json
"verify:browse-table-schema": "node scripts/verify-browse-table-schema.mjs"
```

- [ ] **Step 5: Re-run the verifier and confirm it still fails for missing implementation**

Run: `npm run verify:browse-table-schema`

Expected: fail on `XuanBrowseTable.vue` / `ComponentCenterView.vue` markers, proving the test is targeting the missing feature rather than a typo.

### Task 2: Refactor XuanBrowseTable to Schema + Column Permission Mode

**Files:**
- Modify: `D:\xuan-erp\frontend\src\framework\components\XuanBrowseTable.vue`
- Modify: `D:\xuan-erp\frontend\src\framework\components\browseTablePreferences.ts`
- Create or Modify: `D:\xuan-erp\frontend\src\framework\components\browseTableSchema.ts`
- Modify: `D:\xuan-erp\frontend\scripts\verify-browse-table-preferences.mjs`

- [ ] **Step 1: Extend the preference input path to work from schema columns**

Keep the existing stored preference shape, but let defaults be built from schema column metadata (`visibleByDefault`, `fixed`, width, title).

- [ ] **Step 2: Add schema-aware props and normalized runtime schema**

`XuanBrowseTable.vue` should accept a `schema` prop and normalize either:
- schema mode
- legacy `columns` mode

This keeps current pages compiling while moving the component toward schema-first rendering.

- [ ] **Step 3: Add column permission helpers**

Implement helpers for:
- building permission lookup keys
- resolving a column's effective access mode
- masking visible values
- deciding whether a column can be toggled by the user

- [ ] **Step 4: Refactor rendering to use normalized schema**

Render from normalized schema for:
- toolbar
- data columns
- row actions
- empty state
- pagination defaults

Retain compatibility for existing slot-based consumers until they migrate.

- [ ] **Step 5: Make the column settings panel permission-aware**

Add UI states for:
- visible columns
- masked columns
- permission-hidden columns
- columns disabled from user toggling

The panel must not let a permission-hidden column be re-enabled.

- [ ] **Step 6: Update the preference verifier**

Add assertions that confirm:
- schema types are imported
- permission helpers are present
- hidden columns are filtered at runtime
- masked columns keep visibility but use masking behavior

- [ ] **Step 7: Run the browse-table verifiers**

Run:
- `npm run verify:browse-table`
- `npm run verify:browse-table-schema`

Expected: both pass.

### Task 3: Migrate the Component Center Browse Table Demo

**Files:**
- Modify: `D:\xuan-erp\frontend\src\views\ComponentCenterView.vue`
- Modify: `D:\xuan-erp\frontend\scripts\verify-component-center-collapsible.mjs`
- Modify: `D:\xuan-erp\frontend\scripts\verify-component-center-first.mjs`

- [ ] **Step 1: Replace demo columns + slots with a schema object**

Move the browse-table demo to:
- schema-defined toolbar actions
- schema-defined columns
- schema-defined row actions
- schema-defined empty state

- [ ] **Step 2: Add mock column permission snapshot**

Use the demo to show all three states:
- one column `VISIBLE`
- one column `MASKED`
- one column `HIDDEN`

- [ ] **Step 3: Wire toolbar and row actions through emitted action keys**

Component Center should respond to schema action events with demo `ElMessage` feedback instead of local template slots.

- [ ] **Step 4: Update component-center verifiers**

Assert that:
- the browse table demo uses `browseTableSchema`
- the old `browseColumns` array is gone
- schema permission snapshot markers exist

- [ ] **Step 5: Run component-center verifiers**

Run:
- `npm run verify:component-center-collapsible`
- `npm run verify:component-center-first`
- `npm run verify:browse-table-schema`

Expected: pass.

### Task 4: Final Verification

**Files:**
- Verify only

- [ ] **Step 1: Run focused verification scripts**

Run:
- `npm run verify:browse-table`
- `npm run verify:browse-table-schema`
- `npm run verify:component-center-collapsible`
- `npm run verify:component-center-first`

- [ ] **Step 2: Run type and build verification**

Run: `npm run build`

Expected: `vue-tsc --noEmit` and Vite build both pass.

- [ ] **Step 3: Run diff hygiene**

Run: `git diff --check -- frontend/package.json frontend/scripts/verify-browse-table-preferences.mjs frontend/scripts/verify-browse-table-schema.mjs frontend/scripts/verify-component-center-collapsible.mjs frontend/scripts/verify-component-center-first.mjs frontend/src/framework/components/browseTablePreferences.ts frontend/src/framework/components/browseTableSchema.ts frontend/src/framework/components/XuanBrowseTable.vue frontend/src/views/ComponentCenterView.vue`

Expected: no whitespace or patch-format issues.
