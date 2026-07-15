import { defineAsyncComponent, defineComponent, h, type Component } from 'vue';
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { appFrameworkConfig } from '@/app/frameworkConfig';
import { useAuthStore } from '@/stores/auth';
import { useAuthorizationStore } from '@/stores/authorization';

type RouteComponentLoader = () => Promise<{ default: Component }>;

function createRouteCacheComponent(name: string, loader: RouteComponentLoader) {
  const AsyncView = defineAsyncComponent(async () => (await loader()).default);
  return defineComponent({
    name,
    setup() {
      return () => h(AsyncView);
    },
  });
}

const AppLayout = () => import('@/layouts/AppLayout.vue');
const LoginView = () => import('@/views/LoginView.vue');
const DashboardView = createRouteCacheComponent('DashboardView', () => import('@/views/DashboardView.vue'));
const ComponentCenterView = createRouteCacheComponent('ComponentCenterView', () => import('@/views/ComponentCenterView.vue'));
const IamMenuManagementView = createRouteCacheComponent('IamMenuManagementView', () => import('@/views/IamMenuManagementView.vue'));
const IamPermissionManagementView = createRouteCacheComponent('IamPermissionManagementView', () => import('@/views/IamPermissionManagementView.vue'));
const IamRoleManagementView = createRouteCacheComponent('IamRoleManagementView', () => import('@/views/IamRoleManagementView.vue'));
const IamUserManagementView = createRouteCacheComponent('IamUserManagementView', () => import('@/views/IamUserManagementView.vue'));
const IamTenantInitTemplateManagementView = createRouteCacheComponent('IamTenantInitTemplateManagementView', () => import('@/views/IamTenantInitTemplateManagementView.vue'));
const TenantManagementView = createRouteCacheComponent('TenantManagementView', () => import('@/views/TenantManagementView.vue'));
const TenantPlanManagementView = createRouteCacheComponent('TenantPlanManagementView', () => import('@/views/TenantPlanManagementView.vue'));
const AuditLogQueryView = createRouteCacheComponent('AuditLogQueryView', () => import('@/views/AuditLogQueryView.vue'));
const InterfaceCostView = createRouteCacheComponent('InterfaceCostView', () => import('@/views/InterfaceCostView.vue'));
const SqlRankingView = createRouteCacheComponent('SqlRankingView', () => import('@/views/SqlRankingView.vue'));
const ProductManagementView = createRouteCacheComponent('ProductManagementView', () => import('@/views/ProductManagementView.vue'));
const PurchaseOrderCreateView = createRouteCacheComponent('PurchaseOrderCreateView', () => import('@/views/PurchaseOrderCreateView.vue'));
const PurchaseOrderDraftView = createRouteCacheComponent('PurchaseOrderDraftView', () => import('@/views/PurchaseOrderView.vue'));
const PurchaseOrderApprovedView = createRouteCacheComponent('PurchaseOrderApprovedView', () => import('@/views/PurchaseOrderView.vue'));
const PurchaseReturnDraftView = createRouteCacheComponent('PurchaseReturnDraftView', () => import('@/views/PurchaseOrderView.vue'));
const PurchaseReturnApprovedView = createRouteCacheComponent('PurchaseReturnApprovedView', () => import('@/views/PurchaseOrderView.vue'));
const ForbiddenView = () => import('@/views/ForbiddenView.vue');
const NotFoundView = () => import('@/views/NotFoundView.vue');

