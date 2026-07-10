<template>
  <div class="app-shell" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
    <aside class="app-sidebar">
      <div class="sidebar-head">
        <AppLogo />
        <button class="shell-icon-button sidebar-collapse" type="button" aria-label="折叠菜单" @click="collapseSidebar">
          <PanelLeftOpen v-if="isSidebarCollapsed" :size="16" />
          <PanelLeftClose v-else :size="16" />
        </button>
      </div>

      <label class="sidebar-search">
        <Search :size="16" />
        <input
          v-model.trim="menuSearchKeyword"
          :placeholder="t('shell.search')"
          @keydown.enter.prevent="handleMenuSearchEnter"
        />
      </label>

      <nav class="side-nav" aria-label="main navigation">
        <template v-for="item in filteredMenuGroups" :key="item.key">
          <RouterLink
            v-if="item.path"
            class="side-item side-root"
            :class="{ active: isPathActive(item.path) }"
            :title="item.title"
            :to="item.path"
          >
            <component :is="item.icon" v-if="item.icon" :size="16" />
            <span class="side-label">{{ item.title }}</span>
          </RouterLink>

          <div v-else class="side-tree">
            <button
              class="side-item side-root side-toggle"
              :class="{ active: isMenuActive(item) }"
              :title="item.title"
              type="button"
              @click="toggleMenu(item.key)"
            >
              <component :is="item.icon" v-if="item.icon" :size="16" />
              <span class="side-label">{{ item.title }}</span>
              <ChevronRight class="side-arrow" :class="{ open: isMenuOpen(item.key) }" :size="15" />
            </button>

            <div v-if="!isSidebarCollapsed && isMenuOpen(item.key)" class="side-children">
              <template v-for="child in item.children" :key="child.key">
                <RouterLink
                  v-if="child.path"
                  class="side-item side-child"
                  :class="{ active: isPathActive(child.path), muted: child.disabled }"
                  :title="child.title"
                  :to="menuTarget(child)"
                >
                  <span class="side-label">{{ child.title }}</span>
                </RouterLink>

                <div v-else class="side-tree">
                  <button
                    class="side-item side-child side-toggle"
                    :class="{ active: isMenuActive(child) }"
                    :title="child.title"
                    type="button"
                    @click="toggleMenu(child.key)"
                  >
                    <span class="side-label">{{ child.title }}</span>
                    <ChevronRight class="side-arrow" :class="{ open: isMenuOpen(child.key) }" :size="15" />
                  </button>

                  <div v-if="isMenuOpen(child.key)" class="side-grandchildren">
                    <RouterLink
                      v-for="grandchild in child.children"
                      :key="grandchild.key"
                      class="side-item side-grandchild"
                      :class="{ active: isPathActive(grandchild.path), muted: grandchild.disabled }"
                      :title="grandchild.title"
                      :to="menuTarget(grandchild)"
                    >
                      <span class="side-label">{{ grandchild.title }}</span>
                    </RouterLink>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </template>
      </nav>
    </aside>

    <section class="app-main">
      <header class="app-topbar">
        <nav class="breadcrumb" aria-label="breadcrumb">
          <template v-for="(item, index) in breadcrumbItems" :key="`${item.title}-${index}`">
            <RouterLink
              v-if="item.path && index < breadcrumbItems.length - 1"
              class="breadcrumb-link"
              :to="item.path"
            >
              {{ item.title }}
            </RouterLink>
            <span v-else :aria-current="index === breadcrumbItems.length - 1 ? 'page' : undefined">{{ item.title }}</span>
            <span v-if="index < breadcrumbItems.length - 1" class="breadcrumb-separator">/</span>
          </template>
        </nav>

        <div class="topbar-actions">
          <span class="tenant-pill">{{ t('shell.tenant') }} {{ authStore.tenantId || 'default' }}</span>
          <LanguageSwitcher />
          <ThemeSwitcher />
          <button class="shell-icon-button layout-settings" type="button" aria-label="布局设置" @click="layoutSettingsVisible = true">
            <SlidersHorizontal :size="16" />
          </button>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <button class="avatar-button" type="button">{{ authStore.username.slice(0, 1).toUpperCase() }}</button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">{{ t('shell.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div v-if="settingsStore.showTabs" class="tab-strip" aria-label="open pages">
        <RouterLink
          v-for="tab in openTabs"
          :key="tab.path"
          class="page-tab"
          :class="{ active: tab.path === route.path }"
          :to="tab.path"
          @mousedown.middle.prevent
          @auxclick.prevent="handleTabAuxClick($event, tab)"
          @contextmenu.prevent.stop="openTabContextMenu($event, tab)"
        >
          <span v-if="tab.path === route.path" class="tab-dot" />
          <span>{{ tab.title }}</span>
          <button
            v-if="tab.closable !== false"
            class="tab-close"
            type="button"
            aria-label="关闭标签"
            @click.prevent.stop="removeTab(tab)"
          >
            <X :size="13" />
          </button>
        </RouterLink>

        <div
          v-if="tabContextMenu.visible"
          class="tab-context-menu"
          :style="{ left: `${tabContextMenu.x}px`, top: `${tabContextMenu.y}px` }"
          @click.stop
        >
          <button type="button" :disabled="!canCloseTab(tabContextMenu.tab)" @click="closeContextTab">关闭当前</button>
          <button type="button" @click="closeOtherTabs">关闭其他</button>
          <button type="button" @click="closeLeftTabs">关闭左侧</button>
          <button type="button" @click="closeRightTabs">关闭右侧</button>
          <button type="button" @click="closeAllTabs">关闭全部</button>
          <button type="button" @click="refreshCurrentTab">刷新当前页</button>
        </div>
      </div>

      <div class="route-loading-bar" :class="{ active: routeLoading }" />

      <main class="app-content">
        <RouterView v-slot="{ Component, route: viewRoute }">
          <KeepAlive :include="cachedRouteNames">
            <component
              :is="Component"
              v-if="viewRoute.meta.keepAlive"
              :key="routeComponentKey(viewRoute)"
            />
          </KeepAlive>
          <component
            :is="Component"
            v-if="!viewRoute.meta.keepAlive"
            :key="routeComponentKey(viewRoute)"
          />
        </RouterView>
      </main>
    </section>

    <el-drawer v-model="layoutSettingsVisible" title="布局设置" direction="rtl" size="320px">
      <div class="settings-panel">
        <label class="settings-row">
          <span>暗色模式</span>
          <el-switch :model-value="settingsStore.darkMode" @update:model-value="settingsStore.setDarkMode" />
        </label>
        <label class="settings-row">
          <span>紧凑模式</span>
          <el-switch :model-value="settingsStore.compactMode" @update:model-value="settingsStore.setCompactMode" />
        </label>
        <label class="settings-row">
          <span>显示标签页</span>
          <el-switch :model-value="settingsStore.showTabs" @update:model-value="settingsStore.setShowTabs" />
        </label>
        <label class="settings-row">
          <span>显示面包屑</span>
          <el-switch :model-value="settingsStore.showBreadcrumb" @update:model-value="settingsStore.setShowBreadcrumb" />
        </label>
        <label class="settings-row">
          <span>固定顶部栏</span>
          <el-switch :model-value="settingsStore.fixedTopbar" @update:model-value="settingsStore.setFixedTopbar" />
        </label>
        <div class="settings-block">
          <span>字号</span>
          <el-radio-group :model-value="settingsStore.fontSize" @update:model-value="settingsStore.setFontSize">
            <el-radio-button label="small">小</el-radio-button>
            <el-radio-button label="default">默认</el-radio-button>
            <el-radio-button label="large">大</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter, type RouteLocationNormalizedLoaded } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ChevronRight, PanelLeftClose, PanelLeftOpen, Search, SlidersHorizontal, X } from 'lucide-vue-next';
