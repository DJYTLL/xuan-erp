export type FrameworkShellConfig = {
  appName: string;
  footerText: string;
  showTenant: boolean;
  tenantLabelKey: string;
  defaultAvatarText: string;
};

export type FrameworkRouteConfig = {
  loginPath: string;
  loginRouteName: string;
  homePath: string;
  homeRouteName: string;
};

export type FrameworkStorageConfig = {
  tokenKey: string;
  tokenExpiresAtKey: string;
  refreshTokenKey: string;
  refreshTokenExpiresAtKey: string;
  localeKey: string;
  openTabsKeyPrefix: string;
  settingKeyPrefix: string;
};

export type FrameworkPreferenceConfig = {
  shellLayoutKey: string;
  shellTabsKey: string;
};

export type FrameworkAuthConfig = {
  loginPath: string;
  refreshTokenPath: string;
  logoutPath: string;
  currentUserPath: string;
  permissionSnapshotPath: string;
  currentMenusPath: string;
  currentUserProbePath: string;
  authProbePaths: string[];
  authExpiredEventName: string;
  httpErrorEventName: string;
};

export type FrameworkAppConfig = {
  shell: FrameworkShellConfig;
  routes: FrameworkRouteConfig;
  storage: FrameworkStorageConfig;
  preferences: FrameworkPreferenceConfig;
  auth: FrameworkAuthConfig;
};

export type FrameworkAppConfigInput = {
  shell?: Partial<FrameworkShellConfig>;
  routes?: Partial<FrameworkRouteConfig>;
  storage?: Partial<FrameworkStorageConfig>;
  preferences?: Partial<FrameworkPreferenceConfig>;
  auth?: Partial<FrameworkAuthConfig>;
};
