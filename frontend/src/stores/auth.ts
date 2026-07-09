import { defineStore } from 'pinia';
import { getCurrentUser, login } from '@/api/auth';
import { setBearerToken } from '@/api/http';
import type { CurrentUser, LoginRequest } from '@/types/auth';

interface AuthState {
  token: string | null;
  currentUser: CurrentUser | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('xuan-erp-token'),
    currentUser: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    username: (state) => state.currentUser?.username || 'A',
    tenantId: (state) => state.currentUser?.tenantId ?? 0,
  },
  actions: {
    async login(request: LoginRequest) {
      const response = await login(request);
      this.token = response.accessToken;
      this.currentUser = response.currentUser;
      localStorage.setItem('xuan-erp-token', response.accessToken);
      setBearerToken(response.accessToken);
    },
    async loadCurrentUser() {
      if (!this.token) {
        return;
      }
      setBearerToken(this.token);
      this.currentUser = await getCurrentUser();
    },
    logout() {
      this.token = null;
      this.currentUser = null;
      localStorage.removeItem('xuan-erp-token');
      setBearerToken(null);
    },
  },
});
