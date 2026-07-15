export interface LoginRequest {
  tenantId: number;
  username: string;
  password: string;
}

export interface CurrentUser {
  userId: number;
  tenantId: number;
  username: string;
  roles: string[];
  authVersion: number;
  permissions: string[];
}

export interface LoginResponse {
  tokenType: string;
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  currentUser: CurrentUser;
}

export interface CurrentMenuNode {
  code: string;
  title: string;
  i18nKey: string | null;
  path: string | null;
  icon: string | null;
  permissionCode: string | null;
  sortNo: number;
  children: CurrentMenuNode[];
}

export interface CurrentPermissionSnapshot {
  menus: CurrentMenuNode[];
  routePermissions: string[];
  buttonPermissions: string[];
  columnPermissions: Record<string, string[]>;
  fieldPermissions: Record<string, string[]>;
  dataScopes: string[];
  stateActionRules: Record<string, string[]>;
  authVersion: number;
}

export interface ApiResponse<T> {
  code?: string | number;
  message?: string;
  data?: T;
}
