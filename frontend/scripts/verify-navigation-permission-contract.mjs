import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { createServer } from 'vite';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');
const routerSource = readFileSync(resolve(currentDir, '../src/router/index.ts'), 'utf8');
const navigationSource = readFileSync(resolve(currentDir, '../src/framework/navigation/menu.ts'), 'utf8');

const routePermissions = {
  '/components': ['component-center:view'],
  '/system/iam/menus': ['iam-menu:view'],
  '/system/iam/permissions': ['iam-permission:view'],
  '/system/iam/roles': ['iam-role:view'],
  '/system/iam/column-permissions': ['iam-column-permission:view'],
  '/system/iam/users': ['iam-user:view'],
  '/system/iam/init-templates': ['iam-init-template:view'],
  '/system/tenants': ['tenant:view'],
  '/system/tenant-plans': ['tenant-plan:view'],
  '/system/audit/logs': ['audit:log:view'],
  '/system/audit/interface-costs': ['audit:interface-cost:view'],
  '/system/audit/sql-rankings': ['audit:sql-ranking:view'],
  '/inventory/products': ['product:view'],
  '/party': ['party:view'],
  '/warehouse': ['warehouse:view'],
  '/inventory': ['inventory:view'],
  '/sales': ['sales:view'],
  '/finance': ['finance:view'],
  '/document': ['document:view'],
  '/manufacturing': ['manufacturing:view'],
  '/report': ['query:view'],
  '/purchase/orders/draft': ['procurement:view'],
  '/purchase/orders/create': ['procurement:create'],
  '/purchase/orders/approved': ['procurement:view'],
  '/purchase/returns/draft': ['procurement:view'],
  '/purchase/returns/approved': ['procurement:view'],
};

const menuRedirectContracts = {
  '/product': '/inventory/products',
  '/procurement': '/purchase/orders/draft',
  '/system': '/components',
};

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function routeBlock(path) {
  const childPath = path.slice(1);
  const routeStart = routerSource.indexOf(`path: '${childPath}'`);
  if (routeStart < 0) {
    return '';
  }
  const nextRouteStart = routerSource.indexOf('\n      {', routeStart + 1);
  return routerSource.slice(routeStart, nextRouteStart > routeStart ? nextRouteStart : routeStart + 900);
}

for (const [path, permissions] of Object.entries(routePermissions)) {
  const block = routeBlock(path);
  assert(block, `Missing route for ${path}`);
  for (const permission of permissions) {
    assert(block.includes(`'${permission}'`), `Route ${path} should require permission ${permission}`);
  }
  assert(block.includes('permission:'), `Route ${path} should declare route permissions.`);
}

for (const [menuPath, targetPath] of Object.entries(menuRedirectContracts)) {
  const menuChildPath = menuPath.slice(1);
  const block = routeBlock(menuPath);
  assert(block, `Missing menu entry route for ${menuPath}`);
  assert(block.includes(`redirect: '${targetPath}'`), `Menu route ${menuPath} should redirect to ${targetPath}`);
  assert(routerSource.includes(`path: '${menuChildPath}'`), `Router should include menu route ${menuPath}`);
}

assert(routerSource.includes('hasRoutePermission'), 'Router guard should enforce routePermissions.');
assert(navigationSource.includes('node.permissionCode || undefined'), 'Navigation should preserve backend menu permissionCode.');
assert(navigationSource.includes('node.path || undefined'), 'Navigation should preserve backend menu path.');
assert(navigationSource.includes('filterNavigationMenuByPermission'), 'Navigation should expose menu permission filtering helper.');

const server = await createServer({
  root: frontendRoot,
  logLevel: 'silent',
  server: { middlewareMode: true },
});

