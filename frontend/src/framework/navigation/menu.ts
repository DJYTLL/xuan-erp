import type { Component } from 'vue';

export type FrameworkMenuSourceNode = {
  code: string;
  title: string;
  i18nKey?: string | null;
  path?: string | null;
  icon?: string | null;
  permissionCode?: string | null;
  sortNo: number;
  children?: FrameworkMenuSourceNode[];
};

export type MenuNode = {
  key: string;
  title: string;
  path?: string;
  icon?: Component;
  disabled?: boolean;
  children?: MenuNode[];
  permission?: string | string[];
  keepAlive?: boolean;
  affixTab?: boolean;
  closable?: boolean;
};

export type NavigationPermissionNode = {
  key: string;
  title: string;
  path?: string;
  sourceCode?: string;
  menuCodes: string[];
  children: NavigationPermissionNode[];
};

export type NavigationTranslator = (key: string) => string;

export type MenuUiMeta = Pick<MenuNode, 'icon' | 'disabled' | 'keepAlive' | 'affixTab' | 'closable'>;

export type NavigationBuildOptions = {
  iconMap?: Record<string, Component>;
  menuUiMetaMap?: Record<string, MenuUiMeta>;
  pathUiMetaMap?: Record<string, MenuUiMeta>;
};

function resolveMenuTitle(node: FrameworkMenuSourceNode, t: NavigationTranslator) {
  if (!node.i18nKey) {
    return node.title;
  }
  const translated = t(node.i18nKey);
  return translated === node.i18nKey ? node.title : translated;
}

function resolveMenuIcon(node: FrameworkMenuSourceNode, meta: MenuUiMeta, iconMap: Record<string, Component>) {
  if (meta.icon) {
    return meta.icon;
  }
  return node.icon ? iconMap[node.icon] : undefined;
}

function uniqueCodes(codes: string[]) {
  return [...new Set(codes)];
}

function createDefaultMenuItem(
  node: FrameworkMenuSourceNode,
  t: NavigationTranslator,
  options: NavigationBuildOptions,
): MenuNode | null {
  const path = node.path || undefined;
  const meta = {
    ...(path ? options.pathUiMetaMap?.[path] : {}),
    ...(options.menuUiMetaMap?.[node.code] || {}),
  };
  const children = (node.children || [])
    .map((child) => createDefaultMenuItem(child, t, options))
    .filter((child): child is MenuNode => Boolean(child));
  if (!path && children.length === 0) {
    return null;
  }
  return {
    key: node.code,
    title: resolveMenuTitle(node, t),
    path,
    permission: node.permissionCode || undefined,
    icon: resolveMenuIcon(node, meta, options.iconMap || {}),
    disabled: meta.disabled,
    keepAlive: meta.keepAlive,
    affixTab: meta.affixTab,
    closable: meta.closable,
    children,
  };
}

function createDefaultPermissionNode(
  node: FrameworkMenuSourceNode,
  t: NavigationTranslator,
): NavigationPermissionNode {
  const children = (node.children || []).map((child) => createDefaultPermissionNode(child, t));
  return {
    key: node.code,
    title: resolveMenuTitle(node, t),
    path: node.path || undefined,
    sourceCode: node.code,
    menuCodes: uniqueCodes([node.code, ...children.flatMap((child) => child.menuCodes)]),
    children,
  };
}

export function createNavigationMenu(
  nodes: FrameworkMenuSourceNode[],
  t: NavigationTranslator,
  options: NavigationBuildOptions = {},
): MenuNode[] {
  return [...nodes]
    .sort((left, right) => left.sortNo - right.sortNo)
    .map((node) => createDefaultMenuItem(node, t, options))
    .filter((item): item is MenuNode => Boolean(item));
}

export function filterNavigationMenuByPermission(
  items: MenuNode[],
  canAccess: (item: MenuNode) => boolean,
): MenuNode[] {
  const result: MenuNode[] = [];
  for (const item of items) {
    const children = item.children ? filterNavigationMenuByPermission(item.children, canAccess) : [];
    const selfAllowed = item.path ? canAccess(item) : false;
    if (selfAllowed || children.length) {
      result.push({
        ...item,
        path: selfAllowed ? item.path : undefined,
        children,
      });
    }
  }
  return result;
}

export function createNavigationPermissionTree(
  nodes: FrameworkMenuSourceNode[],
  t: NavigationTranslator,
): NavigationPermissionNode[] {
  return [...nodes]
    .sort((left, right) => left.sortNo - right.sortNo)
    .map((node) => createDefaultPermissionNode(node, t));
}
