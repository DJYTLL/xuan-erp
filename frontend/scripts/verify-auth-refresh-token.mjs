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
const authTypesSource = read('src/types/auth.ts');
const authApiSource = read('src/api/auth.ts');
const httpSource = read('src/api/http.ts');
const tokenStorageSource = read('src/framework/auth/tokenStorage.ts');
const authStoreSource = read('src/stores/auth.ts');
const frameworkTypesSource = read('src/framework/config/types.ts');
const frameworkDefaultsSource = read('src/framework/config/defaults.ts');
const appConfigSource = read('src/app/frameworkConfig.ts');

assert(
  packageSource.includes('verify:auth-refresh-token'),
  'package.json 应暴露 refresh token 接入校验脚本。',
);

for (const marker of ['refreshToken: string', 'refreshTokenExpiresAt: string']) {
  assert(authTypesSource.includes(marker), `LoginResponse 应包含 ${marker}。`);
}

for (const marker of [
  'refreshTokenPath: string',
  'refreshTokenKey: string',
  'refreshTokenExpiresAtKey: string',
]) {
  assert(frameworkTypesSource.includes(marker), `框架配置类型应包含 ${marker}。`);
}

assert(
  frameworkDefaultsSource.includes("refreshTokenPath: '/api/auth/refresh'"),
  '框架默认配置应包含 refreshTokenPath。',
);

assert(
  appConfigSource.includes("refreshTokenPath: '/api/iam/auth/refresh'"),
  'Xuan ERP 配置应指向 IAM refresh 接口。',
);

for (const marker of [
  'getStoredRefreshToken',
  'setStoredRefreshToken',
  'clearStoredRefreshToken',
  'setStoredAuthSession',
  'clearStoredAuthSession',
]) {
  assert(tokenStorageSource.includes(marker), `tokenStorage 应提供 ${marker}。`);
}

assert(
  authApiSource.includes('export async function refreshToken'),
  'auth API 应暴露 refreshToken 方法。',
);

assert(
  authApiSource.includes('appFrameworkConfig.auth.refreshTokenPath'),
  'refreshToken 方法应从框架配置读取 refresh 接口路径。',
);

assert(
  authStoreSource.includes('refreshSession('),
  'auth store 应提供 refreshSession 动作。',
);

assert(
  authStoreSource.includes('response.refreshToken'),
  '登录和刷新后应保存后端返回的新 refreshToken。',
);

const storageFirstRefreshTokenIndex = authStoreSource.indexOf(
  'getStoredRefreshToken(appFrameworkConfig) || this.refreshToken',
);
assert(
  storageFirstRefreshTokenIndex >= 0
    && storageFirstRefreshTokenIndex < authStoreSource.indexOf('const response = await refreshToken(storedRefreshToken)'),
  'refreshSession 应优先使用 storage 中最新的 refreshToken，避免拦截器轮换后继续使用 store 里的旧 ref。',
);

for (const marker of [
  'pendingRefreshRequest',
  'refreshAccessToken',
  'shouldAttemptRefresh',
  'retryOriginalRequest',
  '_retryAfterRefresh',
  'clearStoredAuthSession',
]) {
  assert(httpSource.includes(marker), `HTTP 拦截器应包含 ${marker}。`);
}

assert(
  httpSource.indexOf('await refreshAccessToken()') < httpSource.indexOf('await handleHttpError(error)'),
  '401 时应先尝试 refresh token，再进入统一过期退出处理。',
);

console.log('Verified frontend refresh token storage and 401 retry contract.');
