import type { Component } from 'vue';
import {
  Activity,
  BadgeDollarSign,
  Boxes,
  Building2,
  CircleDollarSign,
  Database,
  KeyRound,
  LayoutDashboard,
  ListTree,
  PackageOpen,
  ScrollText,
  Settings,
  ShieldCheck,
  Users,
} from 'lucide-vue-next';
import {
  createNavigationMenu as createFrameworkNavigationMenu,
  createNavigationPermissionTree as createFrameworkNavigationPermissionTree,
  type MenuNode,
  type MenuUiMeta,
  type NavigationPermissionNode,
  type NavigationTranslator,
} from '@/framework/navigation/menu';
import type { CurrentMenuNode } from '@/types/auth';

export type {
  MenuNode,
  NavigationPermissionNode,
  NavigationTranslator,
};

const iconMap: Record<string, Component> = {
  Activity,
  BadgeDollarSign,
  Boxes,
  Building2,
  CircleDollarSign,
  Database,
  KeyRound,
  LayoutDashboard,
  ListTree,
  PackageOpen,
  ScrollText,
  Settings,
  ShieldCheck,
  Users,
};

const menuUiMetaMap: Record<string, MenuUiMeta> = {
  workbench: { icon: LayoutDashboard, keepAlive: true, affixTab: true, closable: false },
  dashboard: { icon: LayoutDashboard, keepAlive: true, affixTab: true, closable: false },
  product: { keepAlive: true },
  inventory: { icon: PackageOpen },
  products: { keepAlive: true },
  sales: { icon: BadgeDollarSign },
  warehouse: { icon: Boxes },
  finance: { icon: CircleDollarSign },
  system: { icon: Settings },
  'system-permission-center': { icon: KeyRound },
  'system-tenant-center': { icon: Building2 },
  'system-audit-center': { icon: ScrollText },
  'iam-menu-management': { icon: ListTree, keepAlive: true },
  'iam-permission-management': { icon: KeyRound, keepAlive: true },
  'iam-role-management': { icon: ShieldCheck, keepAlive: true },
  'iam-user-management': { icon: Users, keepAlive: true },
  'iam-init-template-management': { icon: ShieldCheck, keepAlive: true },
  'tenant-management': { icon: Building2, keepAlive: true },
  'tenant-plan-management': { icon: CircleDollarSign, keepAlive: true },
  'audit-logs': { icon: ScrollText, keepAlive: true },
  'audit-interface-costs': { icon: Activity, keepAlive: true },
  'audit-sql-rankings': { icon: Database, keepAlive: true },
  'purchase-order-draft': { keepAlive: true },
  'purchase-order-approved': { keepAlive: true },
  'purchase-return-draft': { keepAlive: true },
  'purchase-return-approved': { keepAlive: true },
};

const pathUiMetaMap: Record<string, MenuUiMeta> = {
  '/workbench': { icon: LayoutDashboard, keepAlive: true, affixTab: true, closable: false },
  '/product': { keepAlive: true },
  '/dashboard': { icon: LayoutDashboard, keepAlive: true, affixTab: true, closable: false },
  '/inventory/products': { keepAlive: true },
  '/system/iam/menus': { icon: ListTree, keepAlive: true },
  '/system/iam/permissions': { icon: KeyRound, keepAlive: true },
  '/system/iam/roles': { icon: ShieldCheck, keepAlive: true },
  '/system/iam/users': { icon: Users, keepAlive: true },
  '/system/iam/init-templates': { icon: ShieldCheck, keepAlive: true },
  '/system/tenants': { icon: Building2, keepAlive: true },
  '/system/tenant-plans': { icon: CircleDollarSign, keepAlive: true },
  '/system/audit/logs': { icon: ScrollText, keepAlive: true },
  '/system/audit/interface-costs': { icon: Activity, keepAlive: true },
  '/system/audit/sql-rankings': { icon: Database, keepAlive: true },
  '/purchase/orders/draft': { keepAlive: true },
  '/purchase/orders/approved': { keepAlive: true },
  '/purchase/returns/draft': { keepAlive: true },
  '/purchase/returns/approved': { keepAlive: true },
};

const navigationOptions = {
  iconMap,
  menuUiMetaMap,
  pathUiMetaMap,
};

export function createNavigationMenu(nodes: CurrentMenuNode[], t: NavigationTranslator): MenuNode[] {
  return createFrameworkNavigationMenu(nodes, t, navigationOptions);
}

export function createNavigationPermissionTree(
  nodes: CurrentMenuNode[],
  t: NavigationTranslator,
): NavigationPermissionNode[] {
  return createFrameworkNavigationPermissionTree(nodes, t);
}

export function createDefaultNavigationMenu(nodes: CurrentMenuNode[], t: NavigationTranslator): MenuNode[] {
  return createNavigationMenu(nodes, t);
}
