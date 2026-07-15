import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { createServer } from 'vite';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');
const routerSource = readFileSync(resolve(currentDir, '../src/router/index.ts'), 'utf8');
const navigationSource = readFileSync(resolve(currentDir, '../src/framework/navigation/menu.ts'), 'utf8');

const routePermissions = {
  '/inventory/products': ['product:view'],
  '/purchase/orders/draft': ['procurement:view'],
  '/purchase/orders/create': ['procurement:create'],
  '/purchase/orders/approved': ['procurement:view'],
  '/purchase/returns/draft': ['procurement:view'],
  '/purchase/returns/approved': ['procurement:view'],
  '/components': ['iam:view'],
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

const server = await createServer({
  root: frontendRoot,
  logLevel: 'silent',
  server: { middlewareMode: true },
});

try {
  const { createNavigationMenu } = await server.ssrLoadModule('/src/config/navigation.ts');
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
} finally {
  await server.close();
}

console.log('Verified navigation route permission contract.');
