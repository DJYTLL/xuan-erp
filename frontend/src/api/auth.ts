import { http } from './http';
import { appFrameworkConfig } from '@/app/frameworkConfig';
import type {
  ApiResponse,
  CurrentMenuNode,
  CurrentPermissionSnapshot,
  CurrentUser,
  LoginRequest,
  LoginResponse,
} from '@/types/auth';

function unwrap<T>(body: ApiResponse<T> | T): T {
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as ApiResponse<T>).data as T;
  }
  return body as T;
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await http.post<ApiResponse<LoginResponse> | LoginResponse>(
    appFrameworkConfig.auth.loginPath,
    request,
  );
  return unwrap<LoginResponse>(response.data);
}

export async function refreshToken(refreshToken: string): Promise<LoginResponse> {
  const response = await http.post<ApiResponse<LoginResponse> | LoginResponse>(
    appFrameworkConfig.auth.refreshTokenPath,
    { refreshToken },
  );
  return unwrap<LoginResponse>(response.data);
}

export async function logout(refreshToken: string): Promise<void> {
  await http.post<ApiResponse<void> | void>(
    appFrameworkConfig.auth.logoutPath,
    { refreshToken },
  );
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await http.get<ApiResponse<CurrentUser> | CurrentUser>(appFrameworkConfig.auth.currentUserPath);
  return unwrap<CurrentUser>(response.data);
}

export async function getCurrentPermissionSnapshot(): Promise<CurrentPermissionSnapshot> {
  const response = await http.get<ApiResponse<CurrentPermissionSnapshot> | CurrentPermissionSnapshot>(
    appFrameworkConfig.auth.permissionSnapshotPath,
  );
  return unwrap<CurrentPermissionSnapshot>(response.data);
}

export async function getCurrentMenus(): Promise<CurrentMenuNode[]> {
  const response = await http.get<ApiResponse<CurrentMenuNode[]> | CurrentMenuNode[]>(
    appFrameworkConfig.auth.currentMenusPath,
  );
  return unwrap<CurrentMenuNode[]>(response.data);
}
