import type { BrowseTableDensity } from '@/framework/components/browseTablePreferences';
import type {
  XuanBrowseTableEmptyStateSchema,
  XuanBrowseTablePaginationSchema,
  XuanBrowseTableRowActionSchema,
  XuanBrowseTableSchema,
  XuanBrowseTableToolbarSchema,
} from '@/framework/components/browseTableSchema';
import type { IamUser } from '@/types/iamAdmin';

type IamUserStatusTagType = 'success' | 'info';

export type IamUserBrowseTableSchemaOptions = {
  pageCode: string;
  tableCode: string;
  selectable?: boolean;
  actionsWidth?: number;
  defaultDensity?: BrowseTableDensity;
  defaultPageSize?: number;
  toolbar?: XuanBrowseTableToolbarSchema;
  rowActions?: Array<XuanBrowseTableRowActionSchema<IamUser>>;
  pagination?: XuanBrowseTablePaginationSchema;
  emptyState?: XuanBrowseTableEmptyStateSchema;
};

export function resolveIamUserStatus(user: IamUser): { label: string; type: IamUserStatusTagType } {
  if (user.enabled && user.accountNonLocked) {
    return { label: '正常', type: 'success' };
  }
  return { label: '受限', type: 'info' };
}

export function resolveIamUserDisplayName(user: IamUser): string {
  const displayName = user.displayName?.trim() || '';
  if (displayName && !hasCorruptedText(displayName)) {
    return displayName;
  }
  if (user.username === 'codex_tenant_viewer') {
    return 'Codex 租户查看员';
  }
  return user.username;
}

function hasCorruptedText(value: string) {
  return /\?{2,}|�/.test(value);
}

export function createIamUserBrowseTableSchema(options: IamUserBrowseTableSchemaOptions): XuanBrowseTableSchema<IamUser> {
  return {
    pageCode: options.pageCode,
    tableCode: options.tableCode,
    selectable: options.selectable ?? false,
    actionsWidth: options.actionsWidth ?? 300,
    defaultDensity: options.defaultDensity ?? 'default',
    defaultPageSize: options.defaultPageSize ?? 20,
    toolbar: options.toolbar ?? {
      showDensity: true,
      showColumnSetting: true,
      actions: [],
    },
    columns: [
      {
        key: 'username',
        title: '用户名',
        minWidth: 150,
        permission: {
          resourceKey: 'iam-user',
          columnKey: 'username',
        },
      },
      {
        key: 'displayName',
        title: '显示名',
        minWidth: 140,
        formatter: (row) => resolveIamUserDisplayName(row),
        permission: {
          resourceKey: 'iam-user',
          columnKey: 'displayName',
        },
      },
      {
        key: 'phone',
        title: '手机号',
        minWidth: 140,
        permission: {
          resourceKey: 'iam-user',
          columnKey: 'phone',
          maskType: 'phone',
        },
      },
      {
        key: 'email',
        title: '邮箱',
        minWidth: 190,
        permission: {
          resourceKey: 'iam-user',
          columnKey: 'email',
        },
      },
      {
        key: 'authVersion',
        title: '权限版本',
        width: 110,
        align: 'center',
        permission: {
          resourceKey: 'iam-user',
          columnKey: 'authVersion',
        },
      },
      {
        key: 'status',
        title: '状态',
        width: 120,
        displayType: 'tag',
        formatter: (row) => resolveIamUserStatus(row).label,
        tagType: (row) => resolveIamUserStatus(row).type,
        permission: {
          resourceKey: 'iam-user',
          columnKey: 'status',
        },
      },
    ],
    rowActions: options.rowActions ?? [],
    pagination: options.pagination ?? {
      show: true,
      pageSizes: [10, 20, 50, 100],
    },
    emptyState: options.emptyState ?? {
      title: '暂无用户',
      description: '当前筛选条件下没有用户数据。',
    },
  };
}
