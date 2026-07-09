import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import AppLayout from '@/layouts/AppLayout.vue';
import LoginView from '@/views/LoginView.vue';
import DashboardView from '@/views/DashboardView.vue';
import ComponentCenterView from '@/views/ComponentCenterView.vue';
import ProductManagementView from '@/views/ProductManagementView.vue';

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '仪表盘' } },
      { path: 'components', name: 'components', component: ComponentCenterView, meta: { title: '组件管理' } },
      {
        path: 'inventory/products',
        name: 'products',
        component: ProductManagementView,
        meta: { title: '商品管理', permission: 'product:view' },
      },
    ],
  },
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (!to.meta.public && auth.isAuthenticated && !auth.currentUser) {
    try {
      await auth.loadCurrentUser();
    } catch {
      auth.logout();
      return { name: 'login', query: { redirect: to.fullPath } };
    }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'dashboard' };
  }
  return true;
});