const routes: RouteRecordRaw[] = [
  {
    path: appFrameworkConfig.routes.loginPath,
    name: appFrameworkConfig.routes.loginRouteName,
    component: LoginView,
    meta: { public: true },
  },
  {
    path: '/',
    component: AppLayout,
    redirect: appFrameworkConfig.routes.homePath,
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: {
          title: '仪表盘',
          titleKey: 'route.dashboard',
          breadcrumb: [
            { titleKey: 'route.home', title: '首页', path: appFrameworkConfig.routes.homePath },
            { titleKey: 'route.dashboard', title: '仪表盘' },
          ],
          keepAlive: true,
          cacheName: 'DashboardView',
          affixTab: true,
          closable: false,
        },
      },
      {
        path: 'workbench',
        redirect: appFrameworkConfig.routes.homePath,
        meta: { title: '工作台', titleKey: 'route.workbench' },
      },
      {
        path: 'components',
        name: 'components',
        component: ComponentCenterView,
        meta: {
          title: '组件管理',
          titleKey: 'route.components',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.components', title: '组件管理', path: '/components' }],
          keepAlive: true,
          cacheName: 'ComponentCenterView',
          affixTab: false,
          closable: true,
          permission: 'iam:view',
        },
      },
      {
        path: 'system',
        redirect: '/components',
        meta: { title: '系统', titleKey: 'route.system' },
      },
      {
        path: 'system/iam/menus',
        name: 'iam-menu-management',
        component: IamMenuManagementView,
        meta: {
          title: '菜单管理',
          titleKey: 'route.iamMenus',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.iamMenus', title: '菜单管理', path: '/system/iam/menus' }],
          keepAlive: true,
          cacheName: 'IamMenuManagementView',
          affixTab: false,
          closable: true,
          permission: 'iam:view',
        },
      },
      {
        path: 'system/iam/permissions',
        name: 'iam-permission-management',
        component: IamPermissionManagementView,
        meta: {
          title: '权限管理',
          titleKey: 'route.iamPermissions',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.iamPermissions', title: '权限管理', path: '/system/iam/permissions' }],
          keepAlive: true,
          cacheName: 'IamPermissionManagementView',
          affixTab: false,
          closable: true,
          permission: 'iam:view',
        },
      },
      {
        path: 'system/iam/roles',
        name: 'iam-role-management',
        component: IamRoleManagementView,
        meta: {
          title: '角色授权',
          titleKey: 'route.iamRoles',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.iamRoles', title: '角色授权', path: '/system/iam/roles' }],
          keepAlive: true,
          cacheName: 'IamRoleManagementView',
          affixTab: false,
          closable: true,
          permission: 'iam:view',
        },
      },
      {
        path: 'system/iam/users',
        name: 'iam-user-management',
        component: IamUserManagementView,
        meta: {
          title: '用户授权',
          titleKey: 'route.iamUsers',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.iamUsers', title: '用户授权', path: '/system/iam/users' }],
          keepAlive: true,
          cacheName: 'IamUserManagementView',
          affixTab: false,
          closable: true,
          permission: 'iam:view',
        },
      },
      {
        path: 'system/iam/init-templates',
        name: 'iam-init-template-management',
        component: IamTenantInitTemplateManagementView,
        meta: {
          title: '初始化模板',
          titleKey: 'route.iamInitTemplates',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.iamInitTemplates', title: '初始化模板', path: '/system/iam/init-templates' }],
          keepAlive: true,
          cacheName: 'IamTenantInitTemplateManagementView',
          affixTab: false,
          closable: true,
          permission: 'iam:view',
        },
      },
      {
        path: 'system/tenants',
        name: 'tenant-management',
        component: TenantManagementView,
        meta: {
          title: '租户管理',
          titleKey: 'route.tenants',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.tenants', title: '租户管理', path: '/system/tenants' }],
          keepAlive: true,
          cacheName: 'TenantManagementView',
          affixTab: false,
          closable: true,
          permission: 'tenant:view',
        },
      },
      {
        path: 'system/tenant-plans',
        name: 'tenant-plan-management',
        component: TenantPlanManagementView,
        meta: {
          title: '套餐管理',
          titleKey: 'route.tenantPlans',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.tenantPlans', title: '套餐管理', path: '/system/tenant-plans' }],
          keepAlive: true,
          cacheName: 'TenantPlanManagementView',
          affixTab: false,
          closable: true,
          permission: 'tenant-plan:view',
        },
      },
      {
        path: 'system/audit/logs',
        name: 'audit-logs',
        component: AuditLogQueryView,
        meta: {
          title: '审计日志',
          titleKey: 'route.auditLogs',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.auditLogs', title: '审计日志', path: '/system/audit/logs' }],
          keepAlive: true,
          cacheName: 'AuditLogQueryView',
          affixTab: false,
          closable: true,
          permission: 'audit:log:view',
        },
      },
      {
        path: 'system/audit/interface-costs',
        name: 'audit-interface-costs',
        component: InterfaceCostView,
        meta: {
          title: '接口耗时',
          titleKey: 'route.auditInterfaceCosts',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.auditInterfaceCosts', title: '接口耗时', path: '/system/audit/interface-costs' }],
          keepAlive: true,
          cacheName: 'InterfaceCostView',
          affixTab: false,
          closable: true,
          permission: 'audit:interface-cost:view',
        },
      },
      {
        path: 'system/audit/sql-rankings',
        name: 'audit-sql-rankings',
        component: SqlRankingView,
        meta: {
          title: 'SQL 排名',
          titleKey: 'route.auditSqlRankings',
          breadcrumb: [{ titleKey: 'route.system', title: '系统设置' }, { titleKey: 'route.auditSqlRankings', title: 'SQL 排名', path: '/system/audit/sql-rankings' }],
          keepAlive: true,
          cacheName: 'SqlRankingView',
          affixTab: false,
          closable: true,
          permission: 'audit:sql-ranking:view',
        },
      },
      {
        path: 'product',
        redirect: '/inventory/products',
        meta: { title: '商品', titleKey: 'route.product' },
      },
      {
        path: 'inventory/products',
        name: 'products',
        component: ProductManagementView,
        meta: {
          title: '商品管理',
          titleKey: 'route.inventoryProducts',
          breadcrumb: [{ titleKey: 'nav.inventory', title: '进销存' }, { titleKey: 'nav.baseData', title: '基础资料' }, { titleKey: 'route.inventoryProducts', title: '商品管理', path: '/inventory/products' }],
          keepAlive: true,
          cacheName: 'ProductManagementView',
          affixTab: false,
          closable: true,
          permission: 'product:view',
        },
      },
      {
        path: 'procurement',
        redirect: '/purchase/orders/draft',
        meta: { title: '采购', titleKey: 'route.procurement' },
      },
      {
        path: 'purchase/orders/draft',
        name: 'purchase-order-draft',
        component: PurchaseOrderDraftView,
        meta: {
          title: '采购单（草稿）',
          titleKey: 'route.purchaseOrderDraft',
          breadcrumb: [{ titleKey: 'nav.inventory', title: '进销存' }, { titleKey: 'nav.purchase', title: '采购管理' }, { titleKey: 'route.purchaseOrderDraft', title: '采购单（草稿）', path: '/purchase/orders/draft' }],
          keepAlive: true,
          cacheName: 'PurchaseOrderDraftView',
          affixTab: false,
          closable: true,
          purchaseMode: 'draft',
          permission: 'procurement:view',
        },
      },
      {
        path: 'purchase/orders/create',
        name: 'purchase-order-create',
        component: PurchaseOrderCreateView,
        meta: {
          title: '新增采购单',
          titleKey: 'route.purchaseOrderCreate',
          breadcrumb: [{ titleKey: 'nav.inventory', title: '进销存' }, { titleKey: 'nav.purchase', title: '采购管理' }, { titleKey: 'route.purchaseOrderCreate', title: '新增采购单', path: '/purchase/orders/create' }],
          keepAlive: true,
          cacheName: 'PurchaseOrderCreateView',
          affixTab: false,
          closable: true,
          permission: 'procurement:create',
        },
      },
      {
        path: 'purchase/orders/approved',
        name: 'purchase-order-approved',
        component: PurchaseOrderApprovedView,
        meta: {
          title: '采购单（已审核）',
          titleKey: 'route.purchaseOrderApproved',
          breadcrumb: [{ titleKey: 'nav.inventory', title: '进销存' }, { titleKey: 'nav.purchase', title: '采购管理' }, { titleKey: 'route.purchaseOrderApproved', title: '采购单（已审核）', path: '/purchase/orders/approved' }],
          keepAlive: true,
          cacheName: 'PurchaseOrderApprovedView',
          affixTab: false,
          closable: true,
          purchaseMode: 'approved',
          permission: 'procurement:view',
        },
      },
      {
        path: 'purchase/returns/draft',
        name: 'purchase-return-draft',
        component: PurchaseReturnDraftView,
        meta: {
          title: '采购退货（草稿）',
          titleKey: 'route.purchaseReturnDraft',
          breadcrumb: [{ titleKey: 'nav.inventory', title: '进销存' }, { titleKey: 'nav.purchase', title: '采购管理' }, { titleKey: 'route.purchaseReturnDraft', title: '采购退货（草稿）', path: '/purchase/returns/draft' }],
          keepAlive: true,
          cacheName: 'PurchaseReturnDraftView',
          affixTab: false,
          closable: true,
          purchaseMode: 'returnDraft',
          permission: 'procurement:view',
        },
      },
      {
        path: '403',
        name: 'forbidden',
        component: ForbiddenView,
        meta: {
          title: '无权访问',
          titleKey: 'route.forbidden',
          breadcrumb: [{ titleKey: 'route.system', title: '系统' }, { titleKey: 'route.forbidden', title: '无权访问' }],
          keepAlive: false,
          affixTab: false,
          closable: true,
        },
      },
      {
        path: 'purchase/returns/approved',
        name: 'purchase-return-approved',
        component: PurchaseReturnApprovedView,
        meta: {
          title: '采购退货（已审核）',
          titleKey: 'route.purchaseReturnApproved',
          breadcrumb: [{ titleKey: 'nav.inventory', title: '进销存' }, { titleKey: 'nav.purchase', title: '采购管理' }, { titleKey: 'route.purchaseReturnApproved', title: '采购退货（已审核）', path: '/purchase/returns/approved' }],
          keepAlive: true,
          cacheName: 'PurchaseReturnApprovedView',
          affixTab: false,
          closable: true,
          purchaseMode: 'returnApproved',
          permission: 'procurement:view',
        },
      },
      {
        path: ':catchAll(.*)*',
        name: 'catchAll',
        component: NotFoundView,
        meta: {
          title: '页面不存在',
          titleKey: 'route.notFound',
          breadcrumb: [{ titleKey: 'route.system', title: '系统' }, { titleKey: 'route.notFound', title: '页面不存在' }],
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
  const authorization = useAuthorizationStore();
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: appFrameworkConfig.routes.loginRouteName, query: { redirect: to.fullPath } };
  }
  if (!to.meta.public && auth.isAuthenticated && !auth.currentUser) {
    try {
      await auth.loadCurrentUser();
      await authorization.loadPermissionSnapshot();
    } catch {
      void auth.logout();
      return { name: appFrameworkConfig.routes.loginRouteName, query: { redirect: to.fullPath } };
    }
  }
  if (!to.meta.public && auth.isAuthenticated && !authorization.isLoaded) {
    try {
      await authorization.loadPermissionSnapshot();
    } catch {
      void auth.logout();
      return { name: appFrameworkConfig.routes.loginRouteName, query: { redirect: to.fullPath } };
    }
  }
  if (to.name === appFrameworkConfig.routes.loginRouteName && auth.isAuthenticated) {
    return { name: appFrameworkConfig.routes.homeRouteName };
  }
  if (
    !to.meta.public
    && to.name !== 'forbidden'
    && !authorization.hasRoutePermission(to.meta.permission as string | string[] | undefined)
  ) {
    return { name: 'forbidden' };
  }
  return true;
});
