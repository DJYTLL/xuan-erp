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

function countOccurrences(source, marker) {
  return source.split(marker).length - 1;
}

const routerSource = read('src/router/index.ts');
const authorizationSource = read('src/stores/authorization.ts');
const tenantManagementSource = read('src/views/TenantManagementView.vue');
const tenantPlanManagementSource = read('src/views/TenantPlanManagementView.vue');

const guardIndex = routerSource.indexOf('router.beforeEach(async (to)');
const loginBranchIndex = routerSource.indexOf('to.name === appFrameworkConfig.routes.loginRouteName', guardIndex);
const protectedLoadIndex = routerSource.indexOf('!to.meta.public && auth.isAuthenticated && !auth.currentUser', guardIndex);
const unauthenticatedRedirectIndex = routerSource.indexOf('!to.meta.public && !auth.isAuthenticated', guardIndex);

assert(guardIndex >= 0, '路由守卫必须存在。');
assert(loginBranchIndex > guardIndex, '路由守卫必须显式处理登录页。');
assert(
  loginBranchIndex < protectedLoadIndex && loginBranchIndex < unauthenticatedRedirectIndex,
  '登录页必须在受保护页面鉴权分支之前处理，异常本地登录态不能把登录页踢到空白页或循环跳转。',
);

const loginBranchEndIndex = routerSource.indexOf('\n  if (!to.meta.public', loginBranchIndex);
const loginBranchSource = routerSource.slice(loginBranchIndex, loginBranchEndIndex);
assert(
  loginBranchSource.includes('await auth.loadCurrentUser()'),
  '登录页遇到本地 token 时应先校验当前用户，不能只凭 token 判定已登录。',
);
assert(
  loginBranchSource.includes('await authorization.loadPermissionSnapshot()'),
  '登录页本地登录态有效时应同步刷新权限快照和当前菜单后再跳转首页。',
);
assert(
  loginBranchSource.includes('await auth.logout()') && loginBranchSource.includes('return true'),
  '登录页本地登录态异常时应清理本地会话并继续渲染登录页。',
);

assert(
  authorizationSource.includes('async refreshCurrentAuthorizationContext'),
  'authorization store 必须提供统一刷新当前权限上下文的方法。',
);
assert(
  authorizationSource.includes('getCurrentPermissionSnapshot()')
    && authorizationSource.includes('getCurrentMenus()')
    && authorizationSource.includes('Promise.all'),
  '刷新当前权限上下文时必须并发重新拉取 /api/iam/permissions/current 和 /api/iam/menus/current。',
);

assert(
  countOccurrences(tenantManagementSource, 'await refreshCurrentAuthorizationIfTenantAffected(') >= 3,
  '租户管理页套餐切换、套餐调整、权限同步修复成功后都应判断是否刷新当前登录用户权限上下文。',
);
assert(
  tenantManagementSource.includes('Number(tenantId) !== authStore.tenantId'),
  '租户管理页只能在当前登录租户受影响时刷新，避免操作其他租户时误刷新当前会话。',
);
assert(
  countOccurrences(tenantPlanManagementSource, 'await refreshCurrentAuthorizationAfterPlanChange();') >= 3,
  '套餐管理页套餐保存、启停、删除成功后都应触发当前权限上下文刷新。',
);

console.log('Verified login safe render and tenant plan authorization refresh contract.');
