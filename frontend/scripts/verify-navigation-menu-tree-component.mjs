import { existsSync, readFileSync } from 'node:fs';
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

function assertIncludes(source, marker, message) {
  assert(source.includes(marker), message);
}

const componentPath = resolve(frontendRoot, 'src/framework/components/NavigationMenuTree.vue');
assert(existsSync(componentPath), 'NavigationMenuTree component should exist.');

const packageSource = read('package.json');
const componentSource = read('src/framework/components/NavigationMenuTree.vue');
const menuViewSource = read('src/views/IamMenuManagementView.vue');
const permissionViewSource = read('src/views/IamPermissionManagementView.vue');
const componentCenterSource = read('src/views/ComponentCenterView.vue');

assertIncludes(packageSource, 'verify:navigation-menu-tree-component', 'package.json should expose navigation menu tree verification.');

for (const marker of [
  'createNavigationPermissionTree',
  'Teleport',
  'ChevronRight',
  'navigation-menu-tree__expand',
  'navigation-menu-tree__expand-icon',
  'rotated',
  'expandAllNodes',
  'collapseAllNodes',
  '@click.stop="toggleExpanded(node.key)"',
  '@contextmenu="handleRowContextMenu($event, node)"',
  "emit('node-edit'",
  "emit('node-select'",
  "emit('context-menu-command'",
  'contextMenuEnabled',
  'contextMenuActionsResolver',
  'onClick?:',
  'action.onClick?.(payload)',
  'showEditButton',
  'resolveContextMenuActions(node)',
  'navigation-menu-tree__context-menu',
  'contextMenu.actions',
  'contextMenuRef.value?.contains(target)',
  'openContextMenu(event, node)',
  'hideContextMenu()',
  'extraNodes',
  'countResolver',
  'handleRowClick',
  'node.children.length',
  'min-height: 0;',
  'overflow-y: auto;',
  'overscroll-behavior: contain;',
]) {
  assertIncludes(componentSource, marker, `NavigationMenuTree should include ${marker}.`);
}

assertIncludes(
  componentSource,
  "function handleRowContextMenu(event: MouseEvent, node: NavigationMenuTreeNode) {\n  if (!props.contextMenuEnabled) {",
  'NavigationMenuTree right click should open a context menu instead of editing immediately.',
);
assertIncludes(
  componentSource,
  "function resolveContextMenuActions(node: NavigationMenuTreeNode) {\n  return (props.contextMenuActionsResolver?.(node) || [])",
  'NavigationMenuTree should resolve context menu items from the page instead of hardcoding them.',
);

assert(
  !componentSource.includes('handleRowClick(node: NavigationMenuTreeNode) {\n  selectNode(node);\n  if (node.children.length)'),
  'NavigationMenuTree row click should only select; expand/collapse belongs to the arrow control.',
);
assert(
  !componentSource.includes("node.key !== props.allNodeKey && node.children.length"),
  'NavigationMenuTree should allow double-clicking the all-menu root to expand or collapse it.',
);

assertIncludes(menuViewSource, 'NavigationMenuTree', 'Menu management should use shared NavigationMenuTree.');
assertIncludes(menuViewSource, '@node-select="handleMenuNodeSelect"', 'Menu management should react to shared tree selection.');
assertIncludes(menuViewSource, 'height="100%"', 'Menu management table should fill the right panel and scroll internally.');
assertIncludes(menuViewSource, 'grid-template-columns: 300px minmax(0, 1fr);', 'Menu management should use bounded two-column content layout.');
assertIncludes(menuViewSource, 'height: 100%;', 'Menu management layout should be capped by the content area height.');
assertIncludes(menuViewSource, 'min-height: 0;', 'Menu management panels should be allowed to shrink inside the content area.');
assert(!menuViewSource.includes('menuTreeRow'), 'Menu management should not keep its old local tree row implementation.');
assert(!menuViewSource.includes('buildMenuTreeNode'), 'Menu management should not build a duplicate navigation tree.');

assertIncludes(permissionViewSource, 'NavigationMenuTree', 'Permission management should use shared NavigationMenuTree.');
assertIncludes(permissionViewSource, 'UNASSIGNED_MENU_NODE', 'Permission management should pass the unassigned node into the shared tree.');
assertIncludes(permissionViewSource, 'height="100%"', 'Permission management table should fill the right panel and scroll internally.');
assertIncludes(permissionViewSource, 'grid-template-columns: 300px minmax(0, 1fr);', 'Permission management should use bounded two-column content layout.');
assertIncludes(permissionViewSource, 'height: 100%;', 'Permission management layout should be capped by the content area height.');
assertIncludes(permissionViewSource, 'min-height: 0;', 'Permission management panels should be allowed to shrink inside the content area.');
assert(!permissionViewSource.includes('<el-tree'), 'Permission management should no longer use Element Plus tree for menu grouping.');
assert(!permissionViewSource.includes('buildPermissionMenuNode'), 'Permission management should not build a duplicate navigation tree.');

assertIncludes(componentCenterSource, 'NavigationMenuTree', 'Component center should register NavigationMenuTree.');
assertIncludes(componentCenterSource, 'name="navigation-menu-tree"', 'Component center should expose NavigationMenuTree preview panel.');
assertIncludes(componentCenterSource, 'navigationTreeDemoSelectedKey', 'Component center should provide interactive tree demo state.');
assertIncludes(componentCenterSource, 'navigationTreeDemoContextMenuEnabled', 'Component center should expose a right-click menu switch.');
assertIncludes(componentCenterSource, 'navigationTreeDemoContextActions', 'Component center should allow adding context menu actions.');
assertIncludes(componentCenterSource, 'navigationTreeDemoTemplateCode', 'Component center should document NavigationMenuTree template usage.');
assertIncludes(componentCenterSource, 'navigationTreeDemoScriptCode', 'Component center should document NavigationMenuTree context action usage.');
assertIncludes(componentCenterSource, 'component-demo-code-blocks', 'Component center should render implementation code examples.');
assertIncludes(componentCenterSource, 'component-demo-settings-collapse', 'Component center should place component settings in a collapsible panel.');

console.log('Verified shared navigation menu tree component usage.');
