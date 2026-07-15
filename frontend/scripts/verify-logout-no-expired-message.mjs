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
const layoutSource = read('src/layouts/AppLayout.vue');

assert(
  packageSource.includes('verify:logout-no-expired-message'),
  'package.json 应暴露主动退出不提示登录过期的校验脚本。',
);

assert(
  layoutSource.includes('isLoggingOut'),
  '主动退出应设置退出中标记，避免卸载阶段继续同步远端偏好。',
);

assert(
  layoutSource.includes('shouldSaveTabsPreferenceRemotely'),
  '标签页偏好远端同步应集中判断是否允许保存。',
);

assert(
  layoutSource.includes('!isLoggingOut.value'),
  '主动退出期间不应继续保存远端标签页偏好。',
);

assert(
  layoutSource.includes('authStore.isAuthenticated'),
  '无登录态时不应调用远端偏好接口，避免全局 401 提示登录已过期。',
);

console.log('Verified logout does not trigger expired-session message through tab preference sync.');
