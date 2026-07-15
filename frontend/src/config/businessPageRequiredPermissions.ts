const PURCHASE_DOCUMENT_REQUIRED_PERMISSIONS = [
  'product:view',
  'inventory:view',
  'warehouse:view',
  'party:view',
];

const SALES_DOCUMENT_REQUIRED_PERMISSIONS = [
  'product:view',
  'inventory:view',
  'warehouse:view',
  'party:view',
];

const INVENTORY_PAGE_REQUIRED_PERMISSIONS = [
  'product:view',
  'warehouse:view',
];

const MANUFACTURING_PAGE_REQUIRED_PERMISSIONS = [
  'product:view',
  'inventory:view',
  'warehouse:view',
];

const IAM_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'iam:view',
  'iam:create',
  'iam:update',
];

const IAM_USER_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  ...IAM_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam:delete',
];

const TENANT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'tenant:view',
  'tenant:create',
  'tenant:update',
  'tenant-plan:view',
  'tenant-plan:assign',
  'tenant-provision:view',
  'tenant-provision:manage',
];

const TENANT_PLAN_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'tenant-plan:view',
  'tenant-plan:manage',
  'tenant-plan:assign',
];

const AUDIT_LOG_PAGE_REQUIRED_PERMISSIONS = [
  'audit:log:view',
];

const AUDIT_INTERFACE_COST_PAGE_REQUIRED_PERMISSIONS = [
  'audit:interface-cost:view',
];

const AUDIT_SQL_RANKING_PAGE_REQUIRED_PERMISSIONS = [
  'audit:sql-ranking:view',
];

export const businessPageRequiredPermissionMap: Record<string, string[]> = {
  components: ['iam:view'],
  'iam-menu-management': IAM_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-permission-management': IAM_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-role-management': IAM_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-user-management': IAM_USER_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-init-template-management': IAM_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'tenant-management': TENANT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'tenant-plan-management': TENANT_PLAN_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'audit-logs': AUDIT_LOG_PAGE_REQUIRED_PERMISSIONS,
  'audit-interface-costs': AUDIT_INTERFACE_COST_PAGE_REQUIRED_PERMISSIONS,
  'audit-sql-rankings': AUDIT_SQL_RANKING_PAGE_REQUIRED_PERMISSIONS,
  'purchase-management': PURCHASE_DOCUMENT_REQUIRED_PERMISSIONS,
  procurement: PURCHASE_DOCUMENT_REQUIRED_PERMISSIONS,
  'purchase-order-create': PURCHASE_DOCUMENT_REQUIRED_PERMISSIONS,
  'purchase-order-draft': PURCHASE_DOCUMENT_REQUIRED_PERMISSIONS,
  'purchase-order-approved': PURCHASE_DOCUMENT_REQUIRED_PERMISSIONS,
  'purchase-return-draft': PURCHASE_DOCUMENT_REQUIRED_PERMISSIONS,
  'purchase-return-approved': PURCHASE_DOCUMENT_REQUIRED_PERMISSIONS,
  'sales-management': SALES_DOCUMENT_REQUIRED_PERMISSIONS,
  sales: SALES_DOCUMENT_REQUIRED_PERMISSIONS,
  'sales-order': SALES_DOCUMENT_REQUIRED_PERMISSIONS,
  'sales-order-draft': SALES_DOCUMENT_REQUIRED_PERMISSIONS,
  'sales-order-approved': SALES_DOCUMENT_REQUIRED_PERMISSIONS,
  'stock-management': INVENTORY_PAGE_REQUIRED_PERMISSIONS,
  inventory: INVENTORY_PAGE_REQUIRED_PERMISSIONS,
  manufacturing: MANUFACTURING_PAGE_REQUIRED_PERMISSIONS,
};

export function collectBusinessPageRequiredPermissionCodes(
  menuCodes: string[],
  requiredPermissionMap: Record<string, string[]> = businessPageRequiredPermissionMap,
) {
  return [...new Set(menuCodes.flatMap((code) => requiredPermissionMap[code] || []))]
    .sort((left, right) => left.localeCompare(right));
}
