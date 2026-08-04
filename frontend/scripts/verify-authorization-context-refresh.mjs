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

const packageSource = read('package.json');
const authorizationSource = read('src/stores/authorization.ts');
const tenantManagementSource = read('src/views/TenantManagementView.vue');
const tenantPlanManagementSource = read('src/views/TenantPlanManagementView.vue');
const roleManagementSource = read('src/views/IamRoleManagementView.vue');
const userManagementSource = read('src/views/IamUserManagementView.vue');
const roleColumnPermissionSource = read('src/views/IamRoleColumnPermissionManagementView.vue');
const columnPermissionSource = read('src/views/IamColumnPermissionManagementView.vue');

assert(
  packageSource.includes('verify:authorization-context-refresh'),
  'package.json 应暴露权限上下文刷新契约校验脚本。',
);

assert(
  authorizationSource.includes('getCurrentPermissionSnapshot') && authorizationSource.includes('getCurrentMenus'),
  'authorization store 必须同时依赖 /api/iam/permissions/current 和 /api/iam/menus/current。',
);

assert(
  authorizationSource.includes('refreshCurrentAuthorizationContext'),
  'authorization store 应提供统一刷新当前权限上下文的方法。',
);

const refreshMethodIndex = authorizationSource.indexOf('async refreshCurrentAuthorizationContext');
assert(refreshMethodIndex >= 0, 'authorization store 应声明 async refreshCurrentAuthorizationContext。');
assert(
  authorizationSource.indexOf('getCurrentPermissionSnapshot()', refreshMethodIndex) > refreshMethodIndex,
  '统一刷新方法必须重新拉取 /api/iam/permissions/current。',
);
assert(
  authorizationSource.indexOf('getCurrentMenus()', refreshMethodIndex) > refreshMethodIndex,
  '统一刷新方法必须重新拉取 /api/iam/menus/current。',
);
assert(
  authorizationSource.includes('Promise.all') && authorizationSource.includes('menus: normalizeMenus(currentMenus'),
  '统一刷新方法应并发刷新权限快照和当前菜单，并以 /api/iam/menus/current 结果更新菜单。',
);
assert(
  authorizationSource.includes('return this.refreshCurrentAuthorizationContext();'),
  'loadPermissionSnapshot 应兼容代理到统一刷新方法，避免旧入口漏刷菜单。',
);

[
  ['TenantManagementView.vue', tenantManagementSource, 'refreshCurrentAuthorizationIfTenantAffected'],
  ['TenantPlanManagementView.vue', tenantPlanManagementSource, 'refreshCurrentAuthorizationAfterPlanChange'],
  ['IamRoleManagementView.vue', roleManagementSource, 'refreshCurrentAuthorizationContext'],
  ['IamUserManagementView.vue', userManagementSource, 'refreshCurrentAuthorizationContext'],
  ['IamRoleColumnPermissionManagementView.vue', roleColumnPermissionSource, 'refreshCurrentAuthorizationContext'],
  ['IamColumnPermissionManagementView.vue', columnPermissionSource, 'refreshCurrentAuthorizationAfterTemplateChange'],
].forEach(([fileName, source, text]) => {
  assert(source.includes(text), `${fileName} 保存权限相关变更后应主动刷新当前权限上下文。`);
});

assert(
  tenantManagementSource.includes('await refreshCurrentAuthorizationIfTenantAffected(tenant.id);'),
  '租户编辑中切换当前登录租户套餐后应刷新当前权限上下文。',
);
assert(
  tenantManagementSource.includes('await refreshCurrentAuthorizationIfTenantAffected(planAdjustmentTenant.value.id);'),
  '租户套餐调整当前登录租户后应刷新当前权限上下文。',
);
assert(
  tenantManagementSource.includes('await refreshCurrentAuthorizationIfTenantAffected(row.id);'),
  '租户权限同步修复当前登录租户后应刷新当前权限上下文。',
);
assert(
  tenantManagementSource.includes('Number(tenantId) !== authStore.tenantId'),
  '租户管理页应只在当前登录租户受影响时刷新，避免无关租户操作造成额外请求。',
);

assert(
  tenantPlanManagementSource.includes('useAuthorizationStore'),
  '套餐管理页应接入 authorization store。',
);
assert(
  tenantPlanManagementSource.includes('Number(authStore.tenantId || 0) > 0'),
  '套餐管理页应在业务租户上下文内刷新当前权限上下文。',
);
assert(
  tenantPlanManagementSource.includes('await refreshCurrentAuthorizationAfterPlanChange();'),
  '套餐保存、启停、删除成功后应刷新当前权限上下文。',
);

console.log('Verified current authorization context refreshes permissions and menus after affected changes.');
