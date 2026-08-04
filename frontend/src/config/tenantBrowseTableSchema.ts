import type { BrowseTableDensity } from '@/framework/components/browseTablePreferences';
import type {
  XuanBrowseTableEmptyStateSchema,
  XuanBrowseTablePaginationSchema,
  XuanBrowseTableRowActionSchema,
  XuanBrowseTableSchema,
  XuanBrowseTableToolbarSchema,
} from '@/framework/components/browseTableSchema';
import type { Tenant, TenantPermissionSyncStatus, TenantStatus } from '@/types/tenant';

type TenantStatusTagType = 'success' | 'warning' | 'info' | 'primary' | 'danger';

export const tenantStatusMeta: Record<TenantStatus, { label: string; type: TenantStatusTagType }> = {
  PROVISIONING: { label: '初始化中', type: 'warning' },
  PROVISIONED: { label: '已开通', type: 'primary' },
  ENABLED: { label: '启用', type: 'success' },
  SUSPENDED: { label: '暂停', type: 'warning' },
  DISABLED: { label: '停用', type: 'info' },
};

export const tenantPermissionSyncStatusMeta: Record<TenantPermissionSyncStatus, { label: string; type: TenantStatusTagType }> = {
  SYNCED: { label: '正常', type: 'success' },
  PENDING_REPAIR: { label: '待修复', type: 'warning' },
  REPAIRING: { label: '修复中', type: 'primary' },
  FAILED: { label: '待修复', type: 'danger' },
};

export type TenantBrowseTableSchemaOptions = {
  pageCode: string;
  tableCode: string;
  selectable?: boolean;
  actionsWidth?: number;
  defaultDensity?: BrowseTableDensity;
  defaultPageSize?: number;
  toolbar?: XuanBrowseTableToolbarSchema;
  rowActions?: Array<XuanBrowseTableRowActionSchema<Tenant>>;
  pagination?: XuanBrowseTablePaginationSchema;
  emptyState?: XuanBrowseTableEmptyStateSchema;
};

export function resolveTenantStatus(status: TenantStatus) {
  return tenantStatusMeta[status] || { label: status, type: 'info' as TenantStatusTagType };
}

export function resolveTenantPermissionSyncStatus(status: TenantPermissionSyncStatus | null | undefined) {
  if (!status) {
    return tenantPermissionSyncStatusMeta.PENDING_REPAIR;
  }
  return tenantPermissionSyncStatusMeta[status] || tenantPermissionSyncStatusMeta.PENDING_REPAIR;
}

export function formatTenantDateTime(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}

export function createTenantBrowseTableSchema(options: TenantBrowseTableSchemaOptions): XuanBrowseTableSchema<Tenant> {
  return {
    pageCode: options.pageCode,
    tableCode: options.tableCode,
    selectable: options.selectable ?? false,
    actionsWidth: options.actionsWidth ?? 460,
    defaultDensity: options.defaultDensity ?? 'default',
    defaultPageSize: options.defaultPageSize ?? 20,
    toolbar: options.toolbar ?? {
      showDensity: true,
      showColumnSetting: true,
      actions: [],
    },
    columns: [
      {
        key: 'code',
        title: '租户编码',
        minWidth: 150,
        permission: {
          resourceKey: 'tenant',
          columnKey: 'code',
        },
      },
      {
        key: 'name',
        title: '租户名称',
        minWidth: 180,
        permission: {
          resourceKey: 'tenant',
          columnKey: 'name',
        },
      },
      {
        key: 'status',
        title: '状态',
        width: 130,
        displayType: 'tag',
        formatter: (row) => resolveTenantStatus(row.status).label,
        tagType: (row) => resolveTenantStatus(row.status).type,
        permission: {
          resourceKey: 'tenant',
          columnKey: 'status',
        },
      },
      {
        key: 'currentPlanName',
        title: '当前套餐',
        minWidth: 140,
        formatter: (row) => row.currentPlanName || row.currentPlanCode || '-',
        permission: {
          resourceKey: 'tenant',
          columnKey: 'currentPlanName',
        },
      },
      {
        key: 'currentPlanExpiresAt',
        title: '套餐到期',
        minWidth: 180,
        formatter: (row) => formatTenantDateTime(row.currentPlanExpiresAt),
        permission: {
          resourceKey: 'tenant',
          columnKey: 'currentPlanExpiresAt',
        },
      },
      {
        key: 'primaryDomain',
        title: '主域名',
        minWidth: 180,
        formatter: (row) => row.primaryDomain || '-',
        permission: {
          resourceKey: 'tenant',
          columnKey: 'primaryDomain',
        },
      },
      {
        key: 'contactName',
        title: '联系人',
        minWidth: 150,
        permission: {
          resourceKey: 'tenant',
          columnKey: 'contactName',
        },
      },
      {
        key: 'contactPhone',
        title: '联系电话',
        minWidth: 150,
        permission: {
          resourceKey: 'tenant',
          columnKey: 'contactPhone',
          maskType: 'phone',
        },
      },
      {
        key: 'provisionedAt',
        title: '开通完成',
        minWidth: 180,
        formatter: (row) => formatTenantDateTime(row.provisionedAt),
        permission: {
          resourceKey: 'tenant',
          columnKey: 'provisionedAt',
        },
      },
      {
        key: 'permissionSyncStatus',
        title: '权限同步状态',
        width: 150,
        displayType: 'tag',
        formatter: (row) => row.permissionSyncStatusLabel || resolveTenantPermissionSyncStatus(row.permissionSyncStatus).label,
        tagType: (row) => resolveTenantPermissionSyncStatus(row.permissionSyncStatus).type,
        permission: {
          resourceKey: 'tenant',
          columnKey: 'permissionSyncStatus',
        },
      },
    ],
    rowActions: options.rowActions ?? [],
    pagination: options.pagination ?? {
      show: true,
      pageSizes: [10, 20, 50, 100],
    },
    emptyState: options.emptyState ?? {
      title: '暂无租户',
      description: '当前筛选条件下没有租户数据。',
    },
  };
}
