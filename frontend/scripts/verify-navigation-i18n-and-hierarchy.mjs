import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { createServer } from 'vite';

const currentDir = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(currentDir, '..');

const sampleMenus = [
  menu('workbench', '工作台', 'menu.workbench', '/workbench', 10),
  menu('inventory-root', '进销存', 'nav.inventory', null, 20, null, [
    menu('stock-management', '库存管理', 'menu.stockManagement', null, 10, null, [
      menu('inventory', '库存', 'menu.inventory', '/inventory', 10, 'inventory:read'),
      menu('manufacturing', '组装拆分', 'menu.manufacturing', '/manufacturing', 20, 'manufacturing:read'),
    ]),
    menu('purchase-management', '采购管理', 'nav.purchase', null, 20, null, [
      menu('procurement', '采购', 'menu.procurement', '/procurement', 10, 'procurement:read'),
    ]),
  ]),
  menu('system', '系统设置', 'menu.system', '/system', 30, 'iam:view', [
    menu('iam-menu-management', '菜单管理', 'menu.iamMenus', '/system/iam/menus', 10, 'iam:view'),
  ]),
];

function menu(code, title, i18nKey, path, sortNo, permissionCode = null, children = []) {
  return {
    code,
    title,
    i18nKey,
    path,
    icon: null,
    permissionCode,
    sortNo,
    children,
  };
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function translator(messages, locale) {
  return (key) => key.split('.').reduce((value, part) => value?.[part], messages[locale]) || key;
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

const server = await createServer({
  root: projectRoot,
  logLevel: 'silent',
  server: { middlewareMode: true },
});

try {
  const [{ createNavigationMenu }, { messages }] = await Promise.all([
    server.ssrLoadModule('/src/config/navigation.ts'),
    server.ssrLoadModule('/src/i18n/messages.ts'),
  ]);

  const zhMenu = createNavigationMenu(sampleMenus, translator(messages, 'zh-CN'));
  const enMenu = createNavigationMenu(sampleMenus, translator(messages, 'en-US'));
  const zhText = JSON.stringify(zhMenu);
  const enText = JSON.stringify(enMenu);

  assert(!zhText.includes('menu.'), 'Chinese navigation still exposes raw menu.* keys.');
  assert(!enText.includes('menu.'), 'English navigation still exposes raw menu.* keys.');
  const inventoryRoot = findByKey(zhMenu, 'inventory-root');
  const stockManagement = findByKey(zhMenu, 'stock-management');
  const manufacturing = findByKey(zhMenu, 'manufacturing');
  const systemRoot = findByKey(zhMenu, 'system');

  assert(inventoryRoot?.title === '进销存', 'Chinese inventory root title is wrong.');
  assert(findByKey(enMenu, 'inventory-root')?.title === 'Inventory', 'English inventory root title is wrong.');
  assert(stockManagement?.title === '库存管理', 'Stock-management group should be translated from backend i18nKey.');
  assert(findByKey(zhMenu, 'purchase-management')?.title === '采购管理', 'Purchase group is missing or untranslated.');
  assert(systemRoot?.title === '系统设置', 'System group should keep the backend root code and title.');
  assert(findByKey(zhMenu, 'inventory')?.path === '/inventory', 'Inventory menu path is wrong.');
  assert(findByKey(zhMenu, 'iam-menu-management')?.path === '/system/iam/menus', 'IAM menu path is wrong.');
  assert(stockManagement?.children?.some((item) => item.key === 'manufacturing'), 'Manufacturing should follow backend hierarchy under stock-management.');
  assert(!inventoryRoot?.children?.some((item) => item.key === 'manufacturing'), 'Manufacturing should not stay pinned at inventory root.');
  assert(manufacturing?.path === '/manufacturing', 'Manufacturing menu path should come from backend data.');

  console.log('Verified navigation translations and hierarchy behavior.');
} finally {
  await server.close();
}
