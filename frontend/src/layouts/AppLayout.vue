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
        <div class="nav-scroll-area">
          <ul class="menu-root">
            <li v-for="item in filteredMenuGroups" :key="item.key" class="menu-item-l1">
              <button
                class="menu-label l1"
                :class="{ 'is-active': isMenuItemActive(item), 'is-muted': item.disabled }"
                :title="item.title"
                type="button"
                @click="handleMenuClick(item)"
              >
                <span class="icon-box">
                  <component :is="item.icon" v-if="item.icon" :size="16" />
                </span>
                <span class="label-text">{{ item.title }}</span>
                <ChevronRight
                  v-if="hasChildren(item)"
                  class="chevron"
                  :class="{ rotated: isMenuExpanded(item) }"
                  :size="15"
                />
              </button>

              <Transition name="slide-down">
                <ul v-if="hasChildren(item) && isMenuExpanded(item)" class="submenu-l2">
                  <li v-for="child in item.children" :key="child.key">
                    <button
                      class="menu-label l2"
                      :class="{ 'is-active': isMenuItemActive(child), 'is-muted': child.disabled }"
                      :title="child.title"
                      type="button"
                      @click="handleMenuClick(child)"
                    >
                      <span v-if="!hasChildren(child)" class="menu-dot" />
                      <span class="label-text">{{ child.title }}</span>
                      <ChevronRight
                        v-if="hasChildren(child)"
                        class="chevron"
                        :class="{ rotated: isMenuExpanded(child) }"
                        :size="14"
                      />
                    </button>

                    <Transition name="slide-down">
                      <ul v-if="hasChildren(child) && isMenuExpanded(child)" class="submenu-l3">
                        <li v-for="grandchild in child.children" :key="grandchild.key">
                          <button
                            class="menu-label l3"
                            :class="{ 'is-active': isMenuItemActive(grandchild), 'is-muted': grandchild.disabled }"
                            :title="grandchild.title"
                            type="button"
                            @click="handleMenuClick(grandchild)"
                          >
                            <span class="label-text">{{ grandchild.title }}</span>
                          </button>
                        </li>
                      </ul>
                    </Transition>
                  </li>
                </ul>
              </Transition>

              <div v-if="isSidebarCollapsed && hasChildren(item)" class="sidebar-flyout" @click.stop>
                <strong class="sidebar-flyout-title">{{ item.title }}</strong>
                <ul class="sidebar-flyout-list">
                  <li v-for="child in item.children" :key="child.key">
                    <button
                      class="sidebar-flyout-item"
                      :class="{ 'is-active': isMenuItemActive(child), 'is-muted': child.disabled }"
                      type="button"
                      @click="handleFlyoutMenuClick(child)"
                    >
                      {{ child.title }}
                    </button>
                    <ul v-if="hasChildren(child)" class="sidebar-flyout-sublist">
                      <li v-for="grandchild in child.children" :key="grandchild.key">
                        <button
                          class="sidebar-flyout-item child"
                          :class="{ 'is-active': isMenuItemActive(grandchild), 'is-muted': grandchild.disabled }"
                          type="button"
                          @click="handleFlyoutMenuClick(grandchild)"
                        >
                          {{ grandchild.title }}
                        </button>
                      </li>
                    </ul>
                  </li>
                </ul>
              </div>
            </li>
          </ul>
        </div>
      </nav>

      <div class="sidebar-footer">{{ frameworkConfig.shell.footerText }}</div>
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
          <span v-if="frameworkConfig.shell.showTenant" class="tenant-pill">
            {{ t(frameworkConfig.shell.tenantLabelKey) }} {{ authStore.tenantId || 'default' }}
          </span>
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
                <el-dropdown-item command="logout-clear-device" divided>{{ t('shell.logoutClearDevice') }}</el-dropdown-item>
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
          <span>{{ tabTitle(tab) }}</span>
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

      <main ref="contentRef" class="app-content" @scroll.passive="handleContentScroll">
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
import { ElMessage } from 'element-plus/es/components/message/index';
import { ElMessageBox } from 'element-plus/es/components/message-box/index';
import { ChevronRight, PanelLeftClose, PanelLeftOpen, Search, SlidersHorizontal, X } from 'lucide-vue-next';
import { appFrameworkConfig } from '@/app/frameworkConfig';
import AppLogo from '@/components/app/AppLogo.vue';
import LanguageSwitcher from '@/components/app/LanguageSwitcher.vue';
import ThemeSwitcher from '@/components/app/ThemeSwitcher.vue';
import { getUserPreference, saveUserPreference } from '@/api/preferences';
import { createNavigationMenu, type MenuNode } from '@/config/navigation';
import { useAuthStore } from '@/stores/auth';
import { useAuthorizationStore } from '@/stores/authorization';
import { useSettingsStore } from '@/stores/settings';
import { clearLoginProfiles } from '@/utils/loginProfiles';

