import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));

const checks = [
  {
    file: 'src/components/business/DynamicFormDialog.vue',
    includes: ['defineProps<', 'defineEmits<', 'fields', 'submit'],
  },
  {
    file: 'src/components/business/DetailDrawer.vue',
    includes: ['defineProps<', 'items', 'modelValue'],
  },
  {
    file: 'src/components/business/ApprovalConfirmDialog.vue',
    includes: ['defineProps<', 'approve', 'reject', 'submit'],
  },
  {
    file: 'src/components/business/BatchConfirmDialog.vue',
    includes: ['defineProps<', 'selectedCount', 'confirm'],
  },
  {
    file: 'src/views/ComponentCenterView.vue',
    includes: ['DynamicFormDialog', 'DetailDrawer', 'ApprovalConfirmDialog', 'BatchConfirmDialog'],
  },
  {
    file: 'src/views/ProductManagementView.vue',
    includes: ['DynamicFormDialog', 'DetailDrawer', 'BatchConfirmDialog'],
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
