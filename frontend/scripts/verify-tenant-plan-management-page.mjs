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
assertFile('src/views/TenantPlanManagementView.vue');

const packageSource = read('package.json');
const apiSource = read('src/api/tenants.ts');
const typeSource = read('src/types/tenant.ts');
const viewSource = read('src/views/TenantPlanManagementView.vue');
const routerSource = read('src/router/index.ts');
const navigationSource = read('src/config/navigation.ts');
const messagesSource = read('src/i18n/messages.ts');

assertIncludes(
  packageSource,
  'verify:tenant-plan-management-page',
  'package.json 应暴露套餐管理页面校验脚本。',
);

[
  'createTenantPlan',
  'updateTenantPlan',
  'enableTenantPlan',
  'disableTenantPlan',
  'deleteTenantPlan',
  "'/api/tenant-plans'",
  '`/api/tenant-plans/${planId}`',
  '`/api/tenant-plans/${planId}/enable`',
  '`/api/tenant-plans/${planId}/disable`',
].forEach((text) => assertIncludes(apiSource, text, `Tenant API should include ${text}.`));

[
  'export interface TenantPlanPayload',
  'iamInitTemplateCode',
  'featureFlagsJson',
  'BillingCycle',
  'TenantPlanStatus',
].forEach((text) => assertIncludes(typeSource, text, `Tenant types should include ${text}.`));

[
  "defineOptions({ name: 'TenantPlanManagementView' })",
  '套餐管理',
  '新增套餐',
  '初始化模板',
  '基础版',
  '标准版',
  '完整版',
  'iamInitTemplateCode',
  'featureFlagsJson',
  "import XuanDecimalInput from '@/framework/components/XuanDecimalInput.vue';",
  '<XuanDecimalInput',
  'permission="tenant-plan:manage"',
  'submitPlan',
  'enableTenantPlan',
  'disableTenantPlan',
].forEach((text) => assertIncludes(viewSource, text, `Tenant plan page should include ${text}.`));

if (viewSource.includes('<el-input-number')) {
  throw new Error('Tenant plan numeric edit fields should use XuanDecimalInput from the component center.');
}

[
  'TenantPlanManagementView',
  "path: 'system/tenant-plans'",
  "name: 'tenant-plan-management'",
  "titleKey: 'route.tenantPlans'",
  "permission: 'tenant-plan:view'",
].forEach((text) => assertIncludes(routerSource, text, `Router should include ${text}.`));

[
  'CircleDollarSign',
  "'tenant-plan-management'",
  "'/system/tenant-plans'",
].forEach((text) => assertIncludes(navigationSource, text, `Navigation metadata should include ${text}.`));

[
  'tenantPlans',
  '套餐管理',
].forEach((text) => assertIncludes(messagesSource, text, `I18n messages should include ${text}.`));

console.log('Tenant plan management page contract verified.');
