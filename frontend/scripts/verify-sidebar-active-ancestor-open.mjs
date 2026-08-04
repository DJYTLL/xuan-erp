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
  layoutSource.includes('const manualSidebarCollapsed = ref(false);'),
  'AppLayout should track user initiated sidebar collapse separately from responsive compact mode.',
);
assert(
  layoutSource.includes('const isCompactViewport = ref(false);'),
  'AppLayout should track compact viewport mode for narrow desktop and tablet widths.',
);
assert(
  layoutSource.includes('const isMobileViewport = ref(false);'),
  'AppLayout should track mobile viewport mode for drawer navigation.',
);
assert(
  layoutSource.includes('const mobileNavVisible = ref(false);'),
  'AppLayout should expose mobile drawer visibility state.',
);
assert(
  layoutSource.includes('const isSidebarCollapsed = computed(() => manualSidebarCollapsed.value || isCompactViewport.value);'),
  'AppLayout should derive the real collapsed state from manual and responsive compact inputs.',
);
assert(
  layoutSource.includes('function syncViewportState()'),
  'AppLayout should synchronize responsive navigation mode from the viewport width.',
);
assert(
  layoutSource.includes('window.addEventListener(\'resize\', syncViewportState);'),
  'AppLayout should update responsive navigation state on resize.',
);
assert(
  layoutSource.includes('<el-drawer v-model="mobileNavVisible"'),
  'AppLayout should render a dedicated mobile navigation drawer.',
);
assert(
  layoutSource.includes('class="shell-icon-button mobile-nav-toggle"'),
  'AppLayout should expose a mobile navigation toggle button in the topbar.',
);
assert(
  layoutSource.includes('function mergeOpenMenuKeys(currentOpenKeys: string[], items: MenuNode[], currentPath: string)'),
  'AppLayout should merge manually opened menus with active ancestor menus.',
);
assert(
  layoutSource.includes('openKeys.value = mergeOpenMenuKeys(openKeys.value, items, route.path);'),
  'Menu data watcher should preserve valid manually opened menus while adding active ancestors.',
);
assert(
  layoutSource.includes('openKeys.value = mergeOpenMenuKeys(openKeys.value, menuGroups.value, currentPath);'),
  'Route watcher should not replace manually opened menus when navigating to another branch.',
);
assert(
  layoutSource.includes('function normalizeNavigablePath(path: string)'),
  'AppLayout should normalize redirect menu paths before active matching.',
);
assert(
  layoutSource.includes('normalizeNavigablePath(item.path) === normalizeNavigablePath(currentPath)'),
  'Sidebar active matching should handle redirect menu paths such as /product -> /inventory/products.',
);
assert(
  !layoutSource.includes('openKeys.value = collectMenuKeys(items);'),
  'AppLayout should not expand every menu group by default.',
);
