import axios, { type AxiosError } from 'axios';
import { ElMessage } from 'element-plus/es/components/message/index';
import { getGatewayApiBaseUrl } from './gateway-base';
import { appFrameworkConfig } from '@/app/frameworkConfig';
import { clearStoredAuthSession, getStoredAuthToken, getStoredLocale } from '@/framework/auth/tokenStorage';

type HttpErrorHandlerOptions = {
  onUnauthorized?: () => void;
  onForbidden?: () => void;
  translateErrorCode?: (code: string) => string;
};

type HttpErrorDetail = {
  status: number | null;
  message: string;
};

type HttpErrorBody = string | {
  code?: string | number;
  message?: unknown;
  msg?: unknown;
  error?: unknown;
  detail?: unknown;
  reason?: unknown;
  data?: unknown;
};

let handlerOptions: HttpErrorHandlerOptions = {};
let lastMessage = '';
let lastMessageAt = 0;
let pendingUnauthorizedProbe: Promise<boolean> | null = null;
const SESSION_EXPIRED_MESSAGE = '登录已过期，请重新登录';
const BUSINESS_UNAUTHORIZED_MESSAGE = '当前账号无权访问该资源';
const SERVER_MESSAGE_KEYS = ['message', 'msg', 'detail', 'reason', 'error'] as const;
const GENERIC_HTTP_MESSAGES = new Set([
  'bad request',
  'unauthorized',
  'forbidden',
  'not found',
  'internal server error',
  'service unavailable',
]);
const ROLE_COLUMN_PERMISSION_ROUTE = '/system/iam/role-column-permissions';
const ROLE_COLUMN_PERMISSION_DEPENDENCIES: Array<{
  permission: string;
  message: string;
  matches: (path: string) => boolean;
}> = [
  {
    permission: 'iam-role:view',
    message: '缺少角色查询权限',
    matches: (path) => path === '/api/iam/roles',
  },
  {
    permission: 'iam-column-permission:view',
    message: '缺少列权限资源查看权限',
    matches: (path) => path === '/api/iam/column-permissions/resources'
      || path.startsWith('/api/iam/column-permissions/tenants/')
      || /^\/api\/iam\/column-permissions\/templates\/\d+\/items$/.test(path),
  },
  {
    permission: 'iam-role-column-permission:view',
    message: '缺少角色列权限查看权限',
    matches: (path) => path.startsWith('/api/iam/column-permissions/roles/'),
  },
];

const AUTH_PROBE_PATHS = appFrameworkConfig.auth.authProbePaths;

export function installHttpErrorHandler(options: HttpErrorHandlerOptions) {
  handlerOptions = options;
}

export async function handleHttpError(error: unknown) {
  const status = getHttpStatus(error);
  const message = getHttpErrorMessage(error);
  window.dispatchEvent(new CustomEvent<HttpErrorDetail>(appFrameworkConfig.auth.httpErrorEventName, {
    detail: { status, message },
  }));

  if (status === 401) {
    if (await shouldForceLogout(error)) {
      notifyHttpError(message);
      clearStoredAuthSession(appFrameworkConfig);
      window.dispatchEvent(new CustomEvent(appFrameworkConfig.auth.authExpiredEventName));
      handlerOptions.onUnauthorized?.();
    } else {
      notifyHttpError(getBusinessUnauthorizedMessage(message));
    }
    return;
  }

  if (status === 403) {
    const dependencyMessage = resolveRoleColumnPermissionForbiddenMessage(error);
    notifyHttpError(dependencyMessage || message);
    if (!dependencyMessage) {
      handlerOptions.onForbidden?.();
    }
    return;
  }

  notifyHttpError(message);
}

