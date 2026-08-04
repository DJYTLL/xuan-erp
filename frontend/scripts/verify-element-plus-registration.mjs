import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const currentDir = dirname(fileURLToPath(import.meta.url));
const root = resolve(currentDir, '..');

function read(relativePath) {
  return readFileSync(resolve(root, relativePath), 'utf8');
}

function assertIncludes(source, marker, message) {
  if (!source.includes(marker)) {
    throw new Error(message);
  }
}

const mainSource = read('src/main.ts');
const layoutSource = read('src/layouts/AppLayout.vue');
const columnPermissionSource = read('src/views/IamColumnPermissionManagementView.vue');

if (layoutSource.includes('<el-radio-group')) {
  assertIncludes(
    mainSource,
    'ElRadio,',
    'main.ts should register ElRadio because Element Plus installs el-radio-group and el-radio-button through the parent radio component.',
  );
}

if (columnPermissionSource.includes('<el-segmented')) {
  assertIncludes(
    mainSource,
    'ElSegmented,',
    'main.ts should register ElSegmented because the column permission rule dialog uses el-segmented.',
  );
}

console.log('Verified Element Plus components are registered for current app usage.');
