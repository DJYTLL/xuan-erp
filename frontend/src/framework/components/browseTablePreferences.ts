export type BrowseTableDensity = 'default' | 'small' | 'large';

export type BrowseTableColumnSetting = {
  key: string;
  title: string;
  visible: boolean;
  width: number;
  order: number;
  fixed: 'left' | 'right' | null;
};

export type BrowseTableColumnInput = {
  key: string;
  title: string;
  width?: number;
  fixed?: 'left' | 'right' | null;
  visible?: boolean;
};

export type BrowseTablePreference = {
  tenantId: string;
  userId: string;
  pageCode: string;
  tableCode: string;
  columns: BrowseTableColumnSetting[];
  pageSize: number;
  density: BrowseTableDensity;
};

const MIN_COLUMN_WIDTH = 90;

export function getBrowseTablePreferenceKey(options: {
  tenantId: string;
  userId: string;
  pageCode: string;
  tableCode: string;
}) {
  return [
    'xuan-erp-browse-table-v1',
    options.tenantId || 'default-tenant',
    options.userId || 'anonymous',
    options.pageCode,
    options.tableCode,
  ].join(':');
}

export function createDefaultBrowseTablePreference(options: {
  tenantId: string;
  userId: string;
  pageCode: string;
  tableCode: string;
  columns: BrowseTableColumnInput[];
  pageSize: number;
  density: BrowseTableDensity;
}): BrowseTablePreference {
  return {
    tenantId: options.tenantId,
    userId: options.userId,
    pageCode: options.pageCode,
    tableCode: options.tableCode,
    columns: options.columns.map((column, index) => ({
      key: column.key,
      title: column.title,
      visible: column.visible ?? true,
      width: normalizeWidth(column.width),
      order: index + 1,
      fixed: column.fixed ?? null,
    })),
    pageSize: options.pageSize,
    density: options.density,
  };
}

export function mergeBrowseTablePreference(
  defaults: BrowseTablePreference,
  saved: BrowseTablePreference | null | undefined,
): BrowseTablePreference {
  if (!saved) {
    return defaults;
  }

  const savedColumnMap = new Map(saved.columns.map((column) => [column.key, column]));
  const defaultColumnMap = new Map(defaults.columns.map((column) => [column.key, column]));
  const mergedColumns = defaults.columns.map((column) => {
    const savedColumn = savedColumnMap.get(column.key);
    return {
      ...column,
      visible: savedColumn?.visible ?? column.visible,
      width: normalizeWidth(savedColumn?.width ?? column.width),
      order: savedColumn?.order ?? column.order,
      fixed: savedColumn?.fixed ?? column.fixed,
      title: column.title,
    };
  });

  const orderedKnownColumns = mergedColumns
    .filter((column) => savedColumnMap.has(column.key))
    .sort((left, right) => left.order - right.order);
  const newColumns = defaults.columns.filter((column) => !savedColumnMap.has(column.key));
  const nextColumns = [...orderedKnownColumns, ...newColumns]
    .filter((column) => defaultColumnMap.has(column.key))
    .map((column, index) => ({ ...column, order: index + 1 }));

  return {
    ...defaults,
    pageSize: saved.pageSize || defaults.pageSize,
    density: saved.density || defaults.density,
    columns: nextColumns,
  };
}

export function moveColumn(
  preference: BrowseTablePreference,
  columnKey: string,
  direction: -1 | 1,
): BrowseTablePreference {
  const nextColumns = [...preference.columns];
  const currentIndex = nextColumns.findIndex((column) => column.key === columnKey);
  const targetIndex = currentIndex + direction;

  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= nextColumns.length) {
    return preference;
  }

  const [column] = nextColumns.splice(currentIndex, 1);
  nextColumns.splice(targetIndex, 0, column);

  return {
    ...preference,
    columns: nextColumns.map((item, index) => ({ ...item, order: index + 1 })),
  };
}

export function resizeColumn(
  preference: BrowseTablePreference,
  columnKey: string,
  width: number,
): BrowseTablePreference {
  return {
    ...preference,
    columns: preference.columns.map((column) => {
      if (column.key !== columnKey) {
        return column;
      }
      return { ...column, width: normalizeWidth(width) };
    }),
  };
}

export function toggleColumnVisibility(
  preference: BrowseTablePreference,
  columnKey: string,
  visible: boolean,
): BrowseTablePreference {
  return {
    ...preference,
    columns: preference.columns.map((column) => {
      if (column.key !== columnKey) {
        return column;
      }
      return { ...column, visible };
    }),
  };
}

export function normalizeWidth(width: number | undefined) {
  if (!width || Number.isNaN(width)) {
    return 120;
  }
  return Math.max(MIN_COLUMN_WIDTH, Math.round(width));
}

