import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');

function read(relativePath) {
  return readFileSync(resolve(frontendRoot, relativePath), 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const componentPath = 'src/framework/components/MenuPermissionAssignment.vue';
const roleViewPath = 'src/views/IamRoleManagementView.vue';
const componentCenterPath = 'src/views/ComponentCenterView.vue';

assert(existsSync(resolve(frontendRoot, componentPath)), 'Menu permission assignment component should exist.');

const componentSource = read(componentPath);
const roleViewSource = read(roleViewPath);
const componentCenterSource = read(componentCenterPath);

for (const marker of [
  'defineProps',
  'menus',
  'permissions',
  'createNavigationPermissionTree',
  'toCurrentMenuNodes',
  'NavigationPermissionNode',
  'pageRequiredPermissionMap',
  'modelValue',
  'update:modelValue',
  'permission-assignment-search',
  'permission-node-count',
  'permission-related-tag',
  '页面依赖',
  'ChevronRight',
  'permission-expand-icon',
  'activePermissionItems',
  'toggleNodePermissions',
  'selectAllPermissions',
  'clearAllPermissions',
  'height: 100%;',
  'min-height: 0;',
  'overflow-y: auto;',
  'overscroll-behavior: contain;',
]) {
  assert(componentSource.includes(marker), `MenuPermissionAssignment should include ${marker}.`);
}

assert(
  !componentSource.includes("{{ isExpanded(node.key) ? '⌄' : '›' }}"),
  'MenuPermissionAssignment should use a stable icon component instead of font-dependent text arrows.',
);

for (const marker of [
  'visiblePermissionCodes',
  'pruneInvisiblePermissionNodes',
  'node.visiblePermissionCodes.length || node.children.length',
]) {
  assert(componentSource.includes(marker), `MenuPermissionAssignment should separate menu visibility with ${marker}.`);
}

for (const marker of [
  'role-grant-dialog',
  'MenuPermissionAssignment',
  'businessPageRequiredPermissionMap',
  'DynamicFormDialog',
  ':render-form="false"',
  'menus.value',
  ':menus="menus"',
  ':permissions="grantAvailablePermissions"',
  'grantAvailablePermissions',
  ':page-required-permission-map="roleGrantRequiredPermissionMap"',
  'roleGrantRequiredPermissionMap',
  'v-model="selectedPermissionCodes"',
]) {
  assert(roleViewSource.includes(marker), `Role management should wire ${marker}.`);
}

assert(
  !roleViewSource.includes('<el-drawer v-model="grantVisible"'),
  'Role grant should use a dialog instead of a drawer.',
);

for (const marker of [
  'MenuPermissionAssignment',
  'permissionDemoMenus',
  'permissionDemoPermissions',
  'permissionDemoSelectedCodes',
  ':menus="permissionDemoMenus"',
  ':permissions="permissionDemoPermissions"',
  ':page-required-permission-map="permissionDemoRequiredPermissionMap"',
  'permissionDemoRequiredPermissionMap',
  'v-model="permissionDemoSelectedCodes"',
]) {
  assert(componentCenterSource.includes(marker), `Component center should preview ${marker}.`);
}

assert(
  roleViewSource.includes('DynamicFormDialog'),
  '角色授权页应使用组件中心 DynamicFormDialog 作为弹窗壳。',
);

assert(
  roleViewSource.includes(':render-form="false"'),
  '角色授权页应把权限分配内容放进组件中心 DynamicFormDialog 的无表单模式。',
);

console.log('Verified menu permission assignment component contract.');
