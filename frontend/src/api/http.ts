import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { handleHttpError } from './http-error';
import { getGatewayApiBaseUrl } from './gateway-base';
import { appFrameworkConfig } from '@/app/frameworkConfig';
import {
  clearStoredAuthSession,
  getStoredAuthToken,
  getStoredRefreshToken,
  getStoredLocale,
  setStoredAuthSession,
} from '@/framework/auth/tokenStorage';
import { emitAuthSessionRefreshed } from '@/framework/auth/sessionEvents';
import type { ApiResponse, LoginResponse } from '@/types/auth';

type RefreshRetryConfig = InternalAxiosRequestConfig & {
  _retryAfterRefresh?: boolean;
};

type RefreshRetryReason = 'unauthorized' | 'forbidden-permission';

let pendingRefreshRequest: Promise<LoginResponse> | null = null;

export const http = axios.create({
  baseURL: getGatewayApiBaseUrl(),
  timeout: 15000,
});

export function setBearerToken(token: string | null) {
  if (token) {
    http.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete http.defaults.headers.common.Authorization;
  }
}

export function getCurrentLocale() {
  return getStoredLocale(appFrameworkConfig);
}

http.interceptors.request.use((config) => {
  const token = getStoredAuthToken(appFrameworkConfig);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.headers['Accept-Language'] = getCurrentLocale();
  return config;
});

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const refreshReason = getRefreshRetryReason(error);
    if (refreshReason) {
      try {
        const refreshed = await refreshAccessToken();
        return retryOriginalRequest(error, refreshed.accessToken);
      } catch {
        if (refreshReason === 'unauthorized') {
          clearStoredAuthSession(appFrameworkConfig);
          setBearerToken(null);
        }
      }
    }
    await handleHttpError(error);
    return Promise.reject(error);
  },
);

function shouldAttemptRefresh(error: unknown) {
  return Boolean(getRefreshRetryReason(error));
}

function getRefreshRetryReason(error: unknown): RefreshRetryReason | null {
  const axiosError = error as AxiosError;
  const status = axiosError.response?.status || null;
  const config = axiosError.config as RefreshRetryConfig | undefined;
  if (!config || config._retryAfterRefresh) {
    return null;
  }
  const requestPath = normalizeRequestPath(config.url);
  if (requestPath.endsWith(appFrameworkConfig.auth.loginPath)
    || requestPath.endsWith(appFrameworkConfig.auth.refreshTokenPath)
    || requestPath.endsWith(appFrameworkConfig.auth.logoutPath)) {
    return null;
  }
  if (!getStoredRefreshToken(appFrameworkConfig)) {
    return null;
  }
  if (status === 401) {
    return 'unauthorized';
  }
  if (status === 403 && getResponseErrorCode(error) === 'SECURITY_PERMISSION_DENIED') {
    return 'forbidden-permission';
  }
  return null;
}

async function refreshAccessToken() {
  if (!pendingRefreshRequest) {
    pendingRefreshRequest = requestRefreshToken().finally(() => {
      pendingRefreshRequest = null;
    });
  }
  return pendingRefreshRequest;
}

async function requestRefreshToken() {
  const refreshToken = getStoredRefreshToken(appFrameworkConfig);
  if (!refreshToken) {
    throw new Error('缺少 refresh token');
  }
  const response = await axios.post<ApiResponse<LoginResponse> | LoginResponse>(
    appFrameworkConfig.auth.refreshTokenPath,
    { refreshToken },
    {
      baseURL: getGatewayApiBaseUrl(),
      timeout: 15000,
      headers: {
        'Accept-Language': getCurrentLocale(),
      },
    },
  );
  const refreshed = unwrap<LoginResponse>(response.data);
  setStoredAuthSession(appFrameworkConfig, refreshed);
  setBearerToken(refreshed.accessToken);
  emitAuthSessionRefreshed(refreshed);
  return refreshed;
}

function retryOriginalRequest(error: unknown, accessToken: string) {
  const config = (error as AxiosError).config as RefreshRetryConfig;
  config._retryAfterRefresh = true;
  config.headers.Authorization = `Bearer ${accessToken}`;
  return http(config);
}

function unwrap<T>(body: ApiResponse<T> | T): T {
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as ApiResponse<T>).data as T;
  }
  return body as T;
}

function normalizeRequestPath(url?: string) {
  if (!url) {
    return '';
  }
  try {
    return new URL(url, window.location.origin).pathname;
  } catch {
    return url.split('?')[0] || '';
  }
}

function getResponseErrorCode(error: unknown) {
  const body = (error as AxiosError<ApiResponse<unknown>>).response?.data;
  if (!body || typeof body !== 'object' || !('code' in body)) {
    return '';
  }
  const code = body.code;
  return code === null || code === undefined ? '' : String(code);
}