type BreadcrumbItem = {
  title?: string;
  titleKey?: string;
  path?: string;
};

type PageTab = {
  title: string;
  path: string;
  affixTab?: boolean;
  closable?: boolean;
};

const frameworkConfig = appFrameworkConfig;
const OPEN_TABS_STORAGE_KEY = frameworkConfig.storage.openTabsKeyPrefix;
const OPEN_TABS_PREFERENCE_KEY = frameworkConfig.preferences.shellTabsKey;

type ShellTabsPreference = {
  tabs: PageTab[];
  activePath: string;
  scrollPositions: Record<string, number>;
};

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const authStore = useAuthStore();
const authorizationStore = useAuthorizationStore();
const settingsStore = useSettingsStore();

const menuGroups = computed(() => createNavigationMenu(authorizationStore.menus, t));
const filteredMenuGroups = computed(() => {
  const keyword = menuSearchKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return menuGroups.value;
  }
  return filterMenuTree(menuGroups.value, keyword);
});
const openKeys = ref<string[]>([]);
const isSidebarCollapsed = ref(false);
const menuSearchKeyword = ref('');
const layoutSettingsVisible = ref(false);
const routeLoading = ref(false);
const isLoggingOut = ref(false);
const openTabs = ref<PageTab[]>([]);
const contentRef = ref<HTMLElement | null>(null);
const routeRefreshVersion = ref<Record<string, number>>({});
const scrollPositions = ref<Record<string, number>>({});
let tabPreferenceRestored = false;
let scrollSaveTimer: number | undefined;
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
  title: t('route.dashboard'),
  path: frameworkConfig.routes.homePath,
  affixTab: true,
  closable: false,
}));

const breadcrumbItems = computed<BreadcrumbItem[]>(() => {
  if (!settingsStore.showBreadcrumb) {
    return [];
  }
  return resolveBreadcrumbItems(route);
});

function resolveBreadcrumbItems(currentRoute: RouteLocationNormalizedLoaded): BreadcrumbItem[] {
  const breadcrumb = currentRoute.meta.breadcrumb;
  if (Array.isArray(breadcrumb)) {
    return (breadcrumb as BreadcrumbItem[]).map((item) => ({
      ...item,
      title: resolveLocalizedText(item.titleKey, item.title),
    }));
  }
  if (typeof breadcrumb === 'string') {
    return breadcrumb.split('/').map((title) => ({ title: title.trim() }));
  }
  return [{ title: resolveRouteTitle(currentRoute) }];
}

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

function hasChildren(item: MenuNode) {
  return Boolean(item.children?.length);
}

function isMenuExpanded(item: MenuNode) {
  return !isSidebarCollapsed.value && (isMenuOpen(item.key) || Boolean(menuSearchKeyword.value.trim()));
}

async function handleMenuClick(item: MenuNode) {
  if (item.disabled) {
    return;
  }
  if (hasChildren(item)) {
    toggleMenu(item.key);
    return;
  }
  if (item.path) {
    await router.push(item.path);
  }
}

async function handleFlyoutMenuClick(item: MenuNode) {
  if (item.disabled || !item.path) {
    return;
  }
  await router.push(item.path);
}

function getRouteMetaByPath(path: string) {
  return router.resolve(path).meta;
}

