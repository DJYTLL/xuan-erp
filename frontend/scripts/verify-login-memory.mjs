import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');
const loginViewPath = resolve(frontendRoot, 'src/views/LoginView.vue');
const recentAccountSelectPath = resolve(frontendRoot, 'src/components/login/RecentAccountSearchSelect.vue');
const loginProfilesPath = resolve(frontendRoot, 'src/utils/loginProfiles.ts');
const messagesPath = resolve(frontendRoot, 'src/i18n/messages.ts');
const appLayoutPath = resolve(frontendRoot, 'src/layouts/AppLayout.vue');
const authStorePath = resolve(frontendRoot, 'src/stores/auth.ts');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

assert(existsSync(loginViewPath), 'LoginView should exist.');
assert(existsSync(recentAccountSelectPath), 'RecentAccountSearchSelect component should exist.');
assert(existsSync(loginProfilesPath), 'loginProfiles utility should exist.');
assert(existsSync(messagesPath), 'i18n messages file should exist.');
assert(existsSync(appLayoutPath), 'AppLayout should exist.');
assert(existsSync(authStorePath), 'Auth store should exist.');

const loginViewSource = readFileSync(loginViewPath, 'utf8');
const recentAccountSelectSource = readFileSync(recentAccountSelectPath, 'utf8');
const loginProfilesSource = readFileSync(loginProfilesPath, 'utf8');
const messagesSource = readFileSync(messagesPath, 'utf8');
const appLayoutSource = readFileSync(appLayoutPath, 'utf8');
const authStoreSource = readFileSync(authStorePath, 'utf8');

for (const marker of [
  '<RecentAccountSearchSelect',
  'RecentAccountSearchSelect',
  'useAuthorizationStore',
  'authorizationStore.loadPermissionSnapshot',
  'selectedHistoryKey',
  'rememberPassword',
  'applyLoginProfileSelection',
  'saveLoginProfile',
  'loadLoginProfiles',
  'clearAllLoginProfiles',
  'remember-password-hint',
  'caps-lock-warning',
  'focusPreferredField',
  'login.submitting',
]) {
  assert(loginViewSource.includes(marker), `Login view should include ${marker}.`);
}

assert(
  !loginViewSource.includes('onMounted(() =>'),
  'Login view should not auto-focus a field on page mount because it causes the page to visually jump.',
);

for (const marker of [
  'defineProps',
  'defineEmits',
  'LoginProfile',
  'recent-account-search',
  'recent-account-search.is-open .recent-account-search-box',
  '.recent-account-search-box input:focus',
  'box-shadow: none !important',
  'recent-account-panel',
  'recent-account-suggestions',
  "'select'",
  "'remove'",
]) {
  assert(recentAccountSelectSource.includes(marker), `Recent account search select should include ${marker}.`);
}

for (const marker of [
  'const searchActive = ref(false)',
  'if (!searchActive.value || !normalizedSearch.value)',
  'searchActive.value = true',
  'searchActive.value = false',
]) {
  assert(
    recentAccountSelectSource.includes(marker),
    `Recent account search select should keep selected account display separate from active filtering: ${marker}.`,
  );
}

for (const marker of [
  'recent-account-related',
  'ScanSearch',
  'relatedItems',
  'assistLabel',
  '0 0 0 4px',
  'border: 2px solid var(--xuan-primary)',
  'border-color: var(--xuan-primary)',
  'border-radius 180ms',
  'transform: translateY',
  'recent-account-panel-enter-active',
  'recent-account-panel-leave-active',
]) {
  assert(!recentAccountSelectSource.includes(marker), `Recent account search select should not include ${marker}.`);
}

assert(
  !loginViewSource.includes('<el-select'),
  'Login view should use RecentAccountSearchSelect instead of inline Element Plus select.',
);

const submitLoginStart = loginViewSource.indexOf('async function submitLogin()');
const loginCallIndex = loginViewSource.indexOf('await authStore.login', submitLoginStart);
const saveProfileIndex = loginViewSource.indexOf('saveLoginProfile({', submitLoginStart);
const permissionSnapshotIndex = loginViewSource.indexOf('await authorizationStore.loadPermissionSnapshot()', submitLoginStart);

assert(submitLoginStart >= 0, 'Login view should define submitLogin.');
assert(loginCallIndex > submitLoginStart, 'Login view should authenticate before saving a recent account.');
assert(saveProfileIndex > loginCallIndex, 'Login view should save a recent account after authentication succeeds.');
assert(
  permissionSnapshotIndex > saveProfileIndex,
  'Login view should save the recent account before loading permission snapshot so snapshot failures do not drop local history.',
);

for (const marker of [
  'export interface LoginProfile',
  'const LOGIN_PROFILES_STORAGE_KEY',
  'const MAX_LOGIN_PROFILES',
  'loadLoginProfiles()',
  'saveLoginProfile(',
  'findLoginProfile(',
  'deleteLoginProfile(',
  'clearLoginProfiles(',
]) {
  assert(loginProfilesSource.includes(marker), `loginProfiles utility should include ${marker}.`);
}

for (const marker of [
  "recentAccount: '最近账号'",
  "recentAccountPlaceholder: '选择最近登录账号'",
  "rememberPassword: '记住密码'",
  "rememberPasswordHint: '仅限本人设备使用，密码将保存在当前浏览器'",
  "removeCurrentAccount: '删除当前记录'",
  "clearHistory: '清空历史'",
  "capsLockOn: '检测到大写锁定已开启'",
  "submitting: '登录中...'",
  "logoutClearDevice: '退出并清除本机记录'",
  "IAM_INVALID_CREDENTIALS: '账号或密码错误'",
  "IAM_USER_LOCKED: '账号已被锁定，请联系管理员'",
]) {
  assert(messagesSource.includes(marker), `Chinese login i18n should include ${marker}.`);
}

for (const marker of [
  'logout-clear-device',
  'clearLoginProfiles',
  'ElMessageBox.confirm',
]) {
  assert(appLayoutSource.includes(marker), `App layout should include ${marker}.`);
}

assert(
  !authStoreSource.includes('loadPermissionSnapshot'),
  'Auth store login should not load permission snapshot before the login page can save local recent-account history.',
);

console.log('Verified login memory UI and storage contract.');
