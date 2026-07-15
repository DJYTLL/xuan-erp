import type { FrameworkAppConfig, FrameworkAppConfigInput } from './types';

export const defaultFrameworkConfig: FrameworkAppConfig = {
  shell: {
    appName: 'Application',
    footerText: 'Application',
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
    tokenKey: 'app-token',
    tokenExpiresAtKey: 'app-token-expires-at',
    refreshTokenKey: 'app-refresh-token',
    refreshTokenExpiresAtKey: 'app-refresh-token-expires-at',
    localeKey: 'app-locale',
    openTabsKeyPrefix: 'app-open-tabs-v1',
    settingKeyPrefix: 'app',
  },
  preferences: {
    shellLayoutKey: 'shell.layout',
    shellTabsKey: 'shell.tabs',
  },
  auth: {
    loginPath: '/api/auth/login',
    refreshTokenPath: '/api/auth/refresh',
    logoutPath: '/api/auth/logout',
    currentUserPath: '/api/auth/current-user',
    permissionSnapshotPath: '/api/auth/permissions/current',
    currentMenusPath: '/api/auth/menus/current',
    currentUserProbePath: '/api/auth/current-user',
    authProbePaths: [
      '/api/auth/current-user',
      '/api/auth/permissions/current',
      '/api/auth/menus/current',
    ],
    authExpiredEventName: 'app-auth-expired',
    httpErrorEventName: 'app-http-error',
  },
};

export function defineFrameworkConfig(input: FrameworkAppConfigInput): FrameworkAppConfig {
  return {
    shell: { ...defaultFrameworkConfig.shell, ...input.shell },
    routes: { ...defaultFrameworkConfig.routes, ...input.routes },
    storage: { ...defaultFrameworkConfig.storage, ...input.storage },
    preferences: { ...defaultFrameworkConfig.preferences, ...input.preferences },
    auth: {
      ...defaultFrameworkConfig.auth,
      ...input.auth,
      authProbePaths: input.auth?.authProbePaths || defaultFrameworkConfig.auth.authProbePaths,
    },
  };
}
