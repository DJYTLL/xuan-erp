import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const pageSource = read('src/views/IamColumnPermissionManagementView.vue');
const rolePageSource = read('src/views/IamRoleManagementView.vue');
const roleColumnPermissionPageSource = read('src/views/IamRoleColumnPermissionManagementView.vue');
const apiSource = read('src/api/iamAdmin.ts');
const tenantApiSource = read('src/api/tenants.ts');
const typesSource = read('src/types/iamAdmin.ts');
const routerSource = read('src/router/index.ts');
const i18nSource = read('src/i18n/messages.ts');
const permissionMapSource = read('src/config/businessPageRequiredPermissions.ts');
const pageConfigSource = read('src/config/columnPermissionPages.ts');
const tenantBrowseTableSchemaSource = read('src/config/tenantBrowseTableSchema.ts');
const iamUserBrowseTableSchemaSource = read('src/config/iamUserBrowseTableSchema.ts');

for (const component of ['ListPageShell', 'QueryToolbar', 'DynamicFormDialog', 'PermissionButton', 'NavigationMenuTree']) {
  assert(pageSource.includes(component), `Column permission page should use ${component}.`);
}

for (const snippet of [
  'viewMode',
  'templateKeyword',
  'enabledFilter',
  'filteredTemplates',
  'openTemplateDetail',
  'backToTemplateList',
  '列权限模板列表',
  '返回模板列表',
  '模板编码',
  '模板名称',
  '租户范围',
  '字段',
  '字段配置器',
  '{{ activePageTitle }}预览',
  'selectedTemplateId',
  'selectedMenuCode',
  'menuKeyword',
  'saveTemplateRules',
  'countColumnsByMenuCodes',
  'toggleColumnVisibility',
  'isMenuTreeCollapsed',
  'toggleMenuTreeCollapse',
  'startWorkbenchResize',
  'toggleEditorPanelCollapse',
  'togglePreviewPanelCollapse',
  'XuanBrowseTable',
  'previewTableSchema',
  'previewRows',
  'loadPreviewRows',
  'preview-summary-head',
  'preview-source-badge',
  'createPreviewTableSchema',
  'column.width || column.minWidth',
  'preview-page :deep(.table-shell-body)',
  'preview-page :deep(.el-table)',
  '--column-tree-width',
  'grid-template-columns: var(--column-tree-width',
  'grid-template-rows: auto minmax(0, 1fr) auto',
  'preview-page :deep(.el-scrollbar__wrap)',
  'max-width: 760px',
  'const editorPanelHeight = ref(240)',
  'const minEditorHeight = 160',
  'const minPreviewHeight = 260',
  'flex: 1 1 0',
]) {
  assert(pageSource.includes(snippet), `Column permission page should include ${snippet}.`);
}

assert(pageSource.includes('loadTemplateItems('), 'Column permission page should load template items for the selected template.');
assert(pageSource.includes('NavigationMenuTree'), 'Column permission page should render the shared navigation menu tree.');
assert(pageSource.includes('columnPermissionPageConfigs'), 'Column permission page should consume the menu/resource bridge config.');
assert(pageSource.includes('createTenantBrowseTableSchema'), 'Column permission page should reuse the shared tenant browse table schema.');
assert(!pageSource.includes('template-filter'), 'Column permission page should not expose the old template selector in the page query area.');
assert(!pageSource.includes('v-model.number="tenantId" class="query-input"'), 'Column permission detail should derive tenantId from the selected template instead of showing the old tenant input.');
assert(!pageSource.includes("label: '租户 ID'"), 'Column permission template dialog should not expose tenantId because templates are assigned to tenants later.');
assert(!/const templateFormFields[\s\S]*?key:\s*'tenantId'/.test(pageSource), 'Column permission template dialog fields should not include tenantId.');
assert(/function openCreate\(\)[\s\S]*?tenantId:\s*0,/.test(pageSource), 'Creating a column permission template should default tenantId to 0 as a platform template.');
assert(pageSource.includes('resolveTemplateTenantIdForSubmit'), 'Column permission template submit should preserve tenantId when editing and default to platform scope when creating.');
assert(!pageSource.includes('openEditCurrent'), 'Column permission detail should edit the selected template from list actions instead of a top query button.');
assert(pageSource.includes('column-tree-collapse'), 'Column permission page should render a collapse control for the left navigation tree.');
assert(pageSource.includes('workbench-divider'), 'Column permission page should render a draggable divider between editor and preview panels.');
assert(pageSource.includes('column-panel-toggle'), 'Column permission page should render panel arrow toggles for editor and preview sections.');
assert(!pageSource.includes('页面真实预览已折叠，点击右上角箭头展开。'), 'Column permission page should drop the duplicated preview title wording.');
assert(!pageSource.includes('preview-scroll-shell'), 'Column permission page should drop the extra preview scroll wrapper.');
assert(!pageSource.includes('gridTemplateColumns:'), 'Column permission workspace columns should be CSS-driven so media queries can control narrow layouts.');
assert(!pageSource.includes('@media (max-width: 1240px) {\n  .column-workspace {\n    height: auto;'), 'Column permission page should not disable the fixed workbench height on tablet-width screens.');
assert(!pageSource.includes('@media (max-width: 900px)'), 'Column permission page should keep the split preview header until true small-screen widths.');
assert(/\.field-grid\s*{[^}]*flex:\s*1 1 0;[^}]*min-height:\s*0;/s.test(pageSource), 'Field grid should own its vertical scroll area inside the editor panel.');

