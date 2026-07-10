import type { Component } from 'vue';
import {
  BadgeDollarSign,
  Blocks,
  Boxes,
  CircleDollarSign,
  LayoutDashboard,
  PackageOpen,
  Settings,
} from 'lucide-vue-next';

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

export type NavigationTranslator = (key: string) => string;

export function createNavigationMenu(t: NavigationTranslator): MenuNode[] {
  return [
    {
      key: 'dashboard',
      title: t('nav.dashboard'),
      path: '/dashboard',
      icon: LayoutDashboard,
      keepAlive: true,
      affixTab: true,
      closable: false,
    },
    {
      key: 'inventory',
      title: t('nav.inventory'),
      icon: PackageOpen,
      children: [
        {
          key: 'base-data',
          title: t('nav.baseData'),
          children: [
            {
              key: 'products',
              title: t('nav.products'),
              path: '/inventory/products',
              permission: 'product:view',
              keepAlive: true,
            },
            {
              key: 'suppliers',
              title: '供应商管理',
              path: '/inventory/suppliers',
              disabled: true,
            },
            {
              key: 'warehouses',
              title: '仓库管理',
              path: '/inventory/warehouses',
              disabled: true,
            },
          ],
        },
        {
          key: 'purchase-management',
          title: t('nav.purchase'),
          children: [
            {
              key: 'purchase-order-draft',
              title: '采购单（草稿）',
              path: '/purchase/orders/draft',
              permission: 'purchase:order:view',
              keepAlive: true,
            },
            {
              key: 'purchase-order-approved',
              title: '采购单（已审核）',
              path: '/purchase/orders/approved',
              permission: 'purchase:order:view',
              keepAlive: true,
            },
            {
              key: 'purchase-return-draft',
              title: '采购退货（草稿）',
              path: '/purchase/returns/draft',
              permission: 'purchase:return:view',
              keepAlive: true,
            },
            {
              key: 'purchase-return-approved',
              title: '采购退货（已审核）',
              path: '/purchase/returns/approved',
              permission: 'purchase:return:view',
              keepAlive: true,
            },
          ],
        },
      ],
    },
    {
      key: 'sales',
      title: t('nav.sales'),
      icon: BadgeDollarSign,
      children: [
        {
          key: 'sales-orders',
          title: '销售订单',
          path: '/sales/orders',
          disabled: true,
        },
        {
          key: 'sales-return',
          title: '销售退货',
          path: '/sales/returns',
          disabled: true,
        },
      ],
    },
    {
      key: 'warehouse',
      title: '仓库管理',
      icon: Boxes,
      children: [
        {
          key: 'stock-in',
          title: '入库单',
          path: '/warehouse/in',
          disabled: true,
        },
        {
          key: 'stock-out',
          title: '出库单',
          path: '/warehouse/out',
          disabled: true,
        },
      ],
    },
    {
      key: 'finance',
      title: t('nav.finance'),
      icon: CircleDollarSign,
      children: [
        {
          key: 'payables',
          title: '应付款',
          path: '/finance/payables',
          disabled: true,
        },
        {
          key: 'receivables',
          title: '应收款',
          path: '/finance/receivables',
          disabled: true,
        },
      ],
    },
    {
      key: 'system',
      title: '系统设置',
      icon: Settings,
      children: [
        {
          key: 'components',
          title: t('nav.components'),
          path: '/components',
          icon: Blocks,
          keepAlive: true,
        },
      ],
    },
  ];
}
