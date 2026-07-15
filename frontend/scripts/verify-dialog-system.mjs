import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));

const checks = [
  {
    file: 'src/framework/components/DynamicFormDialog.vue',
    includes: ['defineProps<', 'defineEmits<', 'fields', 'submit', 'DynamicFormSection', 'workspace-dialog-body'],
  },
  {
    file: 'src/styles/shell.css',
    includes: ['.dynamic-form-dialog--workspace', 'margin: 24px auto', 'max-height: calc(100vh - 48px)'],
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
    includes: ['DynamicFormDialog', 'DetailDrawer', 'ApprovalConfirmDialog', 'BatchConfirmDialog'],
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
}

if (failures.length) {
  console.error('Dialog system verification failed:');
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log('Dialog system verification passed.');
