import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

const requiredFiles = [
  'src/framework/index.ts',
  'src/framework/auth/index.ts',
  'src/framework/components/index.ts',
  'src/framework/components-erp/index.ts',
  'src/framework/config/index.ts',
  'src/framework/navigation/index.ts',
  'src/framework/preferences/index.ts',
];

for (const file of requiredFiles) {
  assert(fs.existsSync(path.join(root, file)), `Missing framework public API file: ${file}`);
}

const componentIndex = read('src/framework/components/index.ts');
const erpIndex = read('src/framework/components-erp/index.ts');
const frameworkIndex = read('src/framework/index.ts');
const componentCenter = read('src/views/ComponentCenterView.vue');
const mainSource = read('src/main.ts');

for (const token of [
  'export { default as PermissionButton }',
  'export { default as XuanBrowseTable }',
  'export type { XuanBrowseTableColumn }',
]) {
  assert(componentIndex.includes(token), `framework/components index should export ${token}`);
}

for (const token of [
  'export { default as DocumentEditorShell }',
  'DocumentBasicInfo',
  'DocumentLineItem',
]) {
  assert(erpIndex.includes(token), `framework/components-erp index should export ${token}`);
}

assert(frameworkIndex.includes("export * from './components'"), 'framework index should re-export components public API.');
assert(frameworkIndex.includes("export * from './components-erp'"), 'framework index should re-export ERP public API.');
assert(componentCenter.includes("from '@/framework/components'"), 'ComponentCenterView should import reusable components from framework/components.');
assert(componentCenter.includes("from '@/framework/components-erp'"), 'ComponentCenterView should import ERP reusable components from framework/components-erp.');
assert(mainSource.includes("from './framework'"), 'main.ts should consume framework public API barrel.');

console.log('Verified frontend framework public API barrels.');
