import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));

const checks = [
  {
    file: 'src/framework/components-erp/DocumentEditorShell.vue',
    includes: ['defineProps<', 'document-editor-shell', 'sticky-summary', 'actions'],
  },
  {
    file: 'src/framework/components-erp/DocumentSection.vue',
    includes: ['defineProps<', 'document-section', 'document-section-title'],
  },
  {
    file: 'src/framework/components-erp/DocumentBasicInfoCard.vue',
    includes: [
      'defineModel<',
      'supplierOptions',
      'settlementAccountOptions',
      'document-basic-grid',
      'document-basic-remark-input',
      ':rows="1"',
    ],
  },
  {
    file: 'src/framework/components-erp/DocumentLineItemsTable.vue',
    includes: [
      'defineModel<',
      'addLine',
      'removeSelectedLines',
      'lineTotal',
      'document-line-table',
      'XuanDecimalInput',
      'document-line-table-cell-center',
      'header-align="center"',
    ],
    excludes: ['<el-input-number'],
  },
  {
    file: 'src/framework/components-erp/DocumentSettlementCard.vue',
    includes: ['defineModel<', 'settlementMethodOptions', 'discountAmount', 'document-settlement-grid', 'XuanDecimalInput'],
    excludes: ['<el-input-number'],
  },
  {
    file: 'src/views/PurchaseOrderCreateView.vue',
    includes: [
      'DocumentEditorShell',
      'DocumentBasicInfoCard',
      'DocumentLineItemsTable',
      'DocumentSettlementCard',
      'summaryItems',
      'formatCurrentDateTime',
      'documentTime: formatCurrentDateTime()',
      'submitForApproval',
    ],
    excludes: ["documentTime: '2026-07-10 18:21:02'"],
  },
  {
    file: 'src/views/ComponentCenterView.vue',
    includes: [
      'DocumentEditorShell',
      'DocumentBasicInfoCard',
      'DocumentLineItemsTable',
      'DocumentSettlementCard',
      'documentSummaryItems',
      'formatCurrentDateTime',
      'documentTime: formatCurrentDateTime()',
    ],
    excludes: ["documentTime: '2026-07-10 18:21:02'"],
  },
  {
    file: 'src/router/index.ts',
    includes: ['PurchaseOrderCreateView', "path: 'purchase/orders/create'", "name: 'purchase-order-create'"],
  },
  {
    file: 'src/views/PurchaseOrderView.vue',
    includes: ["router.push('/purchase/orders/create')"],
  },
  {
    file: 'src/styles/shell.css',
    includes: [
      '.document-editor-shell',
      '.document-editor-header',
      '.document-basic-grid',
      '.document-line-table',
      '.document-line-table-cell-center',
      '.document-basic-remark-input .el-textarea__inner',
      'resize: vertical',
      'text-align: center',
      '.document-summary-bar',
      '.document-settlement-grid',
    ],
  },
  {
    file: 'src/framework/components/XuanDecimalInput.vue',
    includes: ['align?:', "'center'", "align: 'center'", '.xuan-decimal-input--center'],
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
  for (const fragment of check.excludes || []) {
    if (content.includes(fragment)) {
      failures.push(`${check.file} should not contain "${fragment}"`);
    }
  }
}

if (failures.length) {
  console.error('Document editor page verification failed:');
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log('Document editor page verification passed.');
