import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const root = resolve(import.meta.dirname, '..');
const layoutSource = readFileSync(resolve(root, 'src/layouts/AppLayout.vue'), 'utf8');

assert(
  layoutSource.includes('function collectActiveMenuKeys(items: MenuNode[], currentPath: string)'),
  'AppLayout should compute open menu keys from the active route path.',
);
assert(
  layoutSource.includes('openKeys.value = collectActiveMenuKeys(items, route.path);'),
  'Menu data watcher should open only active ancestor menus.',
);
assert(
  layoutSource.includes('openKeys.value = collectActiveMenuKeys(menuGroups.value, currentPath);'),
  'Route watcher should update open menus to the new active route ancestors.',
);
assert(
  !layoutSource.includes('openKeys.value = collectMenuKeys(items);'),
  'AppLayout should not expand every menu group by default.',
);
