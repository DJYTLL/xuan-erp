import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');
const componentCenterPath = resolve(frontendRoot, 'src/views/ComponentCenterView.vue');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

assert(existsSync(componentCenterPath), 'ComponentCenterView should exist.');

const source = readFileSync(componentCenterPath, 'utf8');

for (const marker of [
  '<el-collapse',
  'componentCenterActivePanels',
  'component-preview-collapse',
  'component-collapse-panel',
  'component-panel-title',
  'componentOverviewPanels',
  'renderOverviewDemo',
  '.component-overview-collapse',
  'margin-bottom: 12px',
  'name="overview-query"',
  'name="overview-tags"',
  'name="overview-permission-buttons"',
  'name="overview-dialogs"',
  'dialog-preview-strip',
  'roleGrantPreviewVisible',
  'name="search-action-bar"',
  'name="login-recent-account-select"',
  '<RecentAccountSearchSelect',
  'loginRecentAccountDemoProfiles',
  'selectedLoginRecentAccountKey',
  'name="browse-table"',
  'browseTableSchema',
  'browseColumnPermissionSnapshot',
  'name="browse-table-reuse-guide"',
  'browse-table-guide',
  'browseTableGuideTemplateCode',
  'browseTableGuideScriptCode',
  'name="permission-assignment"',
  'name="navigation-menu-tree"',
  'navigationTreeDemoContextMenuEnabled',
  'navigationTreeDemoContextActions',
  'navigationTreeDemoTemplateCode',
  'navigationTreeDemoScriptCode',
  'component-demo-code-blocks',
  'component-demo-settings-collapse',
  'name="navigation-tree-context-menu"',
  'name="document-editor"',
]) {
  assert(source.includes(marker), `Component center collapsible layout should include ${marker}.`);
}

assert(!source.includes('class="component-grid"'), 'Component center should not use the old card grid layout.');

assert(
  source.indexOf('name="permission-assignment"') < source.indexOf('<MenuPermissionAssignment'),
  'Permission assignment preview should live inside its collapsible panel.',
);

assert(
  source.indexOf('name="login-recent-account-select"') < source.indexOf('<RecentAccountSearchSelect'),
  'Recent account select preview should live inside its collapsible panel.',
);

console.log('Verified component center collapsible layout.');