function resolveLocalizedText(titleKey?: string, fallback?: unknown) {
  if (titleKey) {
    const translated = t(titleKey);
    if (translated !== titleKey) {
      return translated;
    }
  }
  return String(fallback || t('route.dashboard'));
}

function resolveRouteTitle(targetRoute: RouteLocationNormalizedLoaded | ReturnType<typeof router.resolve>) {
  return resolveLocalizedText(targetRoute.meta.titleKey as string | undefined, targetRoute.meta.title);
}

function tabTitle(tab: PageTab) {
  return resolveRouteTitle(router.resolve(tab.path)) || tab.title;
}

function makeTab(path: string): PageTab {
  const resolved = router.resolve(path);
  return {
    title: resolveRouteTitle(resolved),
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
    .map((tab) => (tab.path === frameworkConfig.routes.homePath ? dashboardTab.value : makeTab(tab.path)));
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

async function restoreTabsFromPreference() {
  try {
    const preference = await getUserPreference<ShellTabsPreference>(OPEN_TABS_PREFERENCE_KEY);
    if (!preference) {
      restoreTabs();
      return;
    }
    openTabs.value = sanitizeTabs(preference.tabs || []);
    scrollPositions.value = normalizeScrollPositions(preference.scrollPositions);
    persistTabs();
    if (route.path === frameworkConfig.routes.homePath && preference.activePath && preference.activePath !== route.path) {
      const resolved = router.resolve(preference.activePath);
      if (!resolved.meta.public && resolved.matched.length) {
        await router.replace(resolved.path);
      }
    }
  } catch {
    restoreTabs();
  } finally {
    tabPreferenceRestored = true;
  }
}

function persistTabs() {
  sessionStorage.setItem(openTabsStorageKey(), JSON.stringify(openTabs.value));
}

function shouldSaveTabsPreferenceRemotely() {
  return tabPreferenceRestored && authStore.isAuthenticated && !isLoggingOut.value;
}

function saveTabsPreference() {
  if (!shouldSaveTabsPreferenceRemotely()) {
    return;
  }
  const preference: ShellTabsPreference = {
    tabs: openTabs.value,
    activePath: route.path,
    scrollPositions: scrollPositions.value,
  };
  saveUserPreference(OPEN_TABS_PREFERENCE_KEY, preference).catch(() => {
    // 标签页状态先落 sessionStorage；后端短暂失败不影响 tab 切换手感。
  });
}

function openTabsStorageKey() {
  return `${OPEN_TABS_STORAGE_KEY}:${authStore.tenantId}:${authStore.username}`;
}

function hasActiveChild(item: MenuNode): boolean {
  return Boolean(item.children?.some((child) => isMenuItemActive(child)));
}

function isMenuItemActive(item: MenuNode): boolean {
  if (item.path && route.path === item.path) {
    return true;
  }
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
  delete scrollPositions.value[target.path];

  if (isClosingCurrent) {
    const fallback = openTabs.value[Math.max(0, closingIndex - 1)] || openTabs.value[0] || dashboardTab.value;
    await router.push(fallback.path);
  }
}

async function removeTab(tab: PageTab) {
  await closeTab(tab);
}

function routeComponentKey(viewRoute: RouteLocationNormalizedLoaded) {
  const cacheName = typeof viewRoute.meta.cacheName === 'string' ? viewRoute.meta.cacheName : viewRoute.path;
  return `${cacheName}:${viewRoute.path}:${routeRefreshVersion.value[viewRoute.path] || 0}`;
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
  pruneScrollPositions();
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
  pruneScrollPositions();
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
  pruneScrollPositions();
  if (!openTabs.value.some((tab) => tab.path === route.path)) {
    await router.push(target.path);
  }
  hideTabContextMenu();
}

async function closeAllTabs() {
  openTabs.value = openTabs.value.filter((tab) => tab.affixTab);
  pruneScrollPositions();
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

function normalizeScrollPositions(value: unknown) {
  if (!value || typeof value !== 'object') {
    return {};
  }
  return Object.fromEntries(Object.entries(value as Record<string, unknown>)
    .filter(([, scrollTop]) => typeof scrollTop === 'number' && Number.isFinite(scrollTop))
    .map(([path, scrollTop]) => [path, Math.max(0, Math.round(scrollTop as number))]));
}

function pruneScrollPositions() {
  const openPaths = new Set(openTabs.value.map((tab) => tab.path));
  scrollPositions.value = Object.fromEntries(
    Object.entries(scrollPositions.value).filter(([path]) => openPaths.has(path)),
  );
}

function saveContentScroll(path = route.path) {
  const scrollTop = contentRef.value?.scrollTop || 0;
  scrollPositions.value = {
    ...scrollPositions.value,
    [path]: Math.max(0, Math.round(scrollTop)),
  };
}

function restoreContentScroll(path = route.path) {
  window.requestAnimationFrame(() => {
    if (!contentRef.value) {
      return;
    }
    contentRef.value.scrollTop = scrollPositions.value[path] || 0;
  });
}

function handleContentScroll() {
  if (scrollSaveTimer) {
    window.clearTimeout(scrollSaveTimer);
  }
  scrollSaveTimer = window.setTimeout(() => {
    saveContentScroll();
    saveTabsPreference();
  }, 160);
}

onMounted(async () => {
  await settingsStore.loadRemotePreferences();
  settingsStore.applyTheme();
  await restoreTabsFromPreference();
  addCurrentRouteTab();
  restoreContentScroll();
  window.addEventListener('click', hideTabContextMenu);
});

onBeforeUnmount(() => {
  saveContentScroll();
  saveTabsPreference();
  if (scrollSaveTimer) {
    window.clearTimeout(scrollSaveTimer);
  }
  window.removeEventListener('click', hideTabContextMenu);
});

watch(
  menuGroups,
  (items) => {
    openKeys.value = collectActiveMenuKeys(items, route.path);
  },
  { immediate: true },
);

watch(
  () => route.path,
  (currentPath, previousPath) => {
    if (previousPath) {
      saveContentScroll(previousPath);
    }
    routeLoading.value = true;
    window.setTimeout(() => {
      routeLoading.value = false;
    }, 220);
    if (tabPreferenceRestored) {
      addCurrentRouteTab();
      restoreContentScroll(currentPath);
      saveTabsPreference();
    }
    openKeys.value = collectActiveMenuKeys(menuGroups.value, currentPath);
  },
  { immediate: true },
);

watch(openTabs, () => {
  persistTabs();
  saveTabsPreference();
}, { deep: true });

async function handleUserCommand(command: string) {
  const storageKey = openTabsStorageKey();
  if (command === 'logout') {
    isLoggingOut.value = true;
    await authStore.logout();
    sessionStorage.removeItem(storageKey);
    await router.push(frameworkConfig.routes.loginPath);
    return;
  }
  if (command === 'logout-clear-device') {
    try {
      await ElMessageBox.confirm(
        t('shell.clearLocalRecordsMessage'),
        t('shell.clearLocalRecordsTitle'),
        {
          type: 'warning',
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
        },
      );
    } catch {
      return;
    }
    clearLoginProfiles();
    isLoggingOut.value = true;
    await authStore.logout();
    sessionStorage.removeItem(storageKey);
    await router.push(frameworkConfig.routes.loginPath);
    ElMessage.success(t('shell.localRecordsCleared'));
  }
}

function collectActiveMenuKeys(items: MenuNode[], currentPath: string) {
  const activeKeys: string[] = [];
  for (const item of items) {
    if (!item.children?.length) {
      continue;
    }
    if (item.children.some((child) => isMenuItemActiveForPath(child, currentPath))) {
      activeKeys.push(item.key);
      activeKeys.push(...collectActiveMenuKeys(item.children, currentPath));
    }
  }
  return activeKeys;
}

function isMenuItemActiveForPath(item: MenuNode, currentPath: string): boolean {
  if (item.path && currentPath.startsWith(item.path)) {
    return true;
  }
  return Boolean(item.children?.some((child) => isMenuItemActiveForPath(child, currentPath)));
}
</script>
