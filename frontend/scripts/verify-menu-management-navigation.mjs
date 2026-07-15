import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { createServer } from 'vite';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');

const sampleMenus = [
  menu(1, 'workbench', '工作台', null, 'menu.workbench', '/workbench', 10),
  menu(2, 'inventory-root', '进销存', null, 'nav.inventory', null, 20),
  menu(3, 'stock-management', '库存管理', 2, 'menu.stockManagement', null, 10),
  menu(4, 'inventory', '库存', 3, 'menu.inventory', '/inventory', 10),
  menu(5, 'manufacturing', '组装拆分', 3, 'menu.manufacturing', '/manufacturing', 20),
  menu(6, 'system', '系统设置', null, 'menu.system', '/system', 30),
  menu(7, 'iam-menu-management', '菜单管理', 6, 'menu.iamMenus', '/system/iam/menus', 10),
  menu(8, 'iam-permission-management', '权限管理', 6, 'menu.iamPermissions', '/system/iam/permissions', 20),
  menu(9, 'iam-role-management', '角色管理', 6, 'menu.iamRoles', '/system/iam/roles', 30),
];

function read(relativePath) {
  return readFileSync(resolve(frontendRoot, relativePath), 'utf8');
}

function menu(id, code, title, parentId, i18nKey, path, sortNo) {
  return {
    id,
    code,
    title,
    parentId,
    i18nKey,
    path,
    icon: null,
    permissionCode: null,
    sortNo,
    enabled: true,
  };
}

function assertIncludes(source, marker, message) {
  if (!source.includes(marker)) {
    throw new Error(message);
  }
}

