import { defineStore } from 'pinia';
import { getCurrentUser, login, logout, refreshToken } from '@/api/auth';
import { setBearerToken } from '@/api/http';
import { appFrameworkConfig } from '@/app/frameworkConfig';
import {
  clearStoredAuthSession,
  getStoredAuthToken,
  getStoredAuthTokenExpiresAt,
  getStoredRefreshToken,
  getStoredRefreshTokenExpiresAt,
  setStoredAuthSession,
} from '@/framework/auth/tokenStorage';
import { useAuthorizationStore } from '@/stores/authorization';
import type { CurrentUser, LoginRequest, LoginResponse } from '@/types/auth';

interface AuthState {
  token: string | null;
  tokenExpiresAt: string | null;
  refreshToken: string | null;
  refreshTokenExpiresAt: string | null;
  currentUser: CurrentUser | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getStoredAuthToken(appFrameworkConfig),
    tokenExpiresAt: getStoredAuthTokenExpiresAt(appFrameworkConfig),
    refreshToken: getStoredRefreshToken(appFrameworkConfig),
    refreshTokenExpiresAt: getStoredRefreshTokenExpiresAt(appFrameworkConfig),
    currentUser: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    username: (state) => state.currentUser?.username || appFrameworkConfig.shell.defaultAvatarText,
    tenantId: (state) => state.currentUser?.tenantId ?? 0,
    tenantCode: (state) => state.currentUser?.tenantCode?.trim() || (state.currentUser?.tenantId === 0 ? 'platform' : ''),
    tenantName: (state) => state.currentUser?.tenantName?.trim() || '',
    tenantDisplayLabel: (state) => {
      const tenantId = state.currentUser?.tenantId ?? 0;
      const tenantCode = state.currentUser?.tenantCode?.trim() || (tenantId === 0 ? 'platform' : '');
      const tenantName = state.currentUser?.tenantName?.trim() || (tenantId === 0 ? '平台租户' : '');
      if (tenantName && tenantCode) {
        return `${tenantName}（${tenantCode}）`;
      }
      if (tenantName) {
        return tenantName;
      }
      if (tenantCode) {
        return tenantCode;
      }
      return tenantId > 0 ? `租户 ${tenantId}` : '平台租户';
    },
    hasPermission: (state) => (permission?: string | string[]) => {
      if (!permission) {
        return true;
      }
      const permissions = state.currentUser?.permissions || [];
      if (permissions.includes('*') || permissions.includes('admin:*')) {
        return true;
      }
      const required = Array.isArray(permission) ? permission : [permission];
      return required.every((item) => permissions.includes(item));
    },
  },
  actions: {
    async login(request: LoginRequest) {
      const response = await login(request);
      this.applySession(response);
    },
    applySession(response: LoginResponse) {
      this.token = response.accessToken;
      this.tokenExpiresAt = response.accessTokenExpiresAt;
      this.refreshToken = response.refreshToken;
      this.refreshTokenExpiresAt = response.refreshTokenExpiresAt;
      this.currentUser = response.currentUser;
      setStoredAuthSession(appFrameworkConfig, response);
      setBearerToken(response.accessToken);
    },
    async refreshSession() {
      const storedRefreshToken = getStoredRefreshToken(appFrameworkConfig) || this.refreshToken;
      if (!storedRefreshToken) {
        throw new Error('缺少 refresh token');
      }
      const response = await refreshToken(storedRefreshToken);
      this.applySession(response);
      return response;
    },
    async loadCurrentUser() {
      const storedToken = getStoredAuthToken(appFrameworkConfig) || this.token;
      if (!storedToken) {
        return;
      }
      this.token = storedToken;
      this.tokenExpiresAt = getStoredAuthTokenExpiresAt(appFrameworkConfig);
      this.refreshToken = getStoredRefreshToken(appFrameworkConfig);
      this.refreshTokenExpiresAt = getStoredRefreshTokenExpiresAt(appFrameworkConfig);
      setBearerToken(storedToken);
      this.currentUser = await getCurrentUser();
    },
    async logout() {
      const authorizationStore = useAuthorizationStore();
      const tokenToRevoke = getStoredRefreshToken(appFrameworkConfig) || this.refreshToken;
      try {
        if (tokenToRevoke) {
          await logout(tokenToRevoke);
        }
      } catch {
        // 退出不能被网络错误卡住，本地登录态仍要清理。
      } finally {
        this.token = null;
        this.tokenExpiresAt = null;
        this.refreshToken = null;
        this.refreshTokenExpiresAt = null;
        this.currentUser = null;
        clearStoredAuthSession(appFrameworkConfig);
        setBearerToken(null);
        authorizationStore.clear();
      }
    },
  },
});
