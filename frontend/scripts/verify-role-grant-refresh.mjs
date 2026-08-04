import { readFileSync } from 'node:fs';
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

const packageSource = read('package.json');
const roleViewSource = read('src/views/IamRoleManagementView.vue');
const assignmentSource = read('src/framework/components/MenuPermissionAssignment.vue');
const routerSource = read('src/router/index.ts');

assert(
  packageSource.includes('verify:role-grant-refresh'),
  'package.json 应暴露角色授权刷新闭环校验脚本。',
);

assert(
  roleViewSource.includes('MenuPermissionAssignment'),
  '角色授权页应使用菜单树分类权限组件展示授权项。',
);

assert(
  assignmentSource.includes('createNavigationPermissionTree'),
  '菜单权限分配组件应复用导航菜单树分类权限。',
);

assert(
  roleViewSource.includes('useAuthorizationStore'),
  '角色授权页应接入当前权限快照 store。',
);

assert(
  roleViewSource.includes('authorizationStore.refreshCurrentAuthorizationContext'),
  '角色授权保存成功后应重新拉取 /api/iam/permissions/current 和 /api/iam/menus/current。',
);

const saveIndex = roleViewSource.indexOf('await setIamRolePermissions');
const refreshIndex = roleViewSource.indexOf('authorizationStore.refreshCurrentAuthorizationContext', saveIndex);
const successIndex = roleViewSource.indexOf("ElMessage.success('角色授权已保存')", saveIndex);

assert(saveIndex >= 0, '角色授权页应调用 setIamRolePermissions 保存授权。');
assert(refreshIndex > saveIndex, '角色授权页应在保存授权后刷新当前权限快照。');
assert(successIndex > refreshIndex, '角色授权成功提示应在当前权限快照刷新之后显示。');

assert(
  routerSource.includes('hasRoutePermission'),
  '路由守卫应继续依赖当前权限快照判断路由权限。',
);

console.log('Verified role grant refreshes current permission snapshot.');
