import { defineStore } from 'pinia';
import { getCurrentMenus, getCurrentPermissionSnapshot } from '@/api/auth';
import type { CurrentMenuNode, CurrentPermissionSnapshot } from '@/types/auth';

interface AuthorizationState {
  snapshot: CurrentPermissionSnapshot | null;
  loading: boolean;
}

function emptySnapshot(): CurrentPermissionSnapshot {
  return {
    menus: [],
    routePermissions: [],
    buttonPermissions: [],
    columnPermissions: {},
    fieldPermissions: {},
    dataScopes: [],
    stateActionRules: {},
    authVersion: 0,
  };
}

function hasAnyPermission(owned: string[], permission?: string | string[]) {
  if (!permission) {
    return true;
  }
  if (owned.includes('*') || owned.includes('admin:*')) {
    return true;
  }
  const required = Array.isArray(permission) ? permission : [permission];
  return required.every((item) => owned.includes(item));
}

function normalizeMenus(menus: CurrentMenuNode[] | undefined): CurrentMenuNode[] {
  return [...(menus || [])]
    .sort((left, right) => left.sortNo - right.sortNo)
    .map((item) => ({
      ...item,
      children: normalizeMenus(item.children),
    }));
}

export const useAuthorizationStore = defineStore('authorization', {
  state: (): AuthorizationState => ({
    snapshot: null,
    loading: false,
  }),
  getters: {
    isLoaded: (state) => Boolean(state.snapshot),
    menus: (state) => normalizeMenus(state.snapshot?.menus),
    routePermissions: (state) => state.snapshot?.routePermissions || [],
    buttonPermissions: (state) => state.snapshot?.buttonPermissions || [],
    columnPermissions: (state) => state.snapshot?.columnPermissions || {},
    hasRoutePermission: (state) => (permission?: string | string[]) =>
      hasAnyPermission(state.snapshot?.routePermissions || [], permission),
    hasButtonPermission: (state) => (permission?: string | string[]) =>
      hasAnyPermission(state.snapshot?.buttonPermissions || [], permission),
  },
  actions: {
    async loadPermissionSnapshot() {
      return this.refreshCurrentAuthorizationContext();
    },
    async refreshCurrentAuthorizationContext() {
      this.loading = true;
      try {
        const [snapshot, currentMenus] = await Promise.all([
          getCurrentPermissionSnapshot(),
          getCurrentMenus(),
        ]);
        this.snapshot = {
          ...emptySnapshot(),
          ...snapshot,
          menus: normalizeMenus(currentMenus),
        };
        return this.snapshot;
      } finally {
        this.loading = false;
      }
    },
    clear() {
      this.snapshot = null;
      this.loading = false;
    },
  },
});