export function getHttpErrorMessage(error: unknown) {
  const axiosError = error as AxiosError<HttpErrorBody>;
  const status = axiosError.response?.status || null;
  const serverMessage = getServerErrorMessage(axiosError.response?.data);
  if (serverMessage && !isGenericHttpMessage(serverMessage)) {
    return serverMessage;
  }
  const errorCode = getHttpErrorCode(error);
  const translatedMessage = errorCode ? handlerOptions.translateErrorCode?.(errorCode) : '';
  if (translatedMessage) {
    return translatedMessage;
  }
  if (status === 401) {
    return SESSION_EXPIRED_MESSAGE;
  }
  if (status === 403) {
    return '当前账号无权访问该资源';
  }
  if (status && status >= 500) {
    return '后端服务异常，请稍后重试';
  }
  if (axiosError.code === 'ECONNABORTED') {
    return '请求超时，请检查网络或服务状态';
  }
  if (!axiosError.response) {
    return '无法连接后端服务，请检查网关或网络';
  }
  if (serverMessage) {
    return serverMessage;
  }
  return '请求失败，请稍后重试';
}

function getBusinessUnauthorizedMessage(message: string) {
  return message === SESSION_EXPIRED_MESSAGE ? BUSINESS_UNAUTHORIZED_MESSAGE : message;
}

export function getHttpErrorCode(error: unknown) {
  const body = (error as AxiosError<HttpErrorBody>).response?.data;
  const code = typeof body === 'object' && body !== null ? body.code : undefined;
  if (code === null || code === undefined || code === '') {
    return '';
  }
  return String(code);
}

export function resolveRoleColumnPermissionForbiddenMessage(error: unknown) {
  if (getHttpStatus(error) !== 403 || !isRoleColumnPermissionPageRequest()) {
    return '';
  }
  const requestPath = normalizeRequestPath((error as AxiosError).config?.url);
  return ROLE_COLUMN_PERMISSION_DEPENDENCIES.find((dependency) => dependency.matches(requestPath))?.message || '';
}

function getHttpStatus(error: unknown) {
  return (error as AxiosError).response?.status || null;
}

function isRoleColumnPermissionPageRequest() {
  return typeof window !== 'undefined' && window.location.pathname === ROLE_COLUMN_PERMISSION_ROUTE;
}

async function shouldForceLogout(error: unknown) {
  const requestPath = normalizeRequestPath((error as AxiosError).config?.url);
  if (isAuthProbeRequest(requestPath)) {
    return true;
  }
  const token = getStoredAuthToken(appFrameworkConfig);
  if (!token) {
    return true;
  }
  if (!pendingUnauthorizedProbe) {
    pendingUnauthorizedProbe = probeCurrentSession(token).finally(() => {
      pendingUnauthorizedProbe = null;
    });
  }
  return pendingUnauthorizedProbe;
}

async function probeCurrentSession(token: string) {
  try {
    const response = await axios.get(appFrameworkConfig.auth.currentUserProbePath, {
      baseURL: getGatewayApiBaseUrl(),
      timeout: 5000,
      headers: {
        Authorization: `Bearer ${token}`,
        'Accept-Language': getStoredLocale(appFrameworkConfig),
      },
      validateStatus: () => true,
    });
    return response.status === 401;
  } catch {
    return false;
  }
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

function isAuthProbeRequest(path: string) {
  return AUTH_PROBE_PATHS.some((item) => path.endsWith(item));
}

function getServerErrorMessage(body: unknown): string {
  if (!body) {
    return '';
  }
  if (typeof body === 'string') {
    return body.trim();
  }
  if (typeof body !== 'object') {
    return '';
  }
  const record = body as Record<string, unknown>;
  for (const key of SERVER_MESSAGE_KEYS) {
    const message = normalizeServerMessage(record[key]);
    if (message) {
      return message;
    }
  }
  return getServerErrorMessage(record.data);
}

function normalizeServerMessage(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function isGenericHttpMessage(message: string) {
  return GENERIC_HTTP_MESSAGES.has(message.trim().toLowerCase());
}

function notifyHttpError(message: string) {
  const now = Date.now();
  if (message === lastMessage && now - lastMessageAt < 1200) {
    return;
  }
  lastMessage = message;
  lastMessageAt = now;
  ElMessage.error(message);
}