for (const permission of [
  'iam-column-permission:create',
  'iam-column-permission:update',
]) {
  assert(pageSource.includes(permission), `Column permission page should reference ${permission}.`);
  assert(permissionMapSource.includes(permission), `Business page permission map should include ${permission}.`);
}
assert(permissionMapSource.includes('iam-column-permission:view'), 'Business page permission map should include iam-column-permission:view.');
assert(tenantApiSource.includes('listTenants'), 'Tenant API should expose listTenants for real preview data.');
assert(tenantApiSource.includes('listColumnPermissionTenants'), 'Tenant API should expose a column-permission scoped tenant list.');
assert(tenantApiSource.includes('/api/tenants/column-permission-options'), 'Tenant API should call the column-permission scoped tenant options endpoint.');
assert(tenantBrowseTableSchemaSource.includes('createTenantBrowseTableSchema'), 'Shared tenant browse table schema should exist for preview reuse.');
assert(iamUserBrowseTableSchemaSource.includes('createIamUserBrowseTableSchema'), 'Shared IAM user browse table schema should exist for column permission reuse.');
assert(iamUserBrowseTableSchemaSource.includes('resolveIamUserDisplayName'), 'Shared IAM user browse table schema should normalize corrupted display names.');
assert(
  iamUserBrowseTableSchemaSource.includes("formatter: (row) => resolveIamUserDisplayName(row)"),
  'IAM user displayName column should render through the shared corrupted-name fallback formatter.',
);
assert(pageSource.includes('listColumnPermissionTenants'), 'Column permission page preview should use the column-permission scoped tenant list.');
assert(pageSource.includes('listIamUsers'), 'Column permission page preview should load IAM users for the user-grant preview.');

assert(routerSource.includes("path: 'system/iam/column-permissions'"), 'Router should include the column permission management route.');
assert(routerSource.includes("name: 'iam-column-permission-management'"), 'Router should name the column permission management route.');
assert(routerSource.includes("cacheName: 'IamColumnPermissionManagementView'"), 'Router should keep the column permission page cache name stable.');
assert(routerSource.includes("permission: 'iam-column-permission:view'"), 'Router should protect the column permission page with view permission.');
assert(routerSource.includes("titleKey: 'route.iamColumnPermissions'"), 'Router should use the column permission i18n route key.');

for (const pathFragment of [
  '/api/iam/menus/options',
  '/api/iam/column-permissions/resources',
  '/api/iam/column-permissions/templates',
  '/api/iam/column-permissions/templates/${templateId}/items',
  '/api/iam/column-permissions/tenants/${tenantId}/templates',
  '/api/iam/column-permissions/roles/${roleId}/column-permissions',
]) {
  assert(apiSource.includes(pathFragment), `IAM admin API should include ${pathFragment}.`);
}
assert(apiSource.includes('listIamMenuOptions'), 'IAM admin API should expose listIamMenuOptions for non-menu-management pages.');
assert(pageSource.includes('listIamMenuOptions'), 'Column permission page should use the menu options endpoint instead of the menu management list endpoint.');
assert(!roleColumnPermissionPageSource.includes('listIamMenuOptions'), 'Role column permission page should render the left menu tree from current user visible menus, not the global menu options endpoint.');
assert(roleColumnPermissionPageSource.includes('useAuthorizationStore'), 'Role column permission page should read the current authorization snapshot for visible menus.');
assert(roleColumnPermissionPageSource.includes('loadRoleColumnVisibleMenus'), 'Role column permission page should isolate visible menu loading in a role-column-specific method.');
assert(roleColumnPermissionPageSource.includes('flattenRoleColumnVisibleMenuNodes'), 'Role column permission page should isolate current-menu flattening in a role-column-specific method.');

