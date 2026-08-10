import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function assertFile(relativePath) {
  if (!fs.existsSync(path.join(root, relativePath))) {
    throw new Error(`Missing required file: ${relativePath}`);
  }
}

function assertIncludes(source, text, message) {
  if (!source.includes(text)) {
    throw new Error(message);
  }
}

function assertMatches(source, pattern, message) {
  if (!pattern.test(source)) {
    throw new Error(message);
  }
}

assertFile('src/api/tenants.ts');
assertFile('src/api/iamAdmin.ts');
assertFile('src/types/tenant.ts');
assertFile('src/config/businessPageRequiredPermissions.ts');
assertFile('src/config/tenantBrowseTableSchema.ts');
assertFile('src/views/TenantManagementView.vue');

const apiSource = read('src/api/tenants.ts');
const iamAdminApiSource = read('src/api/iamAdmin.ts');
[
  'listTenants',
  'createTenant',
  'updateTenant',
  'enableTenant',
  'disableTenant',
  'deleteTenant',
  'listTenantProvisionTasks',
  'retryTenantProvisionTask',
  'retryTenantOutboxEvent',
  'repairTenantPermissionSync',
  "'/api/tenants'",
  '`/api/tenants/${tenantId}`',
  '`/api/tenants/${tenantId}/enable`',
  '`/api/tenants/${tenantId}/disable`',
  'http.delete<ApiResponse<null> | null>(`/api/tenants/${tenantId}`',
  '`/api/tenants/${tenantId}/provision-tasks`',
  '`/api/tenants/${tenantId}/permission-sync/repair`',
  '`/api/tenant-provision-tasks/${taskId}/retry`',
  '`/api/tenant-outbox-events/${eventId}/retry`',
].forEach((text) => assertIncludes(apiSource, text, `Tenant API should include ${text}.`));

[
  'resetIamTenantAdminPassword',
  '`/api/iam/users/tenants/${tenantId}/admin/reset-password`',
].forEach((text) => assertIncludes(iamAdminApiSource, text, `IAM admin API should include ${text}.`));

const typeSource = read('src/types/tenant.ts');
[
  'export interface Tenant',
  'export interface CreateTenantPayload',
  'export interface UpdateTenantPayload',
  'export interface ChangeTenantStatusPayload',
  'export interface DeleteTenantPayload',
  'export interface TenantProvisionTask',
  'export interface TenantProvisionTaskStep',
  'lastErrorCode?: string | null',
  'lastErrorMessage?: string | null',
  'currentPlanAssignmentId',
  'currentPlanExpiresAt',
  'planExpiresAt',
  'export interface TenantPlanAssignmentPayload',
  "export type TenantStatus",
  "export type TenantPermissionSyncStatus",
  'permissionSyncStatus',
  'permissionSyncStatusLabel',
  'permissionSyncLastCheckedAt',
  'permissionSyncLastSyncedAt',
  'permissionSyncLastErrorMessage',
].forEach((text) => assertIncludes(typeSource, text, `Tenant types should include ${text}.`));

const requiredPermissionSource = read('src/config/businessPageRequiredPermissions.ts');
[
  'TENANT_MANAGEMENT_PAGE_REQUIRED_PERMISSIONS',
  "'tenant:admin-password:reset'",
  "'tenant:enable'",
  "'tenant:disable'",
  "'tenant:delete'",
  "'tenant-domain:view'",
  "'tenant-domain:manage'",
  "'tenant-contact:view'",
  "'tenant-contact:manage'",
  "'tenant-config:view'",
  "'tenant-config:manage'",
].forEach((text) => assertIncludes(requiredPermissionSource, text, `Tenant page required permissions should include ${text}.`));

const browseTableSchemaSource = read('src/config/tenantBrowseTableSchema.ts');
[
  'createTenantBrowseTableSchema',
  'tenantStatusMeta',
  'resolveTenantStatus',
  "resourceKey: 'tenant'",
  "columnKey: 'code'",
  "columnKey: 'name'",
  "columnKey: 'status'",
  "columnKey: 'currentPlanName'",
  "columnKey: 'currentPlanExpiresAt'",
  "columnKey: 'primaryDomain'",
  "columnKey: 'contactName'",
  "columnKey: 'contactPhone'",
  "columnKey: 'provisionedAt'",
  "columnKey: 'permissionSyncStatus'",
  "title: '权限同步状态'",
  'resolveTenantPermissionSyncStatus',
].forEach((text) => assertIncludes(browseTableSchemaSource, text, `Shared tenant browse table schema should include ${text}.`));

