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

assertFile('src/api/tenants.ts');
assertFile('src/types/tenant.ts');
assertFile('src/views/TenantManagementView.vue');

const apiSource = read('src/api/tenants.ts');
[
  'listTenants',
  'createTenant',
  'updateTenant',
  'listTenantProvisionTasks',
  'retryTenantProvisionTask',
  'retryTenantOutboxEvent',
  "'/api/tenants'",
  '`/api/tenants/${tenantId}`',
  '`/api/tenants/${tenantId}/provision-tasks`',
  '`/api/tenant-provision-tasks/${taskId}/retry`',
  '`/api/tenant-outbox-events/${eventId}/retry`',
].forEach((text) => assertIncludes(apiSource, text, `Tenant API should include ${text}.`));

const typeSource = read('src/types/tenant.ts');
[
  'export interface Tenant',
  'export interface CreateTenantPayload',
  'export interface UpdateTenantPayload',
  'export interface TenantProvisionTask',
  'export interface TenantProvisionTaskStep',
  'lastErrorCode?: string | null',
  'lastErrorMessage?: string | null',
  'currentPlanAssignmentId',
  'currentPlanExpiresAt',
  'planExpiresAt',
  'export interface TenantPlanAssignmentPayload',
  "export type TenantStatus",
].forEach((text) => assertIncludes(typeSource, text, `Tenant types should include ${text}.`));

const viewSource = read('src/views/TenantManagementView.vue');
[
  'defineOptions({ name: \'TenantManagementView\' })',
  'listTenants',
  'createTenant',
  'updateTenant',
  'listTenantProvisionTasks',
  'retryTenantProvisionTask',
  '租户列表',
  '创建租户',
  '编辑租户',
  'openEditTenant',
  'submitEditTenant',
  'editForm.planId',
  'normalizeUpdatePayload',
  '套餐到期时间',
  '套餐调整',
  'currentPlanExpiresAt',
  'createForm.planExpiresAt',
  'planAdjustmentForm.expiresAt',
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
  '租户列表加载失败',
  '初始化任务加载失败',
].forEach((text) => assertIncludes(viewSource, text, `Tenant page should include ${text}.`));

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
