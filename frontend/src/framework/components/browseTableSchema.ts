import type { BrowseTableDensity } from './browseTablePreferences';

export type BrowseTableColumnAccessMode = 'VISIBLE' | 'MASKED' | 'HIDDEN';
export type BrowseTableColumnMaskType = 'generic' | 'phone';
export type BrowseTableButtonType = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info';
export type BrowseTableColumnDisplayType = 'text' | 'tag';
export type BrowseTableNoPermissionMode = 'hide' | 'disable';

export type BrowseTableColumnPermissionSnapshot = Record<string, BrowseTableColumnAccessMode>;

export type XuanBrowseTablePermissionSchema = {
  resourceKey?: string | null;
  columnKey?: string | null;
  maskType?: BrowseTableColumnMaskType;
  allowUserToggle?: boolean;
};

export type XuanBrowseTableColumnSchema<TRow extends object = Record<string, unknown>> = {
  key: string;
  title: string;
  prop?: keyof TRow & string;
  width?: number;
  minWidth?: number;
  visibleByDefault?: boolean;
  fixed?: 'left' | 'right' | null;
  align?: 'left' | 'center' | 'right';
  displayType?: BrowseTableColumnDisplayType;
  formatter?: (row: TRow) => unknown;
  tagType?: string | ((row: TRow, value: string) => string | undefined);
  permission?: XuanBrowseTablePermissionSchema;
};

export type XuanBrowseTableToolbarActionSchema = {
  key: string;
  label: string;
  type?: BrowseTableButtonType;
  text?: boolean;
  plain?: boolean;
  permission?: string;
  noPermissionMode?: BrowseTableNoPermissionMode;
  disabled?: boolean;
  disabledReason?: string;
};

export type XuanBrowseTableRowActionSchema<TRow extends object = Record<string, unknown>> = {
  key: string;
  label: string;
  type?: BrowseTableButtonType;
  link?: boolean;
  text?: boolean;
  plain?: boolean;
  permission?: string;
  noPermissionMode?: BrowseTableNoPermissionMode;
  visible?: boolean | ((row: TRow) => boolean);
  disabled?: boolean | ((row: TRow) => boolean);
  disabledReason?: string | ((row: TRow) => string);
};

export type XuanBrowseTableToolbarSchema = {
  showDensity?: boolean;
  showColumnSetting?: boolean;
  actions?: XuanBrowseTableToolbarActionSchema[];
};

export type XuanBrowseTablePaginationSchema = {
  show?: boolean;
  pageSizes?: number[];
};

export type XuanBrowseTableEmptyStateSchema = {
  title?: string;
  description?: string;
};

export type XuanBrowseTableSchema<TRow extends object = Record<string, unknown>> = {
  pageCode: string;
  tableCode: string;
  selectable?: boolean;
  actionsWidth?: number;
  defaultDensity?: BrowseTableDensity;
  defaultPageSize?: number;
  toolbar?: XuanBrowseTableToolbarSchema;
  columns: Array<XuanBrowseTableColumnSchema<TRow>>;
  rowActions?: Array<XuanBrowseTableRowActionSchema<TRow>>;
  pagination?: XuanBrowseTablePaginationSchema;
  emptyState?: XuanBrowseTableEmptyStateSchema;
};

export function createBrowseTablePermissionKey(resourceKey?: string | null, columnKey?: string | null) {
  const normalizedResourceKey = resourceKey?.trim();
  const normalizedColumnKey = columnKey?.trim();
  if (!normalizedResourceKey || !normalizedColumnKey) {
    return null;
  }
  return `${normalizedResourceKey}::${normalizedColumnKey}`;
}

export function resolveColumnAccessMode<TRow extends object>(
  column: XuanBrowseTableColumnSchema<TRow>,
  snapshot?: BrowseTableColumnPermissionSnapshot | null,
  strictMissing = false,
): BrowseTableColumnAccessMode {
  const permissionKey = createBrowseTablePermissionKey(column.permission?.resourceKey, column.permission?.columnKey);
  if (!permissionKey || !snapshot) {
    return 'VISIBLE';
  }
  return snapshot[permissionKey] || (strictMissing ? 'HIDDEN' : 'VISIBLE');
}

export function isBrowseTableColumnUserToggleAllowed<TRow extends object>(
  column: XuanBrowseTableColumnSchema<TRow>,
  accessMode: BrowseTableColumnAccessMode,
) {
  if (accessMode === 'HIDDEN') {
    return false;
  }
  return column.permission?.allowUserToggle !== false;
}

export function maskBrowseTableValue(value: unknown, maskType: BrowseTableColumnMaskType = 'generic') {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const text = String(value).trim();
  if (!text) {
    return '-';
  }
  if (maskType === 'phone') {
    const digits = text.replace(/\s+/g, '');
    if (/^\d{11}$/.test(digits)) {
      return `${digits.slice(0, 3)}****${digits.slice(-4)}`;
    }
  }
  if (text.length <= 1) {
    return '*';
  }
  if (text.length <= 4) {
    return `${text.slice(0, 1)}${'*'.repeat(text.length - 1)}`;
  }
  return `${text.slice(0, 2)}***${text.slice(-2)}`;
}

export function resolveBrowseTableCellText<TRow extends object>(
  row: TRow,
  column: XuanBrowseTableColumnSchema<TRow>,
  accessMode: BrowseTableColumnAccessMode,
) {
  const prop = column.prop || (column.key as keyof TRow & string);
  const rawValue = column.formatter ? column.formatter(row) : row[prop as keyof TRow];
  if (rawValue === null || rawValue === undefined || rawValue === '') {
    return '-';
  }
  if (accessMode === 'MASKED') {
    return maskBrowseTableValue(rawValue, column.permission?.maskType);
  }
  return String(rawValue);
}

export function resolveBrowseTableTagType<TRow extends object>(
  row: TRow,
  column: XuanBrowseTableColumnSchema<TRow>,
  value: string,
) {
  if (typeof column.tagType === 'function') {
    return column.tagType(row, value);
  }
  return column.tagType;
}
