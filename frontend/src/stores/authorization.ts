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

function normalizeStateActionPart(value: string | number | null | undefined, mode: 'resource' | 'state' | 'action') {
  if (value === null || value === undefined) {
    return '';
  }
  const text = String(value).trim();
  if (!text) {
    return '';
  }
  if (mode === 'state') {
    return text.toUpperCase();
  }
  return text.toLowerCase();
}

function stateActionKey(resourceKey: string, stateCode: string) {
  return `${resourceKey}:${stateCode}`;
}

function allowedStateActions(
  rules: Record<string, string[]>,
  resourceKey: string | number | null | undefined,
  stateCode: string | number | null | undefined,
) {
  const resource = normalizeStateActionPart(resourceKey, 'resource');
  const state = normalizeStateActionPart(stateCode, 'state');
  if (!resource || !state) {
    return [];
  }
  return [
    ...(rules[stateActionKey(resource, state)] || []),
    ...(rules[stateActionKey(resource, '*')] || []),
  ].map((action) => normalizeStateActionPart(action, 'action')).filter(Boolean);
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
    stateActionRules: (state) => state.snapshot?.stateActionRules || {},
    allowedStateActions: (state) => (
      resourceKey: string | number | null | undefined,
      stateCode: string | number | null | undefined,
    ) => allowedStateActions(state.snapshot?.stateActionRules || {}, resourceKey, stateCode),
    isStateActionAllowed: (state) => (
      resourceKey: string | number | null | undefined,
      stateCode: string | number | null | undefined,
      actionCode: string | number | null | undefined,
    ) => {
      if (hasAnyPermission(state.snapshot?.buttonPermissions || [], '*')) {
        return true;
      }
      const action = normalizeStateActionPart(actionCode, 'action');
      if (!action) {
        return true;
      }
      const actions = allowedStateActions(state.snapshot?.stateActionRules || {}, resourceKey, stateCode);
      return actions.includes('*') || actions.includes(action);
    },
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
