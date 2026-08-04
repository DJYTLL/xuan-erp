# Column Permission Workbench Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 IAM 列权限页面从旧的模板列表弹窗模式改成左树右侧工作台，并兼容现有模板接口完成保存。

**Architecture:** 前端通过一个本地桥接配置把 `menuCode` 映射到已有 `resourceKey`，左侧继续复用共享的 `NavigationMenuTree`，右侧改成“轻量信息头 + 字段配置器 + 真实预览”。模板数据仍通过现有 IAM 接口读写，不新增后端协议。

**Tech Stack:** Vue 3、Element Plus、Pinia、现有 IAM Admin API、Node 校验脚本

---

### Task 1: 先让验证脚本表达新工作台结构

**Files:**
- Modify: `frontend/scripts/verify-column-permission-management.mjs`
- Test: `frontend/package.json`

- [ ] **Step 1: 写出面向新结构的断言**

```js
assert(pageSource.includes('NavigationMenuTree'), 'Column permission page should render the shared navigation menu tree.');
assert(pageSource.includes('字段配置器'), 'Column permission page should expose the field configuration workbench.');
assert(pageSource.includes('页面真实预览'), 'Column permission page should render the real preview panel.');
assert(pageSource.includes('selectedTemplateId'), 'Column permission page should keep a selected template state.');
assert(pageSource.includes('selectedMenuCode'), 'Column permission page should keep a selected menu state.');
assert(pageSource.includes('listIamMenus'), 'Column permission page should load IAM menus for the navigation tree.');
```

- [ ] **Step 2: 运行脚本确认先失败**

Run: `npm run verify:column-permission-management`  
Expected: FAIL，提示缺少 `NavigationMenuTree` 或新工作台文案

### Task 2: 建立页面到资源字段的桥接配置

**Files:**
- Create: `frontend/src/config/columnPermissionPages.ts`
- Test: `frontend/scripts/verify-column-permission-management.mjs`

- [ ] **Step 1: 新建桥接配置文件**

```ts
export interface ColumnPermissionPageConfig {
  menuCode: string;
  resourceKeys: string[];
  previewRows: Array<Record<string, string>>;
  previewSearchPlaceholder: string;
}

export const columnPermissionPageConfigs: ColumnPermissionPageConfig[] = [
  {
    menuCode: 'tenant-management',
    resourceKeys: ['tenant'],
    previewSearchPlaceholder: '搜索租户编码 / 名称 / 联系人',
    previewRows: [],
  },
];
```

- [ ] **Step 2: 给租户管理补示例预览数据**

```ts
previewRows: [
  {
    code: 'acme',
    name: '华南测试租户',
    status: '启用',
    currentPlanName: '专业版',
    primaryDomain: 'acme.xuan.local',
    contactName: '陈晓',
    contactPhone: '13812345678',
    remark: '重点客户',
  },
]
```

### Task 3: 重写列权限页面工作台

**Files:**
- Modify: `frontend/src/views/IamColumnPermissionManagementView.vue`
- Modify: `frontend/src/api/iamAdmin.ts`
- Modify: `frontend/src/types/iamAdmin.ts`
- Test: `frontend/scripts/verify-column-permission-management.mjs`

- [ ] **Step 1: 顶部查询区切到模板选择模式**

```vue
<QueryToolbar>
  <el-input v-model.number="tenantId" class="query-input" placeholder="租户 ID，0 为平台模板" />
  <el-select v-model="selectedTemplateId" class="template-filter" placeholder="选择列权限模板">
    <el-option
      v-for="template in templateOptions"
      :key="template.value"
      :label="template.label"
      :value="template.value"
    />
  </el-select>
  <el-input v-model="menuKeyword" class="query-input" placeholder="搜索页面菜单" clearable />
</QueryToolbar>
```

- [ ] **Step 2: 接入左侧菜单树**

```vue
<NavigationMenuTree
  v-model="selectedMenuCode"
  title="导航菜单树"
  all-node-title="全部页面"
  :menus="menus"
  :keyword="menuKeyword"
  :count-resolver="countColumnsByMenuCodes"
  @node-select="handleMenuNodeSelect"
/>
```

- [ ] **Step 3: 实现右侧 V5 版型工作区**

```vue
<section class="column-workbench">
  <section class="column-editor-panel">
    <header class="column-editor-head">...</header>
    <div class="field-grid">...</div>
  </section>
  <section class="column-preview-panel">...</section>
</section>
```

- [ ] **Step 4: 加入模板规则加载与保存**

```ts
async function loadTemplateItems(templateId: number | null) {
  resetRules();
  if (!templateId) return;
  const items = await getIamColumnPermissionTemplateItems(templateId);
  for (const item of items) {
    ruleForm[item.resourceColumnId] = item.accessMode;
  }
}

async function saveTemplateRules() {
  await setIamColumnPermissionTemplateItems(
    activeTemplate.value.id,
    resourceColumns.value.map((column) => ({
      resourceColumnId: column.id,
      accessMode: ruleForm[column.id] || 'VISIBLE',
    })),
    authStore.username,
  );
}
```

- [ ] **Step 5: 加入显示/隐藏与预览映射规则**

```ts
function toggleColumnVisibility(column: IamResourceColumn, checked: boolean) {
  ruleForm[column.id] = checked ? resolvePreferredVisibleMode(column) : 'HIDDEN';
}

function resolvePreferredVisibleMode(column: IamResourceColumn): AccessMode {
  const initial = initialRuleForm.value[column.id];
  if (initial && initial !== 'HIDDEN') {
    return initial;
  }
  return column.maskType ? 'MASKED' : 'VISIBLE';
}
```

### Task 4: 跑验证链并收尾

**Files:**
- Modify: `frontend/src/views/IamColumnPermissionManagementView.vue`
- Test: `frontend/scripts/verify-column-permission-management.mjs`

- [ ] **Step 1: 跑页面专项验证**

Run: `npm run verify:column-permission-management`  
Expected: PASS with `Verified IAM column permission management page contract.`

- [ ] **Step 2: 跑类型检查或构建**

Run: `npm run build`  
Expected: PASS，`vue-tsc --noEmit` 和 `vite build` 全部成功

- [ ] **Step 3: 人工核对关键交互**

Run: `npm run dev`  
Expected: `/system/iam/column-permissions` 页面出现左侧菜单树、字段配置器、真实预览，并能切换模板后保存规则
