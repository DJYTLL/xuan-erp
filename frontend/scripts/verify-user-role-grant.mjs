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
const apiSource = read('src/api/iamAdmin.ts');
const viewSource = read('src/views/IamUserManagementView.vue');
const routerSource = read('src/router/index.ts');
const navigationSource = read('src/config/navigation.ts');
const messagesSource = read('src/i18n/messages.ts');

assert(
  packageSource.includes('verify:user-role-grant'),
  'package.json 应暴露用户分配角色闭环校验脚本。',
);

assert(apiSource.includes('listIamUsers'), 'IAM 管理 API 应提供 listIamUsers。');
assert(apiSource.includes('getIamUserRoles'), 'IAM 管理 API 应提供 getIamUserRoles。');
assert(apiSource.includes('setIamUserRoles'), 'IAM 管理 API 应提供 setIamUserRoles。');
assert(apiSource.includes('/api/iam/users'), 'IAM 管理 API 应请求 /api/iam/users。');
assert(apiSource.includes('/roles'), 'IAM 用户角色 API 应请求用户 roles 子资源。');

assert(viewSource.includes('listIamUsers'), '用户授权页应加载租户用户列表。');
assert(viewSource.includes('getIamUserRoles'), '用户授权页应查询当前用户角色。');
assert(viewSource.includes('setIamUserRoles'), '用户授权页应保存用户角色。');
assert(
  viewSource.includes('isTenantReady') && viewSource.includes('tenantId.value >= 0'),
  '用户授权页应支持平台态 tenantId=0，并在租户 ID 非法时再阻止查询用户和角色。',
);
assert(
  viewSource.includes('请输入有效租户 ID'),
  '用户授权页应在租户 ID 无效时给出明确提示。',
);
assert(
  viewSource.includes('grantReadonly') && viewSource.includes('user.tenantId <= 0'),
  '用户授权页应把平台用户角色分配视为只读查看。',
);
assert(
  viewSource.includes('平台级用户当前仅支持查看角色分配结果'),
  '用户授权页应提示平台级用户角色当前是只读查看。',
);
assert(viewSource.includes('useAuthorizationStore'), '用户授权页应接入当前权限快照 store。');
assert(
  viewSource.includes('authorizationStore.loadPermissionSnapshot'),
  '给当前登录用户保存角色后应重新拉取 /api/iam/permissions/current。',
);
assert(
  viewSource.includes('authStore.currentUser?.userId'),
  '用户授权页应识别保存对象是否为当前登录用户。',
);

assert(
  routerSource.includes('IamUserManagementView') && routerSource.includes('system/iam/users'),
  '前端路由应接入 /system/iam/users 用户授权页。',
);
assert(
  navigationSource.includes('iam-user-management') && navigationSource.includes('/system/iam/users'),
  '导航配置应接入 iam-user-management 用户授权菜单。',
);
assert(
  messagesSource.includes('iamUsers') && messagesSource.includes('用户授权') && messagesSource.includes('User Grants'),
  '中英文国际化应包含用户授权 route/menu 文案。',
);

console.log('Verified user role grant closes user -> role -> permissions snapshot -> frontend refresh loop.');