import AppLogo from '@/components/app/AppLogo.vue';
import LanguageSwitcher from '@/components/app/LanguageSwitcher.vue';
import ThemeSwitcher from '@/components/app/ThemeSwitcher.vue';
import { createNavigationMenu, type MenuNode } from '@/config/navigation';
import { useAuthStore } from '@/stores/auth';
import { useSettingsStore } from '@/stores/settings';

type BreadcrumbItem = {
  title: string;
  path?: string;
};

type PageTab = {
  title: string;
  path: string;
  affixTab?: boolean;
  closable?: boolean;
};

const OPEN_TABS_STORAGE_KEY = 'xuan-erp-open-tabs-v1';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const authStore = useAuthStore();
const settingsStore = useSettingsStore();

const menuGroups = computed(() => createNavigationMenu(t));
const accessibleMenuGroups = computed(() => filterAccessibleMenuTree(menuGroups.value));
const filteredMenuGroups = computed(() => {
  const keyword = menuSearchKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return accessibleMenuGroups.value;
  }
  return filterMenuTree(accessibleMenuGroups.value, keyword);
});
const openKeys = ref(['inventory', 'base-data', 'purchase-management']);
const isSidebarCollapsed = ref(false);
const menuSearchKeyword = ref('');
const layoutSettingsVisible = ref(false);
const routeLoading = ref(false);
const openTabs = ref<PageTab[]>([]);
const routeRefreshVersion = ref<Record<string, number>>({});
const tabContextMenu = reactive<{
  visible: boolean;
  x: number;
  y: number;
  tab: PageTab | null;
}>({
  visible: false,
  x: 0,
  y: 0,
  tab: null,
});

