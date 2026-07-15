import { readFileSync } from 'node:fs';

function read(path) {
  return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

function assertIncludes(content, marker, message) {
  if (!content.includes(marker)) {
    throw new Error(message);
  }
}

const packageJson = read('package.json');
const configTypes = read('src/framework/config/types.ts');
const configDefaults = read('src/framework/config/defaults.ts');
const appConfig = read('src/app/frameworkConfig.ts');
const authApi = read('src/api/auth.ts');
const authStore = read('src/stores/auth.ts');
const httpClient = read('src/api/http.ts');
const appLayout = read('src/layouts/AppLayout.vue');

assertIncludes(
  packageJson,
  '"verify:auth-logout-revoke": "node scripts/verify-auth-logout-revoke.mjs"',
  'package.json 应暴露 auth logout revoke 校验脚本。',
);
assertIncludes(configTypes, 'logoutPath: string;', 'FrameworkAuthConfig 应声明 logoutPath。');
assertIncludes(configDefaults, "logoutPath: '/api/auth/logout'", '默认框架配置应提供 logoutPath。');
assertIncludes(appConfig, "logoutPath: '/api/iam/auth/logout'", 'Xuan ERP 配置应指向 IAM logout 接口。');
assertIncludes(authApi, 'export async function logout(refreshToken: string): Promise<void>', 'auth API 应导出 logout(refreshToken)。');
assertIncludes(authApi, 'appFrameworkConfig.auth.logoutPath', 'auth logout API 应使用配置化 logoutPath。');
assertIncludes(authStore, "import { getCurrentUser, login, logout, refreshToken } from '@/api/auth';", 'auth store 应导入 logout API。');
assertIncludes(authStore, 'async logout()', 'auth store logout 应为异步动作。');
assertIncludes(authStore, 'const tokenToRevoke = getStoredRefreshToken(appFrameworkConfig) || this.refreshToken;', '退出前应读取待撤销 refresh token。');
assertIncludes(authStore, 'await logout(tokenToRevoke);', '退出时应调用后端撤销 refresh token。');
assertIncludes(authStore, 'finally {', '后端撤销失败也必须进入本地清理兜底。');
assertIncludes(httpClient, 'requestPath.endsWith(appFrameworkConfig.auth.logoutPath)', 'HTTP 401 刷新逻辑应排除 logout 请求。');
assertIncludes(appLayout, 'await authStore.logout();', '布局退出操作应等待异步 logout 完成。');

console.log('Auth logout revoke contract verified.');
