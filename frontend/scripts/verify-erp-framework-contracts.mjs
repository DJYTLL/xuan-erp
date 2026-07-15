import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');
const repoRoot = resolve(frontendRoot, '..');

function read(relativePath) {
  return readFileSync(resolve(repoRoot, relativePath), 'utf8');
}

function assertFile(relativePath) {
  if (!existsSync(resolve(repoRoot, relativePath))) {
    throw new Error(`Missing file: ${relativePath}`);
  }
}

function assertIncludes(source, marker, message) {
  if (!source.includes(marker)) {
    throw new Error(message);
  }
}

assertFile('frontend/src/api/preferences.ts');
assertFile('backend/xuan-iam/src/main/resources/db/migration/V6__add_iam_user_preference.sql');
assertFile('backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/controller/IamUserPreferenceController.java');
assertFile('backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/service/IamUserPreferenceApplicationService.java');
assertFile('backend/xuan-iam/src/main/resources/mapper/iam/IamUserPreferencePersistenceMapper.xml');
assertFile('frontend/scripts/verify-navigation-permission-contract.mjs');

const packageSource = read('frontend/package.json');
const settingsSource = read('frontend/src/stores/settings.ts');
const layoutSource = read('frontend/src/layouts/AppLayout.vue');
const tableSource = read('frontend/src/framework/components/XuanBrowseTable.vue');
const routeContractSource = read('frontend/scripts/verify-navigation-permission-contract.mjs');
const controllerSource = read('backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/controller/IamUserPreferenceController.java');

assertIncludes(packageSource, 'verify:navigation-permission-contract', 'Frontend should expose navigation permission contract verification.');
assertIncludes(settingsSource, 'loadRemotePreferences', 'Settings store should load user layout preferences from backend.');
assertIncludes(settingsSource, 'saveUserPreference', 'Settings store should save user layout preferences to backend.');
assertIncludes(tableSource, 'getUserPreference', 'Browse table should load table preference from backend.');
assertIncludes(tableSource, 'saveUserPreference', 'Browse table should save table preference to backend.');
assertIncludes(layoutSource, 'restoreTabsFromPreference', 'Layout should restore open tabs from user preference.');
assertIncludes(layoutSource, 'saveTabsPreference', 'Layout should save open tab state to user preference.');
assertIncludes(layoutSource, 'restoreContentScroll', 'Layout should restore content scroll per tab.');
assertIncludes(routeContractSource, 'routePermissions', 'Navigation contract should verify route permissions.');
assertIncludes(controllerSource, '/api/iam/user-preferences', 'Backend should expose user preference endpoints.');

console.log('Verified ERP framework contracts for navigation permissions, backend preferences, and refined tab state.');
