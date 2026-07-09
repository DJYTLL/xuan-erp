<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <AppLogo />
      <div class="sidebar-search">
        <Search :size="16" />
        <span>{{ t('shell.search') }}</span>
      </div>

      <nav class="side-nav">
        <RouterLink class="side-item" to="/dashboard" active-class="active">
          <LayoutDashboard :size="16" />
          <span>{{ t('nav.dashboard') }}</span>
        </RouterLink>
        <div class="side-section active">
          <PackageOpen :size="16" />
          <span>{{ t('nav.inventory') }}</span>
        </div>
        <div class="side-subtitle">{{ t('nav.baseData') }}</div>
        <RouterLink class="side-item child" to="/inventory/products" active-class="active">
          {{ t('nav.products') }}
        </RouterLink>
        <div class="side-item child muted">供应商管理</div>
        <div class="side-item child muted">仓库管理</div>
        <div class="side-section">
          <ShoppingCart :size="16" />
          <span>{{ t('nav.purchase') }}</span>
        </div>
        <div class="side-section">
          <BadgeDollarSign :size="16" />
          <span>{{ t('nav.sales') }}</span>
        </div>
        <div class="side-section">
          <CircleDollarSign :size="16" />
          <span>{{ t('nav.finance') }}</span>
        </div>
        <RouterLink class="side-item" to="/components" active-class="active">
          <Blocks :size="16" />
          <span>{{ t('nav.components') }}</span>
        </RouterLink>
      </nav>
    </aside>

    <section class="app-main">
      <header class="app-topbar">
        <div class="breadcrumb">{{ breadcrumb }}</div>
        <div class="topbar-actions">
          <span class="tenant-pill">{{ t('shell.tenant') }} {{ authStore.tenantId || 'default' }}</span>
          <LanguageSwitcher />
          <ThemeSwitcher />
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

      <div class="tab-strip">
        <RouterLink class="page-tab" to="/dashboard">{{ t('nav.dashboard') }}</RouterLink>
        <RouterLink class="page-tab active" :to="route.path">{{ currentTitle }}</RouterLink>
      </div>

      <main class="app-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {
  BadgeDollarSign,
  Blocks,
  CircleDollarSign,
  LayoutDashboard,
  PackageOpen,
  Search,
  ShoppingCart,
} from 'lucide-vue-next';
import AppLogo from '@/components/app/AppLogo.vue';
import LanguageSwitcher from '@/components/app/LanguageSwitcher.vue';
import ThemeSwitcher from '@/components/app/ThemeSwitcher.vue';
import { useAuthStore } from '@/stores/auth';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const currentTitle = computed(() => String(route.meta.title || t('nav.dashboard')));
const breadcrumb = computed(() => {
  if (route.name === 'products') {
    return t('shell.productPath');
  }
  if (route.name === 'components') {
    return t('shell.componentPath');
  }
  return t('shell.dashboardPath');
});

function handleUserCommand(command: string) {
  if (command === 'logout') {
    authStore.logout();
    router.push('/login');
  }
}
</script>
