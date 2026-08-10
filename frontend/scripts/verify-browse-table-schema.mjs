import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = path.dirname(fileURLToPath(import.meta.url));
const frontendRoot = path.resolve(currentDir, '..');

function read(relativePath) {
  return readFileSync(path.join(frontendRoot, relativePath), 'utf8');
}

const schemaPath = path.join(frontendRoot, 'src/framework/components/browseTableSchema.ts');
const tablePath = path.join(frontendRoot, 'src/framework/components/XuanBrowseTable.vue');
const componentCenterPath = path.join(frontendRoot, 'src/views/ComponentCenterView.vue');

assert(existsSync(schemaPath), 'browseTableSchema.ts should exist for schema-driven browse tables.');

const schemaSource = read('src/framework/components/browseTableSchema.ts');
const tableSource = read('src/framework/components/XuanBrowseTable.vue');
const permissionButtonSource = read('src/framework/components/PermissionButton.vue');
const componentCenterSource = read('src/views/ComponentCenterView.vue');

for (const marker of [
  'XuanBrowseTableSchema',
  'XuanBrowseTableColumnSchema',
  'XuanBrowseTableToolbarActionSchema',
  'XuanBrowseTableRowActionSchema',
  'BrowseTableColumnAccessMode',
  'createBrowseTablePermissionKey',
  'maskBrowseTableValue',
]) {
  assert(schemaSource.includes(marker), `browseTableSchema.ts should include ${marker}.`);
}

for (const marker of [
  'schema?: XuanBrowseTableSchema',
  'columnPermissionSnapshot',
  'normalizedSchema',
  'hasToolbarContent',
  'showPagination',
  'resolveColumnAccessMode',
  'MASKED',
  'HIDDEN',
  'toolbar-action',
  'row-action',
]) {
  assert(tableSource.includes(marker), `XuanBrowseTable.vue should include ${marker}.`);
}

assert(schemaSource.includes('show?: boolean'), 'Browse table pagination schema should allow consumers to hide pagination.');
assert(tableSource.includes('<template v-if="hasToolbarContent" #toolbar>'), 'XuanBrowseTable should not render an empty toolbar slot.');
assert(tableSource.includes('<template v-if="showPagination" #pagination>'), 'XuanBrowseTable should allow schema-driven pagination hiding.');
assert(permissionButtonSource.includes('disabled?: boolean'), 'PermissionButton should accept explicit disabled state from schema-driven components.');
assert(schemaSource.includes('stateResource?: string') && schemaSource.includes('stateAction?: string'), 'Browse table action schema should expose state action permission fields.');
assert(
  permissionButtonSource.includes('props.disabled') && permissionButtonSource.includes('Boolean(props.disabledReason)'),
  'PermissionButton disabled state should combine explicit disabled and disabledReason.',
);
assert(
  tableSource.includes(':disabled="Boolean(action.disabled)"')
    && tableSource.includes(':disabled="resolveRowActionDisabled(action, scope.row)"'),
  'XuanBrowseTable should pass schema disabled state into PermissionButton actions as well as plain el-button actions.',
);
assert(
  tableSource.includes(':state-resource="action.stateResource"')
    && tableSource.includes(':state-code="resolveRowActionStateCode(action, scope.row)"')
    && tableSource.includes(':state-action="action.stateAction"'),
  'XuanBrowseTable should pass schema state action fields into PermissionButton actions.',
);

for (const marker of [
  'browseTableSchema',
  'browseColumnPermissionSnapshot',
]) {
  assert(componentCenterSource.includes(marker), `ComponentCenterView.vue should include ${marker}.`);
}

assert(!componentCenterSource.includes('const browseColumns:'), 'ComponentCenterView should stop using the old browseColumns array in schema mode.');

console.log('browse table schema verification passed');
