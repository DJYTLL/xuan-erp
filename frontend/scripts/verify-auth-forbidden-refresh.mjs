import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');

function read(relativePath) {
  const filePath = resolve(frontendRoot, relativePath);
  assert(existsSync(filePath), `${relativePath} 应存在。`);
  return readFileSync(filePath, 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const packageSource = read('package.json');
const httpSource = read('src/api/http.ts');
const mainSource = read('src/main.ts');
const authStoreSource = read('src/stores/auth.ts');
const sessionEventsSource = read('src/framework/auth/sessionEvents.ts');

assert(
  packageSource.includes('verify:auth-forbidden-refresh'),
  'package.json 应暴露 403 权限刷新校验脚本。',
);

assert(
  httpSource.includes("type RefreshRetryReason = 'unauthorized' | 'forbidden-permission';"),
  'HTTP 拦截器应区分 401 续期和 403 权限快照续期。',
);

assert(
  httpSource.includes("status === 403") && httpSource.includes("getResponseErrorCode(error) === 'SECURITY_PERMISSION_DENIED'"),
  'HTTP 拦截器应只对 SECURITY_PERMISSION_DENIED 类型的 403 尝试刷新。',
);

assert(
  httpSource.includes("return 'forbidden-permission';"),
  '403 权限不足应进入 forbidden-permission 刷新分支。',
);

assert(
  httpSource.includes("refreshReason === 'unauthorized'") && httpSource.includes('clearStoredAuthSession(appFrameworkConfig)'),
  'refresh 失败时只有 401 续期场景才应清理会话，普通 403 不能误踢用户。',
);

assert(
  httpSource.includes('emitAuthSessionRefreshed(refreshed);'),
  'refresh 成功后应广播新登录态，供 Pinia store 同步内存状态。',
);

assert(
  sessionEventsSource.includes('AUTH_SESSION_REFRESHED_EVENT') && sessionEventsSource.includes('emitAuthSessionRefreshed'),
  '应提供统一的登录态刷新事件工具。',
);

assert(
  authStoreSource.includes('applySession(response: LoginResponse)'),
  'auth store 应提供 applySession 动作，用于外部刷新同步。',
);

assert(
  mainSource.includes('listenAuthSessionRefreshed') && mainSource.includes('authorizationStore.loadPermissionSnapshot()'),
  '应用入口应监听 refresh 成功事件，并重新加载当前权限快照。',
);

console.log('Verified forbidden permission refresh retry and store synchronization contract.');