const dashboardTab = computed<PageTab>(() => ({
  title: '仪表盘',
  path: '/dashboard',
  affixTab: true,
  closable: false,
}));

const breadcrumbItems = computed<BreadcrumbItem[]>(() => {
  if (!settingsStore.showBreadcrumb) {
    return [];
  }
  const breadcrumb = route.meta.breadcrumb;
  if (Array.isArray(breadcrumb)) {
    return breadcrumb as BreadcrumbItem[];
  }
  if (typeof breadcrumb === 'string') {
    return breadcrumb.split('/').map((title) => ({ title: title.trim() }));
  }
  return [{ title: String(route.meta.title || '仪表盘') }];
});

const cachedRouteNames = computed(() => [...new Set(openTabs.value
  .map((tab) => getRouteMetaByPath(tab.path)?.cacheName)
  .filter((name): name is string => typeof name === 'string' && name.length > 0))]);

function collapseSidebar() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
}

function isMenuOpen(key: string) {
  return openKeys.value.includes(key);
}

function toggleMenu(key: string) {
  if (isSidebarCollapsed.value) {
    isSidebarCollapsed.value = false;
  }
  if (isMenuOpen(key)) {
    openKeys.value = openKeys.value.filter((item) => item !== key);
  } else {
    openKeys.value = [...openKeys.value, key];
  }
}

function filterAccessibleMenuTree(items: MenuNode[]): MenuNode[] {
  const result: MenuNode[] = [];
  for (const item of items) {
    if (!authStore.hasPermission(item.permission)) {
      continue;
    }
    const children = item.children ? filterAccessibleMenuTree(item.children) : undefined;
    if (item.path || children?.length) {
      result.push({ ...item, children });
    }
  }
  return result;
}

function filterMenuTree(items: MenuNode[], keyword: string): MenuNode[] {
  const result: MenuNode[] = [];
  for (const item of items) {
    const children = item.children ? filterMenuTree(item.children, keyword) : undefined;
    const selfMatched = item.title.toLowerCase().includes(keyword);
    if (selfMatched || children?.length) {
      result.push({ ...item, children });
    }
  }
  return result;
}

function firstNavigableMenu(items: MenuNode[]): MenuNode | null {
  for (const item of items) {
    if (item.path && !item.disabled) {
      return item;
    }
    const child = item.children ? firstNavigableMenu(item.children) : null;
    if (child) {
      return child;
    }
  }
  return null;
}

async function handleMenuSearchEnter() {
  const target = firstNavigableMenu(filteredMenuGroups.value);
  if (target?.path) {
    await router.push(target.path);
  }
}

function isPathActive(path?: string) {
  return Boolean(path && route.path === path);
}

function menuTarget(item: MenuNode) {
  if (item.disabled || !item.path) {
    return route.fullPath;
  }
  return item.path;
}

function getRouteMetaByPath(path: string) {
  return router.resolve(path).meta;
}

function makeTab(path: string): PageTab {
  const resolved = router.resolve(path);
  return {
    title: String(resolved.meta.title || '仪表盘'),
    path: resolved.path,
    affixTab: Boolean(resolved.meta.affixTab),
    closable: resolved.meta.closable !== false,
  };
}

function sanitizeTabs(tabs: PageTab[]) {
  const routePaths = new Set(router.getRoutes()
    .filter((item) => !item.meta.public && item.path !== '/')
    .map((item) => router.resolve(item.path).path));
  const cleaned = [dashboardTab.value, ...tabs]
    .filter((tab) => tab && typeof tab.path === 'string' && routePaths.has(tab.path))
    .map((tab) => (tab.path === '/dashboard' ? dashboardTab.value : makeTab(tab.path)));
  return cleaned.filter((tab, index, array) => array.findIndex((item) => item.path === tab.path) === index);
}

function restoreTabs() {
  try {
    const raw = sessionStorage.getItem(openTabsStorageKey());
    openTabs.value = sanitizeTabs(raw ? JSON.parse(raw) as PageTab[] : []);
  } catch {
    openTabs.value = [dashboardTab.value];
  }
}

function persistTabs() {
  sessionStorage.setItem(openTabsStorageKey(), JSON.stringify(openTabs.value));
}

function openTabsStorageKey() {
  return `${OPEN_TABS_STORAGE_KEY}:${authStore.tenantId}:${authStore.username}`;
}

function hasActiveChild(item: MenuNode): boolean {
  return Boolean(item.children?.some((child) => isPathActive(child.path) || hasActiveChild(child)));
}

function isMenuActive(item: MenuNode) {
  return hasActiveChild(item);
}

function addCurrentRouteTab() {
  if (route.meta.public || route.path === '/') {
    return;
  }
  if (!openTabs.value.some((tab) => tab.path === route.path)) {
    openTabs.value = [...openTabs.value, makeTab(route.path)];
  }
}