try {
  const { createNavigationMenu, filterNavigationMenuByPermission } = await server.ssrLoadModule('/src/config/navigation.ts');
  const menu = createNavigationMenu([
    {
      code: 'inventory-root',
      title: '进销存',
      i18nKey: 'nav.inventory',
      path: null,
      icon: null,
      permissionCode: null,
      sortNo: 10,
      children: [
        {
          code: 'inventory',
          title: '库存',
          i18nKey: 'menu.inventory',
          path: '/inventory',
          icon: null,
          permissionCode: 'inventory:read',
          sortNo: 10,
          children: [],
        },
      ],
    },
  ], (key) => ({
    'nav.inventory': '进销存',
    'menu.inventory': '库存',
  }[key] || key));

  const inventoryRoot = menu.find((item) => item.key === 'inventory-root');
  const inventoryLeaf = inventoryRoot?.children?.find((item) => item.key === 'inventory');
  assert(inventoryLeaf?.path === '/inventory', 'Navigation should keep backend menu path in rendered tree.');
  assert(inventoryLeaf?.permission === 'inventory:read', 'Navigation should keep backend permissionCode in rendered tree.');

  const emptyGroupMenu = createNavigationMenu([
    {
      code: 'system',
      title: '系统设置',
      i18nKey: null,
      path: null,
      icon: null,
      permissionCode: null,
      sortNo: 10,
      children: [],
    },
    {
      code: 'workbench',
      title: '工作台',
      i18nKey: null,
      path: '/workbench',
      icon: null,
      permissionCode: null,
      sortNo: 20,
      children: [],
    },
  ], (key) => key);

  assert(!emptyGroupMenu.some((item) => item.key === 'system'), 'Navigation should hide empty permissionless menu groups.');
  assert(emptyGroupMenu.some((item) => item.key === 'workbench'), 'Navigation should keep path-backed public menu entries.');

  const systemMenu = createNavigationMenu([
    {
      code: 'system',
      title: '系统设置',
      i18nKey: null,
      path: null,
      icon: null,
      permissionCode: null,
      sortNo: 10,
      children: [
        {
          code: 'tenant-management',
          title: '租户列表',
          i18nKey: null,
          path: '/system/tenants',
          icon: null,
          permissionCode: 'tenant:view',
          sortNo: 10,
          children: [],
        },
        {
          code: 'iam-menu-management',
          title: '菜单管理',
          i18nKey: null,
          path: '/system/iam/menus',
          icon: null,
          permissionCode: 'iam-menu:view',
          sortNo: 20,
          children: [],
        },
      ],
    },
  ], (key) => key);

  const filteredSystemMenu = filterNavigationMenuByPermission(systemMenu, (item) => item.path !== '/system/tenants');
  const filteredSystemRoot = filteredSystemMenu.find((item) => item.key === 'system');
  assert(filteredSystemRoot, 'Navigation permission filter should keep parent groups with authorized children.');
  assert(
    !filteredSystemRoot.children.some((item) => item.key === 'tenant-management'),
    'Navigation permission filter should hide page menus when route view permission is missing.',
  );
  assert(
    filteredSystemRoot.children.some((item) => item.key === 'iam-menu-management'),
    'Navigation permission filter should keep authorized sibling page menus.',
  );

  const noVisibleChildrenMenu = filterNavigationMenuByPermission(systemMenu, () => false);
  assert(
    !noVisibleChildrenMenu.some((item) => item.key === 'system'),
    'Navigation permission filter should hide groups after all children are filtered.',
  );

  const redirectMenu = createNavigationMenu([
    {
      code: 'system',
      title: '系统设置',
      i18nKey: null,
      path: '/system',
      icon: null,
      permissionCode: null,
      sortNo: 10,
      children: [],
    },
    {
      code: 'procurement',
      title: '采购',
      i18nKey: null,
      path: '/procurement',
      icon: null,
      permissionCode: null,
      sortNo: 20,
      children: [],
    },
  ], (key) => key);
  assert(
    !filterNavigationMenuByPermission(redirectMenu, () => false).length,
    'Navigation permission filter should hide redirect-only menus when redirected page permission is missing.',
  );
} finally {
  await server.close();
}

const appLayoutSource = readFileSync(resolve(currentDir, '../src/layouts/AppLayout.vue'), 'utf8');
assert(appLayoutSource.includes('resolveRoutePermissionMeta'), 'App layout should resolve redirect target route permissions for sidebar menus.');
assert(appLayoutSource.includes("resolved.name !== 'catchAll'"), 'App layout should reject unknown menu routes resolved by the catchAll route.');
assert(appLayoutSource.includes('canAccessRoutePath'), 'App layout should reuse route permission checks for restored tabs.');
assert(appLayoutSource.includes('routePaths.has(tab.path) && canAccessRoutePath(tab.path)'), 'Restored tabs should be filtered by current route permissions.');
assert(appLayoutSource.includes('if (!isKnownNavigableRoute(route.path))'), 'Current route tabs should reject 403 and catchAll pages.');
assert(appLayoutSource.includes('openTabs.value.some((tab) => tab.path === resolved.path)'), 'Preferred restored active tab should still exist after tab permission sanitizing.');
assert(appLayoutSource.includes('canAccessRoutePath(resolved.path)'), 'Preferred restored active tab should respect current route permissions.');

console.log('Verified navigation route permission contract.');
