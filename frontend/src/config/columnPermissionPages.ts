export type ColumnPermissionPreviewSchemaKey = 'tenant-management' | 'iam-user-management';

export interface ColumnPermissionPageConfig {
  menuCode: string;
  resourceKeys: string[];
  detailColumnKeys?: string[];
  previewSchemaKey: ColumnPermissionPreviewSchemaKey;
}

export const columnPermissionPageConfigs: ColumnPermissionPageConfig[] = [
  {
    menuCode: 'tenant-management',
    resourceKeys: ['tenant'],
    detailColumnKeys: ['remark'],
    previewSchemaKey: 'tenant-management',
  },
  {
    menuCode: 'iam-user-management',
    resourceKeys: ['iam-user'],
    previewSchemaKey: 'iam-user-management',
  },
];

export const columnPermissionPageConfigMap = new Map(
  columnPermissionPageConfigs.map((item) => [item.menuCode, item]),
);

export function findColumnPermissionPageConfig(menuCode?: string | null) {
  if (!menuCode) {
    return null;
  }
  return columnPermissionPageConfigMap.get(menuCode) || null;
}
