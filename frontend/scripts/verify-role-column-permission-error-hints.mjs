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
const httpErrorSource = read('src/api/http-error.ts');
const roleColumnPageSource = read('src/views/IamRoleColumnPermissionManagementView.vue');

assert(
  packageSource.includes('verify:role-column-permission-error-hints'),
  'package.json 应暴露角色列权限缺权提示校验脚本。',
);

[
  ['iam-role:view', '缺少角色查询权限'],
  ['iam-column-permission:view', '缺少列权限资源查看权限'],
  ['iam-role-column-permission:view', '缺少角色列权限查看权限'],
].forEach(([permission, message]) => {
  assert(
    roleColumnPageSource.includes(permission) && roleColumnPageSource.includes(message),
    `角色列权限页应展示 ${permission} 对应的缺权说明：${message}。`,
  );
  assert(
    httpErrorSource.includes(permission) && httpErrorSource.includes(message),
    `HTTP 错误处理应能根据接口依赖解析 ${permission} 对应提示：${message}。`,
  );
});

[
  '/api/iam/roles',
  '/api/iam/column-permissions/resources',
  '/api/iam/column-permissions/tenants/',
  '/api/iam/column-permissions/roles/',
].forEach((path) => {
  assert(httpErrorSource.includes(path), `HTTP 错误处理应识别角色列权限页面依赖接口：${path}。`);
});

assert(
  httpErrorSource.includes('resolveRoleColumnPermissionForbiddenMessage'),
  'HTTP 错误处理应导出角色列权限页面专用 403 说明解析函数。',
);
assert(
  httpErrorSource.includes('isRoleColumnPermissionPageRequest')
    && httpErrorSource.includes("ROLE_COLUMN_PERMISSION_ROUTE = '/system/iam/role-column-permissions'")
    && httpErrorSource.includes('window.location.pathname === ROLE_COLUMN_PERMISSION_ROUTE'),
  '角色列权限依赖接口 403 只应在当前页面保持原页提示，不能影响其他页面的全局 /403 逻辑。',
);
assert(
  roleColumnPageSource.includes('roleColumnPermissionErrorMessage') && roleColumnPageSource.includes('resolveRoleColumnPermissionForbiddenMessage'),
  '角色列权限页应在接口失败时展示明确的缺权提示。',
);
assert(
  roleColumnPageSource.includes('missingDependencyHints'),
  '角色列权限页应根据当前用户快照展示缺失依赖权限。',
);
assert(
  !roleColumnPageSource.includes('authStore.hasPermission(')
    && roleColumnPageSource.includes('authorizationStore.hasButtonPermission('),
  '角色列权限页局部只读状态和依赖提示必须读取 authorizationStore 当前权限快照，不能读取登录态里的旧 permissions。',
);
assert(
  roleColumnPageSource.includes('hasRoleColumnPermissionDependencyWarning')
    && roleColumnPageSource.includes('v-if="hasRoleColumnPermissionDependencyWarning" class="role-column-dependency-strip warning"'),
  '角色列权限页只应在缺失依赖权限或接口失败时展开接口依赖异常提示。',
);
assert(
  roleColumnPageSource.includes('dependency-compact-trigger')
    && roleColumnPageSource.includes('v-if="!hasRoleColumnPermissionDependencyWarning"'),
  '角色列权限页正常态应把接口依赖收敛为工具栏里的紧凑入口。',
);

console.log('Verified role column permission page shows explicit dependency permission errors.');
