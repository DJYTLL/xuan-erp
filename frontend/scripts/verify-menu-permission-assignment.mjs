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
  'activePermissionItems',
  'toggleNodePermissions',
  'selectAllPermissions',
  'clearAllPermissions',
  'height: min(640px, calc(100vh - 220px));',
  'min-height: 0;',
  'overflow-y: auto;',
  'overscroll-behavior: contain;',
]) {
  assert(componentSource.includes(marker), `MenuPermissionAssignment should include ${marker}.`);
}

for (const marker of [
  '<el-dialog',
  'role-grant-dialog',
  'MenuPermissionAssignment',
  'businessPageRequiredPermissionMap',
  'listIamMenus',
  'menus.value',
  ':menus="menus"',
  ':permissions="permissions"',
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

console.log('Verified menu permission assignment component contract.');