function assertNotIncludes(source, marker, message) {
  if (source.includes(marker)) {
    throw new Error(message);
  }
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function findByKey(items, key) {
  for (const item of items) {
    if (item.key === key) {
      return item;
    }
    const child = findByKey(item.children || [], key);
    if (child) {
      return child;
    }
  }
  return null;
}

const packageSource = read('package.json');
const mainSource = read('src/main.ts');
const menuViewSource = read('src/views/IamMenuManagementView.vue');
const treeComponentSource = read('src/framework/components/NavigationMenuTree.vue');
const navigationSource = read('src/config/navigation.ts');

assertIncludes(packageSource, 'verify:menu-management-navigation', 'package.json should expose menu management navigation verification.');
assertIncludes(menuViewSource, 'NavigationMenuTree', 'Menu management should use the shared navigation menu tree component.');
assertIncludes(menuViewSource, 'menu-management-layout', 'Menu management should render a split layout for tree and table.');
assertIncludes(menuViewSource, 'display: grid;', 'Menu management layout should keep tree and table side by side on desktop.');
assertIncludes(menuViewSource, 'grid-template-columns: 300px minmax(0, 1fr);', 'Menu management tree should keep a fixed sidebar width on desktop.');
assertIncludes(menuViewSource, 'selectedNodeKey', 'Menu management should track the active navigation node.');
assertIncludes(menuViewSource, 'filteredMenuRows', 'Menu table should filter rows by the selected navigation node.');
assertIncludes(menuViewSource, '<el-tree-select', 'Parent menu selection should use a tree selector matching menu hierarchy.');
assertIncludes(mainSource, 'ElTreeSelect', 'Parent menu tree selector should be registered globally.');
assertIncludes(mainSource, 'tree-select/index', 'Parent menu tree selector should import Element Plus tree-select.');
assertIncludes(menuViewSource, 'parentMenuTreeOptions', 'Parent menu selection should keep hierarchical options.');
assertIncludes(menuViewSource, 'isParentMenuSelectable', 'Parent menu selection should restrict selectable nodes.');
assertIncludes(menuViewSource, 'hasChildrenMenu', 'Parent menu selection should allow foldable/group nodes instead of only pathless nodes.');
assertIncludes(menuViewSource, '@node-edit="handleMenuNodeEdit"', 'Menu management should open editing from shared tree edit actions.');
assertIncludes(menuViewSource, 'context-menu-enabled', 'Menu management should explicitly enable the shared right-click menu.');
assertIncludes(menuViewSource, ':context-menu-actions-resolver="resolveMenuContextActions"', 'Menu management should provide page-specific right-click menu items.');
assertIncludes(menuViewSource, '@context-menu-command="handleMenuContextCommand"', 'Menu management should handle context menu commands from the shared tree.');
assertIncludes(menuViewSource, 'onClick: ({ node }) => handleMenuNodeEdit(node)', 'Menu management edit context action should open the edit dialog directly.');
assertIncludes(treeComponentSource, 'contextMenuActionsResolver', 'Shared tree component should support page-provided context menu items.');
assertIncludes(menuViewSource, 'resolveMenuDisplayTitle', 'Menu management table should use the same display title contract as the navigation tree.');
assertIncludes(menuViewSource, '菜单类型', 'Menu dialog should make menu type explicit.');
assertIncludes(menuViewSource, "import XuanDecimalInput from '@/framework/components/XuanDecimalInput.vue';", 'Menu dialog should import the project decimal input.');
assertIncludes(menuViewSource, '<XuanDecimalInput', 'Menu dialog sort field should use the project decimal input.');
assertIncludes(menuViewSource, 'normalizeSortNo', 'Menu dialog should normalize decimal input sort value before submit.');
assertNotIncludes(menuViewSource, '<el-input-number', 'Menu dialog sort field should not use raw Element Plus input number.');
assertIncludes(menuViewSource, "label=\"导航分组\"", 'Menu dialog should offer navigation group creation.');
assertIncludes(menuViewSource, "label=\"页面菜单\"", 'Menu dialog should offer page menu creation.');
assertIncludes(menuViewSource, "label=\"分组入口\"", 'Menu dialog should offer group entry creation.');
assertIncludes(menuViewSource, 'menuTypeDescription', 'Menu dialog should explain the selected menu type.');
assertIncludes(menuViewSource, 'handleMenuTypeChange', 'Menu dialog should adjust fields when menu type changes.');
assertIncludes(menuViewSource, '页面菜单必须填写路由路径', 'Menu dialog should validate page menu route path.');
assertIncludes(menuViewSource, '!hasPagePath(menu) || hasChildrenMenu(menu)', 'Parent menu selection should allow pathless groups even before they have children.');
assertIncludes(menuViewSource, 'resolveMenuTagType', 'Menu management should expose semantic tags for enabled state or route type.');
assertIncludes(treeComponentSource, 'createNavigationPermissionTree', 'Shared tree component should build from the shared backend-driven tree helper.');
assertIncludes(navigationSource, 'createNavigationPermissionTree', 'Navigation config should continue exporting the shared permission tree builder.');

const server = await createServer({
  root: frontendRoot,
  logLevel: 'silent',
  server: { middlewareMode: true },
});

try {
  const { createNavigationPermissionTree } = await server.ssrLoadModule('/src/config/navigation.ts');
  const tree = createNavigationPermissionTree(toCurrentMenuNodes(sampleMenus), (key) => ({
    'menu.workbench': '工作台',
    'nav.inventory': '进销存',
    'menu.stockManagement': '库存管理',
    'menu.system': '系统设置',
    'menu.inventory': '库存',
    'menu.manufacturing': '组装拆分',
    'menu.iamMenus': '菜单管理',
    'menu.iamPermissions': '权限管理',
    'menu.iamRoles': '角色管理',
  }[key] || key));

  const systemRoot = findByKey(tree, 'system');
  const inventoryRoot = findByKey(tree, 'inventory-root');
  const stockManagement = findByKey(tree, 'stock-management');
  const manufacturingLeaf = findByKey(tree, 'manufacturing');
  const menuManagementLeaf = findByKey(tree, 'iam-menu-management');

  assert(inventoryRoot?.title === '进销存', 'Menu management tree should keep the backend inventory root.');
  assert(stockManagement?.title === '库存管理', 'Menu management tree should keep the backend stock-management group.');
  assert(stockManagement?.children.some((item) => item.key === 'manufacturing'), 'Menu management tree should follow backend manufacturing nesting.');
  assert(manufacturingLeaf?.menuCodes.includes('manufacturing'), 'Manufacturing leaf should keep its backend menu code.');
  assert(systemRoot?.title === '系统设置', 'Menu management tree should keep the backend system root.');
  assert(
    ['iam-menu-management', 'iam-permission-management', 'iam-role-management'].every((code) => systemRoot.menuCodes.includes(code)),
    'System root should aggregate real IAM menu codes.',
  );
  assert(menuManagementLeaf?.menuCodes.includes('iam-menu-management'), 'Menu management leaf should keep its backend menu code.');
} finally {
  await server.close();
}

console.log('Verified menu management tree follows backend hierarchy.');

function toCurrentMenuNodes(menus, parentId = null) {
  return menus
    .filter((item) => item.parentId === parentId)
    .sort((left, right) => left.sortNo - right.sortNo)
    .map((item) => ({
      code: item.code,
      title: item.title,
      i18nKey: item.i18nKey,
      path: item.path,
      icon: item.icon,
      permissionCode: item.permissionCode,
      sortNo: item.sortNo,
      children: toCurrentMenuNodes(menus, item.id),
    }));
}
