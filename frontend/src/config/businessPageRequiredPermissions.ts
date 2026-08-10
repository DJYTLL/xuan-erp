const PURCHASE_DOCUMENT_REQUIRED_PERMISSIONS = [
  'procurement:view',
  'procurement:create',
  'procurement:audit',
  'procurement:export',
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

const PRODUCT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'product:view',
  'product:create',
  'product:update',
  'product:delete',
  'product:import',
  'product:export',
];

const PARTY_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'party:view',
  'party:create',
  'party:update',
  'party:delete',
  'party:import',
  'party:export',
];

const WAREHOUSE_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'warehouse:view',
  'warehouse:create',
  'warehouse:update',
  'warehouse:delete',
  'warehouse:import',
  'warehouse:export',
];

const INVENTORY_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'inventory:view',
  'inventory:create',
  'inventory:update',
  'inventory:delete',
  'inventory:audit',
  'inventory:import',
  'inventory:export',
  'product:view',
  'warehouse:view',
];

const SALES_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'sales:view',
  'sales:create',
  'sales:update',
  'sales:delete',
  'sales:audit',
  'sales:import',
  'sales:export',
  ...SALES_DOCUMENT_REQUIRED_PERMISSIONS,
];

const FINANCE_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'finance:view',
  'finance:create',
  'finance:update',
  'finance:delete',
  'finance:audit',
  'finance:import',
  'finance:export',
  'party:view',
];

const DOCUMENT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'document:view',
  'document:create',
  'document:update',
  'document:delete',
  'document:export',
];

const MANUFACTURING_PAGE_REQUIRED_PERMISSIONS = [
  'manufacturing:view',
  'manufacturing:create',
  'manufacturing:update',
  'manufacturing:delete',
  'manufacturing:audit',
  'manufacturing:import',
  'manufacturing:export',
  'product:view',
  'inventory:view',
  'warehouse:view',
];

const REPORT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'query:view',
  'query:create',
  'query:update',
  'query:delete',
  'query:export',
];

const IAM_MENU_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'iam-menu:view',
  'iam-menu:create',
  'iam-menu:update',
];

const IAM_PERMISSION_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'iam-permission:view',
  'iam-permission:create',
  'iam-permission:update',
];

const IAM_ROLE_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'iam-role:view',
  'iam-role:create',
  'iam-role:update',
  'iam-state-action:view',
  'iam-state-action:update',
];

const IAM_COLUMN_PERMISSION_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'iam-column-permission:view',
  'iam-column-permission:create',
  'iam-column-permission:update',
];

const IAM_ROLE_COLUMN_PERMISSION_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'iam-role:view',
  'iam-column-permission:view',
  'iam-role-column-permission:view',
  'iam-role-column-permission:update',
];

const IAM_STATE_ACTION_PERMISSION_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'iam-state-action:view',
  'iam-state-action:create',
  'iam-state-action:update',
];

const IAM_USER_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'iam-user:view',
  'iam-user:create',
  'iam-user:update',
  'iam-user:reset-password',
  'iam-user:delete',
];

const IAM_INIT_TEMPLATE_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'iam-init-template:view',
  'iam-init-template:create',
  'iam-init-template:update',
];

const TENANT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'tenant:view',
  'tenant:create',
  'tenant:update',
  'tenant:admin-password:reset',
  'tenant:enable',
  'tenant:disable',
  'tenant:delete',
  'tenant-domain:view',
  'tenant-domain:manage',
  'tenant-contact:view',
  'tenant-contact:manage',
  'tenant-config:view',
  'tenant-config:manage',
  'tenant-plan:view',
  'tenant-plan:assign',
  'tenant-provision:view',
  'tenant-provision:manage',
];

const TENANT_PLAN_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS = [
  'tenant-plan:view',
  'tenant-plan:manage',
  'tenant-plan:assign',
  'iam-column-permission:view',
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
  components: ['component-center:view'],
  'iam-menu-management': IAM_MENU_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-permission-management': IAM_PERMISSION_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-role-management': IAM_ROLE_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-column-permission-management': IAM_COLUMN_PERMISSION_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-role-column-permission-management': IAM_ROLE_COLUMN_PERMISSION_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-state-action-management': IAM_STATE_ACTION_PERMISSION_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-user-management': IAM_USER_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'iam-init-template-management': IAM_INIT_TEMPLATE_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
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
  product: PRODUCT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'product-management': PRODUCT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  party: PARTY_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'party-management': PARTY_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  warehouse: WAREHOUSE_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'warehouse-management': WAREHOUSE_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'sales-management': SALES_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  sales: SALES_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'sales-order': SALES_DOCUMENT_REQUIRED_PERMISSIONS,
  'sales-order-draft': SALES_DOCUMENT_REQUIRED_PERMISSIONS,
  'sales-order-approved': SALES_DOCUMENT_REQUIRED_PERMISSIONS,
  'stock-management': INVENTORY_PAGE_REQUIRED_PERMISSIONS,
  inventory: INVENTORY_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'inventory-management': INVENTORY_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  manufacturing: MANUFACTURING_PAGE_REQUIRED_PERMISSIONS,
  'manufacturing-management': MANUFACTURING_PAGE_REQUIRED_PERMISSIONS,
  finance: FINANCE_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'finance-management': FINANCE_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  document: DOCUMENT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'document-management': DOCUMENT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  report: REPORT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
  'report-center': REPORT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS,
};

export function collectBusinessPageRequiredPermissionCodes(
  menuCodes: string[],
  requiredPermissionMap: Record<string, string[]> = businessPageRequiredPermissionMap,
) {
  return [...new Set(menuCodes.flatMap((code) => requiredPermissionMap[code] || []))]
    .sort((left, right) => left.localeCompare(right));
}
