import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { createServer } from 'vite';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');

const sampleMenus = [
  menu(1, 'inventory-root', '进销存', null, 'nav.inventory', null, 20),
  menu(2, 'stock-management', '库存管理', 1, 'menu.stockManagement', null, 10),
  menu(3, 'inventory', '库存', 2, 'menu.inventory', '/inventory', 10),
  menu(4, 'manufacturing', '组装拆分', 2, 'menu.manufacturing', '/manufacturing', 20),
  menu(5, 'system', '系统设置', null, 'menu.system', '/system', 30),
  menu(6, 'iam-menu-management', '菜单管理', 5, 'menu.iamMenus', '/system/iam/menus', 10),
  menu(7, 'iam-permission-management', '权限管理', 5, 'menu.iamPermissions', '/system/iam/permissions', 20),
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
const permissionViewSource = read('src/views/IamPermissionManagementView.vue');
const treeComponentSource = read('src/framework/components/NavigationMenuTree.vue');
const navigationSource = read('src/config/navigation.ts');

assertIncludes(packageSource, 'verify:permission-menu-tree', 'package.json should expose permission menu tree verification.');
assertIncludes(permissionViewSource, 'listIamMenuOptions', 'Permission management should load IAM menu options for menu tree grouping.');
assertIncludes(permissionViewSource, 'NavigationMenuTree', 'Permission management should use the shared navigation menu tree component.');
assertIncludes(permissionViewSource, 'selectedMenuCode', 'Permission management should filter permissions by selected menu node.');
assertIncludes(permissionViewSource, 'UNASSIGNED_MENU_NODE', 'Permission management should group permissions without menuCode.');
assertIncludes(permissionViewSource, 'menuSelectOptions', 'Permission form should use menu options instead of free text only.');
assertIncludes(permissionViewSource, 'filteredPermissionRows', 'Permission table should render filtered rows from selected menu tree.');
assertIncludes(permissionViewSource, 'businessPageRequiredPermissionMap', 'Permission management should load page dependency permission mapping.');
assertIncludes(permissionViewSource, 'collectBusinessPageRequiredPermissionCodes', 'Permission management should aggregate page dependency permissions.');
assertIncludes(permissionViewSource, '页面依赖', 'Permission management should mark related permissions as page dependencies.');
assertIncludes(navigationSource, 'createNavigationPermissionTree', 'Navigation config should expose a shared permission tree builder.');
assertIncludes(treeComponentSource, 'createNavigationPermissionTree', 'Shared tree component should reuse the shared backend-driven tree helper.');
assertIncludes(treeComponentSource, 'menuCodes', 'Permission menu nodes should carry real backend menu codes.');

const server = await createServer({
  root: frontendRoot,
  logLevel: 'silent',
  server: { middlewareMode: true },
});

try {
  const { createNavigationPermissionTree } = await server.ssrLoadModule('/src/config/navigation.ts');
  const { collectBusinessPageRequiredPermissionCodes } = await server.ssrLoadModule('/src/config/businessPageRequiredPermissions.ts');
  const tree = createNavigationPermissionTree(toCurrentMenuNodes(sampleMenus), (key) => ({
    'nav.inventory': '进销存',
    'menu.stockManagement': '库存管理',
    'menu.system': '系统设置',
    'menu.inventory': '库存',
    'menu.manufacturing': '组装拆分',
    'menu.iamMenus': '菜单管理',
    'menu.iamPermissions': '权限管理',
  }[key] || key));

  const inventoryRoot = findByKey(tree, 'inventory-root');
  const stockManagement = findByKey(tree, 'stock-management');
  const manufacturing = findByKey(tree, 'manufacturing');
  const permissionMenu = findByKey(tree, 'iam-permission-management');

  assert(inventoryRoot?.title === '进销存', 'Permission tree should keep the sidebar inventory root.');
  assert(stockManagement?.title === '库存管理', 'Permission tree should keep the backend stock-management group.');
  assert(
    ['inventory', 'manufacturing'].every((code) => stockManagement.menuCodes.includes(code)),
    'Stock-management group should aggregate real backend child menu codes.',
  );
  assert(manufacturing?.menuCodes.includes('manufacturing'), 'Manufacturing leaf should expose its backend menu code.');
  assert(permissionMenu?.menuCodes.includes('iam-permission-management'), 'Permission management leaf should expose its real menu code.');

  const salesDependencyCodes = collectBusinessPageRequiredPermissionCodes(['sales-management', 'sales']);
  assert(
    ['product:view', 'inventory:view', 'warehouse:view', 'party:view'].every((code) => salesDependencyCodes.includes(code)),
    'Sales management should aggregate product, inventory, warehouse and party permissions as page dependencies.',
  );

  const restoredNavigationDependencies = new Map([
    ['party', ['party:view', 'party:create', 'party:update', 'party:delete', 'party:import', 'party:export']],
    ['warehouse', ['warehouse:view', 'warehouse:create', 'warehouse:update', 'warehouse:delete', 'warehouse:import', 'warehouse:export']],
    ['inventory', ['inventory:view', 'inventory:create', 'inventory:update', 'inventory:delete', 'inventory:audit', 'inventory:import', 'inventory:export']],
    ['sales', ['sales:view', 'sales:create', 'sales:update', 'sales:delete', 'sales:audit', 'sales:import', 'sales:export']],
    ['finance', ['finance:view', 'finance:create', 'finance:update', 'finance:delete', 'finance:audit', 'finance:import', 'finance:export']],
    ['document', ['document:view', 'document:create', 'document:update', 'document:delete', 'document:export']],
    ['manufacturing', ['manufacturing:view', 'manufacturing:create', 'manufacturing:update', 'manufacturing:delete', 'manufacturing:audit', 'manufacturing:import', 'manufacturing:export']],
    ['report', ['query:view', 'query:create', 'query:update', 'query:delete', 'query:export']],
  ]);

  for (const [menuCode, expectedCodes] of restoredNavigationDependencies) {
    const dependencyCodes = collectBusinessPageRequiredPermissionCodes([menuCode]);
    assert(
      expectedCodes.every((code) => dependencyCodes.includes(code)),
      `${menuCode} should expose restored navigation page operation permissions.`,
    );
  }

  const systemPageDependencies = new Map([
    ['iam-menu-management', ['iam-menu:view', 'iam-menu:create', 'iam-menu:update']],
    ['iam-permission-management', ['iam-permission:view', 'iam-permission:create', 'iam-permission:update']],
    ['iam-role-management', ['iam-role:view', 'iam-role:create', 'iam-role:update']],
    ['iam-user-management', ['iam-user:view', 'iam-user:create', 'iam-user:update', 'iam-user:reset-password', 'iam-user:delete']],
    ['iam-init-template-management', ['iam-init-template:view', 'iam-init-template:create', 'iam-init-template:update']],
    ['tenant-management', [
      'tenant:view',
      'tenant:create',
      'tenant:update',
      'tenant:enable',
      'tenant:disable',
      'tenant:delete',
      'tenant-plan:view',
      'tenant-plan:assign',
      'tenant-provision:view',
      'tenant-provision:manage',
    ]],
    ['tenant-plan-management', ['tenant-plan:view', 'tenant-plan:manage', 'tenant-plan:assign']],
    ['audit-logs', ['audit:log:view']],
    ['audit-interface-costs', ['audit:interface-cost:view']],
    ['audit-sql-rankings', ['audit:sql-ranking:view']],
    ['components', ['component-center:view']],
  ]);

  for (const [menuCode, expectedCodes] of systemPageDependencies) {
    const dependencyCodes = collectBusinessPageRequiredPermissionCodes([menuCode]);
    assert(
      expectedCodes.every((code) => dependencyCodes.includes(code)),
      `${menuCode} should expose ${expectedCodes.join(', ')} as page dependency permissions.`,
    );
  }
} finally {
  await server.close();
}

console.log('Verified permission management follows backend menu tree.');

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
