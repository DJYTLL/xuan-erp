import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const srcRoot = path.join(root, 'src');

const frameworkComponents = [
  'ApprovalConfirmDialog.vue',
  'ListPageShell.vue',
  'QueryToolbar.vue',
  'SearchActionBar.vue',
  'DataTableShell.vue',
  'XuanBrowseTable.vue',
  'XuanDateTimeRangePicker.vue',
  'XuanDecimalInput.vue',
  'DynamicFormDialog.vue',
  'DetailDrawer.vue',
  'BatchConfirmDialog.vue',
  'AppState.vue',
  'NavigationMenuTree.vue',
  'MenuPermissionAssignment.vue',
  'PermissionButton.vue',
];

const erpComponents = [
  'DocumentEditorShell.vue',
  'DocumentBasicInfoCard.vue',
  'DocumentLineItemsTable.vue',
  'DocumentSettlementCard.vue',
  'DocumentSection.vue',
];

const requiredFrameworkFiles = [
  'framework/components/browseTablePreferences.ts',
  'framework/auth/permissionChecker.ts',
  'framework/preferences/preferenceAdapter.ts',
  'framework/config/types.ts',
  'framework/config/defaults.ts',
  'framework/auth/tokenStorage.ts',
  'framework/navigation/menu.ts',
  'app/frameworkConfig.ts',
];

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function exists(relativePath) {
  return fs.existsSync(path.join(srcRoot, relativePath));
}

function readAllSourceFiles(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  return entries.flatMap((entry) => {
    const absolutePath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return readAllSourceFiles(absolutePath);
    }
    if (!/\.(vue|ts|js|mjs)$/.test(entry.name)) {
      return [];
    }
    return [absolutePath];
  });
}

for (const file of frameworkComponents) {
  assert(exists(`framework/components/${file}`), `Missing framework component: ${file}`);
  assert(!exists(`components/business/${file}`), `Framework component still lives under components/business: ${file}`);
}

for (const file of erpComponents) {
  assert(exists(`framework/components-erp/${file}`), `Missing ERP framework component: ${file}`);
  assert(!exists(`components/business/${file}`), `ERP component still lives under components/business: ${file}`);
}

for (const file of requiredFrameworkFiles) {
  assert(exists(file), `Missing framework boundary file: ${file}`);
}

const movedComponentNames = [...frameworkComponents, ...erpComponents]
  .map((file) => file.replace(/\.vue$/, ''));

const sourceFiles = readAllSourceFiles(srcRoot);
for (const file of sourceFiles) {
  const content = fs.readFileSync(file, 'utf8');
  for (const componentName of movedComponentNames) {
    const oldImport = `@/components/business/${componentName}.vue`;
    assert(
      !content.includes(oldImport),
      `Old business component import remains in ${path.relative(root, file)}: ${oldImport}`,
    );
  }
}

const frameworkSourceFiles = readAllSourceFiles(path.join(srcRoot, 'framework'));
const forbiddenFrameworkImports = [
  '@/api/',
  '@/app/',
  '@/config/',
  '@/stores/',
  '@/types/',
];

for (const file of frameworkSourceFiles) {
  const content = fs.readFileSync(file, 'utf8');
  for (const forbiddenImport of forbiddenFrameworkImports) {
    assert(
      !content.includes(forbiddenImport),
      `Framework file must not import app-specific module ${forbiddenImport}: ${path.relative(root, file)}`,
    );
  }
}

console.log('Verified frontend framework boundaries and component placement.');
