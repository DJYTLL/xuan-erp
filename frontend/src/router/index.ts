import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const AppLayout = () => import('@/layouts/AppLayout.vue');
const LoginView = () => import('@/views/LoginView.vue');
const DashboardView = () => import('@/views/DashboardView.vue');
const ComponentCenterView = () => import('@/views/ComponentCenterView.vue');
const ProductManagementView = () => import('@/views/ProductManagementView.vue');
const PurchaseOrderView = () => import('@/views/PurchaseOrderView.vue');
const ForbiddenView = () => import('@/views/ForbiddenView.vue');
const NotFoundView = () => import('@/views/NotFoundView.vue');

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: {
          title: '仪表盘',
          breadcrumb: [{ title: '首页', path: '/dashboard' }, { title: '仪表盘' }],
          keepAlive: true,
          cacheName: 'DashboardView',
          affixTab: true,
          closable: false,
        },
      },
      {
        path: 'components',
        name: 'components',
        component: ComponentCenterView,
        meta: {
          title: '组件管理',
          breadcrumb: [{ title: '系统设置' }, { title: '组件管理', path: '/components' }],
          keepAlive: true,
          cacheName: 'ComponentCenterView',
          affixTab: false,
          closable: true,
        },
      },
      {
        path: 'inventory/products',
        name: 'products',
        component: ProductManagementView,
        meta: {
          title: '商品管理',
          breadcrumb: [{ title: '进销存' }, { title: '基础资料' }, { title: '商品管理', path: '/inventory/products' }],
          keepAlive: true,
          cacheName: 'ProductManagementView',
          affixTab: false,
          closable: true,
          permission: 'product:view',
        },
      },
      {
        path: 'purchase/orders/draft',
        name: 'purchase-order-draft',
        component: PurchaseOrderView,
        meta: {
          title: '采购单（草稿）',
          breadcrumb: [{ title: '进销存' }, { title: '采购管理' }, { title: '采购单（草稿）', path: '/purchase/orders/draft' }],
          keepAlive: true,
          cacheName: 'PurchaseOrderView',
          affixTab: false,
          closable: true,
          purchaseMode: 'draft',
          permission: 'purchase:order:view',
        },
      },
      {
        path: 'purchase/orders/approved',
        name: 'purchase-order-approved',
        component: PurchaseOrderView,
        meta: {
          title: '采购单（已审核）',
          breadcrumb: [{ title: '进销存' }, { title: '采购管理' }, { title: '采购单（已审核）', path: '/purchase/orders/approved' }],
          keepAlive: true,
          cacheName: 'PurchaseOrderView',
          affixTab: false,
          closable: true,
          purchaseMode: 'approved',
          permission: 'purchase:order:view',
        },
      },
      {
        path: 'purchase/returns/draft',
        name: 'purchase-return-draft',
        component: PurchaseOrderView,
        meta: {
          title: '采购退货（草稿）',
          breadcrumb: [{ title: '进销存' }, { title: '采购管理' }, { title: '采购退货（草稿）', path: '/purchase/returns/draft' }],
          keepAlive: true,
          cacheName: 'PurchaseOrderView',
          affixTab: false,
          closable: true,
          purchaseMode: 'returnDraft',
          permission: 'purchase:return:view',
        },
      },
      {
        path: '403',
        name: 'forbidden',
        component: ForbiddenView,
        meta: {
          title: '无权访问',
          breadcrumb: [{ title: '系统' }, { title: '无权访问' }],
          keepAlive: false,
          affixTab: false,
          closable: true,
        },
      },
      {
        path: 'purchase/returns/approved',
        name: 'purchase-return-approved',
        component: PurchaseOrderView,
        meta: {
          title: '采购退货（已审核）',
          breadcrumb: [{ title: '进销存' }, { title: '采购管理' }, { title: '采购退货（已审核）', path: '/purchase/returns/approved' }],
          keepAlive: true,
          cacheName: 'PurchaseOrderView',
          affixTab: false,
          closable: true,
          purchaseMode: 'returnApproved',
          permission: 'purchase:return:view',
        },
      },
      {
        path: ':catchAll(.*)*',
        name: 'catchAll',
        component: NotFoundView,
        meta: {
          title: '页面不存在',
          breadcrumb: [{ title: '系统' }, { title: '页面不存在' }],
          keepAlive: false,
          affixTab: false,
          closable: true,
        },
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
  if (!to.meta.public && to.name !== 'forbidden' && !auth.hasPermission(to.meta.permission as string | string[] | undefined)) {
    return { name: 'forbidden' };
  }
  return true;
});
