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
const requiredPermissionsSource = read('src/config/businessPageRequiredPermissions.ts');
const roleManagementSource = read('src/views/IamRoleManagementView.vue');
const initTemplateSource = read('src/views/IamTenantInitTemplateManagementView.vue');
const roleColumnPageSource = read('src/views/IamRoleColumnPermissionManagementView.vue');

assert(
  packageSource.includes('verify:role-column-permission-dependencies'),
  'package.json 应暴露角色列权限页面依赖权限校验脚本。',
);

const constantMatch = requiredPermissionsSource.match(
  /const IAM_ROLE_COLUMN_PERMISSION_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = \[([\s\S]*?)\];/,
);
assert(
  constantMatch,
  'businessPageRequiredPermissions.ts 应声明角色列权限页面运行依赖权限常量。',
);

const dependencyBlock = constantMatch[1];
[
  'iam-role:view',
  'iam-column-permission:view',
  'iam-role-column-permission:view',
  'iam-role-column-permission:update',
].forEach((permission) => {
  assert(
    dependencyBlock.includes(`'${permission}'`),
    `角色列权限页面授权时必须同时带出运行依赖权限：${permission}。`,
  );
});

assert(
  requiredPermissionsSource.includes("'iam-role-column-permission-management': IAM_ROLE_COLUMN_PERMISSION_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS"),
  '角色列权限页面应使用专属依赖权限常量登记到业务页面权限映射。',
);
assert(
  roleManagementSource.includes('businessPageRequiredPermissionMap'),
  '角色授权弹窗应消费统一业务页面依赖权限映射。',
);
assert(
  initTemplateSource.includes('businessPageRequiredPermissionMap'),
  '初始化模板授权应消费统一业务页面依赖权限映射。',
);

[
  ['iam-role:view', '缺少角色查询权限'],
  ['iam-column-permission:view', '缺少列权限资源查看权限'],
  ['iam-role-column-permission:view', '缺少角色列权限查看权限'],
].forEach(([permission, message]) => {
  assert(
    roleColumnPageSource.includes(permission) && roleColumnPageSource.includes(message),
    `角色列权限页应保留 ${permission} 的缺权提示：${message}。`,
  );
});

console.log('Verified role column permission page dependency permissions are registered.');