function canCloseTab(tab: PageTab | null) {
  return Boolean(tab && tab.closable !== false && openTabs.value.length > 1);
}

async function closeTab(tab: PageTab | null) {
  if (!canCloseTab(tab)) {
    return;
  }
  const target = tab as PageTab;
  const closingIndex = openTabs.value.findIndex((item) => item.path === target.path);
  if (closingIndex === -1) {
    return;
  }
  const isClosingCurrent = route.path === target.path;
  openTabs.value = openTabs.value.filter((item) => item.path !== target.path);
  delete routeRefreshVersion.value[target.path];

  if (isClosingCurrent) {
    const fallback = openTabs.value[Math.max(0, closingIndex - 1)] || openTabs.value[0] || dashboardTab.value;
    await router.push(fallback.path);
  }
}

async function removeTab(tab: PageTab) {
  await closeTab(tab);
}

function routeComponentKey(viewRoute: RouteLocationNormalizedLoaded) {
  const cacheName = typeof viewRoute.meta.cacheName === 'string' ? viewRoute.meta.cacheName : viewRoute.fullPath;
  return `${cacheName}:${viewRoute.fullPath}:${routeRefreshVersion.value[viewRoute.fullPath] || 0}`;
}

function handleTabAuxClick(event: MouseEvent, tab: PageTab) {
  if (event.button === 1) {
    closeTab(tab);
  }
}

function boundedMenuPosition(event: MouseEvent) {
  const width = 132;
  const height = 196;
  return {
    x: Math.min(event.clientX, window.innerWidth - width - 8),
    y: Math.min(event.clientY, window.innerHeight - height - 8),
  };
}

function openTabContextMenu(event: MouseEvent, tab: PageTab) {
  const position = boundedMenuPosition(event);
  tabContextMenu.visible = true;
  tabContextMenu.x = position.x;
  tabContextMenu.y = position.y;
  tabContextMenu.tab = tab;
}

function hideTabContextMenu() {
  tabContextMenu.visible = false;
}

async function closeContextTab() {
  await closeTab(tabContextMenu.tab);
  hideTabContextMenu();
}

async function closeOtherTabs() {
  const target = tabContextMenu.tab;
  if (!target) {
    return;
  }
  openTabs.value = openTabs.value.filter((tab) => tab.affixTab || tab.path === target.path);
  if (!openTabs.value.some((tab) => tab.path === route.path)) {
    await router.push(target.path);
  }
  hideTabContextMenu();
}

async function closeLeftTabs() {
  const target = tabContextMenu.tab;
  if (!target) {
    return;
  }
  const targetIndex = openTabs.value.findIndex((tab) => tab.path === target.path);
  openTabs.value = openTabs.value.filter((tab, index) => tab.affixTab || index >= targetIndex);
  if (!openTabs.value.some((tab) => tab.path === route.path)) {
    await router.push(target.path);
  }
  hideTabContextMenu();
}

async function closeRightTabs() {
  const target = tabContextMenu.tab;
  if (!target) {
    return;
  }
  const targetIndex = openTabs.value.findIndex((tab) => tab.path === target.path);
  openTabs.value = openTabs.value.filter((tab, index) => tab.affixTab || index <= targetIndex);
  if (!openTabs.value.some((tab) => tab.path === route.path)) {
    await router.push(target.path);
  }
  hideTabContextMenu();
}

async function closeAllTabs() {
  openTabs.value = openTabs.value.filter((tab) => tab.affixTab);
  const fallback = openTabs.value[0] || dashboardTab.value;
  if (route.path !== fallback.path) {
    await router.push(fallback.path);
  }
  hideTabContextMenu();
}

function refreshCurrentTab() {
  const targetPath = tabContextMenu.tab?.path || route.path;
  routeRefreshVersion.value = {
    ...routeRefreshVersion.value,
    [targetPath]: (routeRefreshVersion.value[targetPath] || 0) + 1,
  };
  hideTabContextMenu();
}

onMounted(() => {
  settingsStore.applyTheme();
  restoreTabs();
  addCurrentRouteTab();
  window.addEventListener('click', hideTabContextMenu);
});

onBeforeUnmount(() => {
  window.removeEventListener('click', hideTabContextMenu);
});

watch(
  () => route.path,
  () => {
    routeLoading.value = true;
    window.setTimeout(() => {
      routeLoading.value = false;
    }, 220);
    addCurrentRouteTab();
  },
  { immediate: true },
);

watch(openTabs, persistTabs, { deep: true });

function handleUserCommand(command: string) {
  if (command === 'logout') {
    const storageKey = openTabsStorageKey();
    authStore.logout();
    sessionStorage.removeItem(storageKey);
    router.push('/login');
  }
}
</script>
