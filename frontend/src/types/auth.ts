export interface LoginRequest {
  tenantCode: string;
  username: string;
  password: string;
}

export interface CurrentUser {
  userId: number;
  tenantId: number;
  tenantCode: string | null;
  tenantName: string | null;
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

export type ColumnPermissionAccessMode = 'VISIBLE' | 'MASKED' | 'HIDDEN';

export interface CurrentPermissionSnapshot {
  menus: CurrentMenuNode[];
  routePermissions: string[];
  buttonPermissions: string[];
  columnPermissions: Record<string, Record<string, ColumnPermissionAccessMode>>;
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
