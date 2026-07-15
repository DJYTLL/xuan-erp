import { http } from './http';
import type { ApiResponse } from '@/types/auth';

export type UserPreferenceResponse<TPreference extends object> = {
  preferenceKey: string;
  value: TPreference;
  updatedAt?: string | null;
};

function unwrap<T>(body: ApiResponse<T> | T): T {
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as ApiResponse<T>).data as T;
  }
  return body as T;
}

function hasPreferenceValue(value: object | null | undefined) {
  return Boolean(value && Object.keys(value).length > 0);
}

export async function getUserPreference<TPreference extends object>(
  preferenceKey: string,
): Promise<TPreference | null> {
  const response = await http.get<ApiResponse<UserPreferenceResponse<TPreference>> | UserPreferenceResponse<TPreference>>(
    `/api/iam/user-preferences/${encodeURIComponent(preferenceKey)}`,
  );
  const body = unwrap<UserPreferenceResponse<TPreference>>(response.data);
  return hasPreferenceValue(body.value) ? body.value : null;
}

export async function saveUserPreference<TPreference extends object>(
  preferenceKey: string,
  value: TPreference,
): Promise<TPreference> {
  const response = await http.put<ApiResponse<UserPreferenceResponse<TPreference>> | UserPreferenceResponse<TPreference>>(
    `/api/iam/user-preferences/${encodeURIComponent(preferenceKey)}`,
    { value },
  );
  return unwrap<UserPreferenceResponse<TPreference>>(response.data).value;
}