for (const typeName of [
  'IamResourceColumn',
  'IamColumnPermissionTemplate',
  'IamColumnPermissionTemplateItem',
  'IamRoleColumnPermissionRule',
  'IamColumnPermissionTemplatePayload',
  'IamRoleColumnPermissionRulePayload',
  'IamTenantColumnPermissionTemplateAssignment',
  'IamTenantColumnPermissionTemplateAssignmentPayload',
]) {
  assert(typesSource.includes(typeName), `IAM admin types should include ${typeName}.`);
}

assert(!rolePageSource.includes('openColumnPermissionBinding'), 'Role management page should not keep the old column-template binding dialog.');
assert(!rolePageSource.includes('setIamRoleColumnPermissionTemplate'), 'Role management page should not save role column permissions by binding a template.');
assert(routerSource.includes("path: 'system/iam/role-column-permissions'"), 'Router should include the standalone role column permission route.');
assert(routerSource.includes("name: 'iam-role-column-permission-management'"), 'Router should name the standalone role column permission route.');
assert(routerSource.includes("cacheName: 'IamRoleColumnPermissionManagementView'"), 'Router should keep the role column permission page cache name stable.');
assert(routerSource.includes("permission: 'iam-role-column-permission:view'"), 'Router should protect the role column permission page with view permission.');
assert(routerSource.includes("titleKey: 'route.iamRoleColumnPermissions'"), 'Router should use the role column permission i18n route key.');
for (const snippet of [
  'defineOptions({ name: \'IamRoleColumnPermissionManagementView\' })',
  '角色列权限',
  'NavigationMenuTree',
  'listColumnPermissionTenants',
  'selectedTenantId',
  'tenantOptions',
  'listIamTenantColumnPermissionTemplates',
  'getIamRoleColumnPermissions',
  'setIamRoleColumnPermissions',
  'toggleColumnVisibility',
  'saveRoleColumnRules',
  'refreshCurrentAuthorizationIfNeeded',
  '字段配置器',
  'ref="workbenchRef"',
  'workbench-divider',
  'startWorkbenchResize',
  'toggleEditorPanelCollapse',
  'togglePreviewPanelCollapse',
  'editorPanelStyle',
  'previewPanelStyle',
  'column-panel-toggle',
  'createPermissionSourceTableSchema',
  'createIamUserBrowseTableSchema',
  'listIamUsers',
  "pageConfig.previewSchemaKey === 'iam-user-management'",
  "selectedPageConfig.value?.previewSchemaKey === 'iam-user-management'",
  'formatRoleLabel(role)',
  'formatRoleDisplayName(role)',
  'hasCorruptedText',
]) {
  assert(roleColumnPermissionPageSource.includes(snippet), `Role column permission page should include ${snippet}.`);
}
assert(!roleColumnPermissionPageSource.includes('v-model.number="tenantId"'), 'Role column permission page should not require manual tenantId input.');
assert(!roleColumnPermissionPageSource.includes('placeholder="租户 ID"'), 'Role column permission page should use tenant selection/context instead of exposing tenantId.');
assert(!/import\s+\{\s*listTenants\s*\}\s+from\s+'@\/api\/tenants'/.test(roleColumnPermissionPageSource), 'Role column permission page should not depend on tenant:view-only listTenants.');
assert(
  /const selectedPageColumns = computed[\s\S]*?selectedPageSchemaColumns\.value\.map[\s\S]*?schemaColumnMap\.has/.test(roleColumnPermissionPageSource),
  'Role column permission page should filter configurable columns by the real page schema, not every resource column in the tenant template pool.',
);
assert(
  !/const selectedPageColumns = computed[\s\S]*?const resourceKeySet = new Set\(selectedPageConfig\.value\.resourceKeys\)[\s\S]*?resourceKeySet\.has\(column\.resourceKey\)/.test(roleColumnPermissionPageSource),
  'Role column permission page must not expose schema-external tenant resource columns such as remark.',
);
assert(
  roleColumnPermissionPageSource.includes('const selectedAssignableRules = computed')
    && roleColumnPermissionPageSource.includes('rules: selectedAssignableRules.value.map'),
  'Role column permission page should save only schema-backed assignable rules instead of the whole tenant template pool.',
);
assert(
  /async function saveRoleColumnRules[\s\S]*?await refreshCurrentAuthorizationIfNeeded\(\)/.test(roleColumnPermissionPageSource)
    && roleColumnPermissionPageSource.includes('authorizationStore.refreshCurrentAuthorizationContext()'),
  'Role column permission page should refresh current authorization context after saving rules for the current user role.',
);
assert(
  !roleColumnPermissionPageSource.includes("ruleForm[rule.resourceColumnId] = 'HIDDEN';"),
  'Role column permission page should initialize unsaved role rules from the tenant template maximum instead of hiding every column by default.',
);
assert(
  /function resolveColumnAccess\(columnId: number\): AccessMode \{\s*return ruleForm\[columnId\] \|\| resolveMaxAccess\(columnId\);/s
    .test(roleColumnPermissionPageSource),
  'Role column permission page should fallback to the tenant maximum access when no explicit role rule exists.',
);
assert(
  /function countColumnsByMenuCodes[\s\S]*?resolvePageSchemaColumns\(pageConfig\)[\s\S]*?isTenantColumnAssignable\(column\.id\)/.test(roleColumnPermissionPageSource),
  'Role column permission tree counts should use the same real-schema and tenant-template-upper-bound intersection as the editor panel.',
);
assert(
  roleColumnPermissionPageSource.includes('function isTenantColumnAssignable')
    && /const selectedAssignableColumns = computed[\s\S]*?isTenantColumnAssignable\(column\.id\)/.test(roleColumnPermissionPageSource),
  'Role column permission editor should hide columns whose tenant upper bound is HIDDEN.',
);
assert(
  roleColumnPermissionPageSource.includes('filterPreviewTableSchemaByAssignableColumns')
    && /const previewTableSchema = computed[\s\S]*?filterPreviewTableSchemaByAssignableColumns/.test(roleColumnPermissionPageSource),
  'Role column permission preview schema should remove columns that are not assignable by the tenant.',
);
assert(
  !roleColumnPermissionPageSource.includes(':label="`${role.name}（${role.code}）`"'),
  'Role column permission role selector should not expose corrupted persisted role names directly.',
);
assert(i18nSource.includes('iamColumnPermissions'), 'i18n messages should include route.iamColumnPermissions.');
assert(i18nSource.includes('iamRoleColumnPermissions'), 'i18n messages should include route.iamRoleColumnPermissions.');
assert(pageConfigSource.includes('tenant-management'), 'Column permission page config should bridge tenant-management to a resource key.');
assert(pageConfigSource.includes('iam-user-management'), 'Column permission page config should bridge iam-user-management to the iam-user resource key.');
assert(pageConfigSource.includes('iam-user'), 'Column permission page config should declare the iam-user resource key.');
assert(pageConfigSource.includes('resourceKeys'), 'Column permission page config should declare resource keys per page.');
assert(pageConfigSource.includes('detailColumnKeys'), 'Column permission page config should still support detail column annotations.');
for (const tenantColumnKey of [
  'code',
  'name',
  'status',
  'currentPlanName',
  'currentPlanExpiresAt',
  'primaryDomain',
  'contactName',
  'contactPhone',
  'provisionedAt',
]) {
  assert(
    tenantBrowseTableSchemaSource.includes(`columnKey: '${tenantColumnKey}'`),
    `Tenant browse table schema should expose ${tenantColumnKey} to column permission management.`,
  );
}
assert(pageSource.includes('createIamUserBrowseTableSchema'), 'Column permission page should reuse the IAM user browse table schema for user-grant fields.');
assert(
  pageSource.includes("pageConfig.previewSchemaKey === 'iam-user-management'"),
  'Column permission page should resolve iam-user-management schema columns instead of returning an empty field list.',
);
assert(
  pageSource.includes("selectedPageConfig.value?.previewSchemaKey === 'iam-user-management'"),
  'Column permission page preview should branch to the user-grant preview path.',
);
assert(
  pageSource.includes('resolvePreviewTenantId') && pageSource.includes('previewRows.value = users.slice'),
  'Column permission page should page IAM user preview rows from the current preview tenant context.',
);
assert(
  !/previewCurrentPage\.value\s*=\s*page\.pageNum/.test(pageSource)
    && !/previewPageSize\.value\s*=\s*page\.pageSize/.test(pageSource),
  'Column permission tenant preview loader must not write API paging values back into v-model paging refs, otherwise the table can refresh repeatedly.',
);
assert(
  !pageSource.includes('activeTemplate && previewTableSchema && selectedPageColumns.length'),
  'Column permission page should render a schema-backed preview even before resource-column seeds are available.',
);
assert(
  !pageSource.includes("selectedPageConfig.value?.previewSchemaKey !== 'tenant-management'") || pageSource.includes("previewSchemaKey === 'iam-user-management'"),
  'Column permission page should not treat every non-tenant page as lacking preview schema.',
);

console.log('Verified IAM column permission management page contract.');
