import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));

const checks = [
  {
    file: 'src/framework/components/DynamicFormDialog.vue',
    includes: [
      'defineProps<',
      'defineEmits<',
      'fields',
      'submit',
      'DynamicFormSection',
      'workspace-dialog-body',
      'slot name="body"',
      'renderForm',
      "size?: 'sm' | 'md' | 'lg'",
      'sizeWidthMap',
      'dialogWidth',
      'slot name="footer"',
      "component === 'radio-group'",
      "component === 'tree-select'",
      "component === 'datetime'",
      'el-radio-group',
      'el-tree-select',
    ],
  },
  {
    file: 'src/styles/shell.css',
    includes: ['.dynamic-form-dialog--workspace', 'margin: 24px auto', 'max-height: calc(100vh - 48px)', 'background: var(--xuan-panel);'],
  },
  {
    file: 'src/framework/components/DetailDrawer.vue',
    includes: ['defineProps<', 'items', 'modelValue'],
  },
  {
    file: 'src/framework/components/ApprovalConfirmDialog.vue',
    includes: ['defineProps<', 'approve', 'reject', 'submit'],
  },
  {
    file: 'src/framework/components/BatchConfirmDialog.vue',
    includes: ['defineProps<', 'selectedCount', 'confirm'],
  },
  {
    file: 'src/views/ComponentCenterView.vue',
    includes: ['DynamicFormDialog', 'MenuPermissionAssignment', 'DetailDrawer', 'ApprovalConfirmDialog', 'BatchConfirmDialog', 'overview-dialogs', 'dialog-preview-strip', 'roleGrantPreviewVisible', 'openRoleGrantPreview'],
  },
  {
    file: 'src/views/IamMenuManagementView.vue',
    includes: ['DynamicFormDialog', 'menuType', 'parentMenuTreeOptions', 'async function submitMenu(value', 'Object.assign(form, value)', 'size="lg"'],
  },
  {
    file: 'src/views/IamPermissionManagementView.vue',
    includes: ['DynamicFormDialog', 'menuSelectOptions', 'async function submitPermission(value:', 'Object.assign(form, value)', 'size="md"'],
  },
  {
    file: 'src/views/IamTenantInitTemplateManagementView.vue',
    includes: [
      'DynamicFormDialog',
      'MenuPermissionAssignment',
      'grantVisible',
      'async function submitTemplate(value:',
      'Object.assign(form, value)',
      'variant="workspace"',
      'size="md"',
    ],
  },
  {
    file: 'src/views/IamRoleManagementView.vue',
    includes: [
      'DynamicFormDialog',
      'MenuPermissionAssignment',
      'size="lg"',
      ':render-form="false"',
      'confirm-text="保存授权"',
    ],
  },
  {
    file: 'src/views/IamUserManagementView.vue',
    includes: [
      'DynamicFormDialog',
      'grantVisible',
      'passwordDialogVisible',
      'size="lg"',
      'size="sm"',
      'variant="workspace"',
      ':render-form="false"',
    ],
  },
  {
    file: 'src/views/TenantPlanManagementView.vue',
    includes: ['DynamicFormDialog', 'planDialogVisible', 'async function submitPlan(value:', 'Object.assign(planForm, value)', 'size="lg"'],
  },
  {
    file: 'src/views/TenantManagementView.vue',
    includes: [
      'DynamicFormDialog',
      'createDialogVisible',
      'editDialogVisible',
      'planAdjustmentDialogVisible',
      'taskRetryDialogVisible',
      'outboxRetryDialogVisible',
      'async function submitCreateTenant(value:',
      'async function submitEditTenant(value:',
      'async function submitPlanAdjustment(value:',
      'async function submitTaskRetry(value:',
      'async function submitOutboxRetry(value:',
      'async function submitTenantActionReason(value:',
      'size="lg"',
      'size="md"',
      'size="sm"',
    ],
  },
  {
    file: 'src/views/ProductManagementView.vue',
    includes: ['DynamicFormDialog', 'DetailDrawer', 'BatchConfirmDialog', 'productFormSections', 'show-custom-fields'],
  },
  {
    file: 'src/views/PurchaseOrderView.vue',
    includes: ['DetailDrawer', 'ApprovalConfirmDialog', 'BatchConfirmDialog'],
  },
];

const failures = [];

for (const check of checks) {
  const path = resolve(root, check.file);
  if (!existsSync(path)) {
    failures.push(`${check.file} does not exist`);
    continue;
  }
  const content = readFileSync(path, 'utf8');
  for (const fragment of check.includes) {
    if (!content.includes(fragment)) {
      failures.push(`${check.file} missing "${fragment}"`);
    }
  }
  if (check.file.startsWith('src/views/')) {
    if (content.includes('<el-dialog v-model=')) {
      failures.push(`${check.file} still contains raw el-dialog usage`);
    }
  }
}

if (failures.length) {
  console.error('Dialog system verification failed:');
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log('Dialog system verification passed.');