const viewSource = read('src/views/TenantManagementView.vue');
[
  'defineOptions({ name: \'TenantManagementView\' })',
  'DynamicFormDialog',
  'XuanBrowseTable',
  'BrowseTableDensity',
  'tenantBrowseTableSchema',
  ':schema="tenantBrowseTableSchema"',
  ':column-permission-snapshot="tenantColumnPermissionSnapshot"',
  ':strict-column-permission-snapshot="strictColumnPermissionSnapshot"',
  '@row-action="handleTenantRowAction"',
  'createTenantBrowseTableSchema',
  'const browseTenantId',
  'const browseUserId',
  'useAuthorizationStore',
  'tenantColumnPermissionSnapshot',
  'strictColumnPermissionSnapshot',
  'tenantRawColumnPermissions',
  'listTenants',
  'createTenant',
  'updateTenant',
  'listTenantProvisionTasks',
  'retryTenantProvisionTask',
  'repairTenantPermissionSync',
  '租户列表',
  '创建租户',
  '编辑租户',
  'openEditTenant',
  'submitEditTenant',
  'resetIamTenantAdminPassword',
  'reset-admin-password',
  '重置管理员密码',
  'tenant:admin-password:reset',
  'adminPasswordDialogVisible',
  'submitAdminPasswordReset',
  'submitEnableTenant',
  'submitDisableTenant',
  'submitDeleteTenant',
  'tenant:enable',
  "stateResource: 'tenant'",
  "stateAction: 'enable'",
  'stateCode: (row) => row.status',
  'tenant:disable',
  "stateAction: 'disable'",
  'tenant:delete',
  "stateAction: 'delete'",
  'tenant-plan:assign',
  'permission-sync-repair',
  '立即修复',
  'submitPermissionSyncRepair',
  'resolveTenantPermissionSyncStatus',
  'tenant-provision:view',
  'createTenantFields',
  'editTenantFields',
  'planAdjustmentFields',
  'taskRetryFields',
  'outboxRetryFields',
  'tenantActionReasonDialogVisible',
  'tenantActionReasonConfirmText',
  'tenantActionReasonConfirmPermission',
  '启用',
  '停用',
  '删除',
  'editForm.planId',
  'normalizeUpdatePayload',
  '套餐到期时间',
  '套餐调整',
  'currentPlanExpiresAt',
  'planExpiresAt',
  'effectiveAt',
  'expiresAt',
  'tenantLabel',
  'currentPlanLabel',
  'taskLabel',
  'tenantActionReasonDialogDescription',
  'tenantActionReasonForm.reason',
  'submitPlanAdjustment',
  '初始化任务',
  '失败原因',
  'resolveProvisionErrorText',
  'const createdTenant = await createTenant',
  'selectedTenant.value = createdTenant',
  'provisionDrawerVisible.value = true',
  'await loadProvisionTasks()',
  '失败重试',
  'PROVISIONING',
].forEach((text) => assertIncludes(viewSource, text, `Tenant page should include ${text}.`));

assertMatches(
  viewSource,
  /function\s+canEnableTenant\s*\(\s*tenant:\s*Tenant\s*\)\s*\{[\s\S]*tenant\.status\s*===\s*'PROVISIONED'[\s\S]*tenant\.status\s*===\s*'DISABLED'[\s\S]*\}/,
  'Tenant page should allow enabling PROVISIONED and DISABLED tenants.',
);
assertMatches(
  viewSource,
  /function\s+canDisableTenant\s*\(\s*tenant:\s*Tenant\s*\)\s*\{[\s\S]*tenant\.status\s*===\s*'ENABLED'[\s\S]*\}/,
  'Tenant page should allow disabling ENABLED tenants.',
);

[
  'column-permission-template',
  'columnTemplateDialogVisible',
  'openColumnTemplateAssignment',
  'submitColumnTemplateAssignment',
  'selectedColumnTemplateIds',
  'defaultColumnTemplateId',
  'setIamTenantColumnPermissionTemplates',
].forEach((text) => {
  if (viewSource.includes(text)) {
    throw new Error(`Tenant management should not manually assign column permission templates anymore: ${text}.`);
  }
});

[
  'createTenantPlanAssignment',
  'updateTenantPlanAssignment',
  "'/api/tenant-plan-assignments'",
  '`/api/tenant-plan-assignments/${assignmentId}`',
].forEach((text) => assertIncludes(apiSource, text, `Tenant API should include ${text}.`));

const mainSource = read('src/main.ts');
[
  'ElEmpty',
  'ElTimeline',
  'ElTimelineItem',
].forEach((text) => assertIncludes(mainSource, text, `App entry should register ${text}.`));

const routerSource = read('src/router/index.ts');
[
  'TenantManagementView',
  "path: 'system/tenants'",
  "permission: 'tenant:view'",
].forEach((text) => assertIncludes(routerSource, text, `Router should include ${text}.`));

const navigationSource = read('src/config/navigation.ts');
[
  'Building2',
  "'tenant-management'",
  "'/system/tenants'",
].forEach((text) => assertIncludes(navigationSource, text, `Navigation metadata should include ${text}.`));

console.log('Tenant management page contract verified.');
