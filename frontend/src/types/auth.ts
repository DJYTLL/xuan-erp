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
  currentUser: CurrentUser;
}

export interface ApiResponse<T> {
  code?: string | number;
  message?: string;
  data?: T;
}
