import { http } from './http';
import type { ApiResponse, CurrentUser, LoginRequest, LoginResponse } from '@/types/auth';

function unwrap<T>(body: ApiResponse<T> | T): T {
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as ApiResponse<T>).data as T;
  }
  return body as T;
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await http.post<ApiResponse<LoginResponse> | LoginResponse>('/api/iam/auth/login', request);
  return unwrap<LoginResponse>(response.data);
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await http.get<ApiResponse<CurrentUser> | CurrentUser>('/api/iam/auth/current-user');
  return unwrap<CurrentUser>(response.data);
}
