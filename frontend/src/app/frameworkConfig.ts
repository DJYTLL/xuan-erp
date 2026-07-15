import { defineFrameworkConfig } from '@/framework/config/defaults';

export const appFrameworkConfig = defineFrameworkConfig({
  shell: {
    appName: 'Xuan ERP',
    footerText: 'Xuan ERP Frontend',
    showTenant: true,
    tenantLabelKey: 'shell.tenant',
    defaultAvatarText: 'A',
  },
  routes: {
    loginPath: '/login',
    loginRouteName: 'login',
    homePath: '/dashboard',
    homeRouteName: 'dashboard',
  },
  storage: {
    tokenKey: 'xuan-erp-token',
    tokenExpiresAtKey: 'xuan-erp-token-expires-at',
    refreshTokenKey: 'xuan-erp-refresh-token',
    refreshTokenExpiresAtKey: 'xuan-erp-refresh-token-expires-at',
    localeKey: 'xuan-locale',
    openTabsKeyPrefix: 'xuan-erp-open-tabs-v1',
    settingKeyPrefix: 'xuan',
  },
  preferences: {
    shellLayoutKey: 'shell.layout',
    shellTabsKey: 'shell.tabs',
  },
  auth: {
    loginPath: '/api/iam/auth/login',
    refreshTokenPath: '/api/iam/auth/refresh',
    logoutPath: '/api/iam/auth/logout',
    currentUserPath: '/api/iam/auth/current-user',
    permissionSnapshotPath: '/api/iam/permissions/current',
    currentMenusPath: '/api/iam/menus/current',
    currentUserProbePath: '/api/iam/auth/current-user',
    authProbePaths: [
      '/api/iam/auth/current-user',
      '/api/iam/permissions/current',
      '/api/iam/menus/current',
    ],
    authExpiredEventName: 'xuan-auth-expired',
    httpErrorEventName: 'xuan-http-error',
  },
});
