import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const workspaceRoot = path.resolve(root, '..');

function read(relativePath) {
  return fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const packageSource = read('frontend/package.json');
const runtimeScript = read('backend/scripts/verify-gateway-authz-runtime.ps1');

assert(
  packageSource.includes('verify:gateway-authz-runtime-script'),
  'package.json 应暴露 Gateway 授权真实链路脚本契约校验。',
);

[
  '/api/iam/auth/login',
  '/api/iam/auth/refresh',
  '/api/iam/auth/current-user',
  '/api/iam/menus/current',
  '/api/iam/permissions/current',
  '/api/tenants/column-permission-options',
].forEach((pathName) => {
  assert(runtimeScript.includes(pathName), `真实链路验证脚本必须覆盖 ${pathName}。`);
});

[
  'XUAN_GATEWAY_VERIFY_TENANT_CODE',
  'XUAN_GATEWAY_VERIFY_USERNAME',
  'XUAN_GATEWAY_VERIFY_PASSWORD',
  'XUAN_GATEWAY_VERIFY_ACCESS_TOKEN',
].forEach((envName) => {
  assert(runtimeScript.includes(envName), `真实链路验证脚本必须支持 ${envName}。`);
});

assert(
  runtimeScript.includes('Assert-JsonProperty'),
  '真实链路验证脚本必须断言响应体结构，不能只看状态码。',
);

assert(
  runtimeScript.includes('columnPermissions') && runtimeScript.includes('records'),
  '真实链路验证脚本必须验证权限快照列权限结构和 Tenant 分页 records。',
);

console.log('Verified Gateway authorization runtime verification script contract.');
