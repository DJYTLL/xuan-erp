import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import ts from 'typescript';

async function importTypescriptModule(filePath) {
  const source = await readFile(filePath, 'utf8');
  const transpiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.ESNext,
      target: ts.ScriptTarget.ES2022,
      strict: true,
    },
  });

  const moduleUrl = `data:text/javascript;base64,${Buffer.from(transpiled.outputText).toString('base64')}`;
  return import(moduleUrl);
}

const modulePath = new URL('../src/framework/components/browseTablePreferences.ts', import.meta.url);
const schemaModulePath = new URL('../src/framework/components/browseTableSchema.ts', import.meta.url);
const componentPath = new URL('../src/framework/components/XuanBrowseTable.vue', import.meta.url);
const shellStylePath = new URL('../src/styles/shell.css', import.meta.url);
const {
  createDefaultBrowseTablePreference,
  mergeBrowseTablePreference,
  moveColumn,
  resizeColumn,
  toggleColumnVisibility,
} = await importTypescriptModule(modulePath);
const {
  createBrowseTablePermissionKey,
  resolveColumnAccessMode,
  maskBrowseTableValue,
  resolveBrowseTableCellText,
} = await importTypescriptModule(schemaModulePath);

const columns = [
  { key: 'code', title: '编码', width: 120 },
  { key: 'name', title: '名称', width: 140 },
  { key: 'factoryCode', title: '厂家编码', width: 160 },
];

const base = createDefaultBrowseTablePreference({
  tenantId: '0',
  userId: 'super_admin',
  pageCode: 'inventory-products',
  tableCode: 'product-list',
  columns,
  pageSize: 10,
  density: 'default',
});

assert.deepEqual(
  base.columns.map((column) => ({
    key: column.key,
    visible: column.visible,
    width: column.width,
    order: column.order,
  })),
  [
    { key: 'code', visible: true, width: 120, order: 1 },
    { key: 'name', visible: true, width: 140, order: 2 },
    { key: 'factoryCode', visible: true, width: 160, order: 3 },
  ],
);

const saved = {
  ...base,
  columns: [
    { key: 'name', title: '旧名称', visible: false, width: 210, order: 1, fixed: null },
    { key: 'code', title: '旧编码', visible: true, width: 180, order: 2, fixed: null },
  ],
  pageSize: 20,
  density: 'small',
};

const merged = mergeBrowseTablePreference(base, saved);

assert.deepEqual(
  merged.columns.map((column) => ({
    key: column.key,
    title: column.title,
    visible: column.visible,
    width: column.width,
    order: column.order,
  })),
  [
    { key: 'name', title: '名称', visible: false, width: 210, order: 1 },
    { key: 'code', title: '编码', visible: true, width: 180, order: 2 },
    { key: 'factoryCode', title: '厂家编码', visible: true, width: 160, order: 3 },
  ],
);
assert.equal(merged.pageSize, 20);
assert.equal(merged.density, 'small');

const moved = moveColumn(merged, 'factoryCode', -1);
assert.deepEqual(moved.columns.map((column) => column.key), ['name', 'factoryCode', 'code']);
assert.deepEqual(moved.columns.map((column) => column.order), [1, 2, 3]);

const resized = resizeColumn(moved, 'factoryCode', 88);
assert.equal(resized.columns.find((column) => column.key === 'factoryCode')?.width, 90);

const hidden = toggleColumnVisibility(resized, 'name', true);
assert.equal(hidden.columns.find((column) => column.key === 'name')?.visible, true);

assert.equal(createBrowseTablePermissionKey('erp-product', 'supplier'), 'erp-product::supplier');
assert.equal(maskBrowseTableValue('13812345678', 'phone'), '138****5678');
assert.equal(maskBrowseTableValue('MODEL-2026', 'generic'), 'MO***26');
assert.equal(
  resolveColumnAccessMode(
    {
      key: 'supplier',
      title: '来源供应商',
      permission: { resourceKey: 'erp-product', columnKey: 'supplier' },
    },
    { 'erp-product::supplier': 'HIDDEN' },
  ),
  'HIDDEN',
);
assert.equal(
  resolveColumnAccessMode(
    {
      key: 'authVersion',
      title: '权限版本',
      permission: { resourceKey: 'iam-user', columnKey: 'authVersion' },
    },
    { 'iam-user::username': 'VISIBLE' },
    true,
  ),
  'HIDDEN',
);
assert.equal(
  resolveColumnAccessMode(
    {
      key: 'authVersion',
      title: '权限版本',
      permission: { resourceKey: 'iam-user', columnKey: 'authVersion' },
    },
    { 'iam-user::username': 'VISIBLE' },
    false,
  ),
  'VISIBLE',
);
assert.equal(
  resolveBrowseTableCellText(
    { supplier: '默认供应商' },
    {
      key: 'supplier',
      title: '来源供应商',
      permission: { resourceKey: 'erp-product', columnKey: 'supplier', maskType: 'generic' },
    },
    'MASKED',
  ),
  '默认***应商',
);

const componentSource = await readFile(componentPath, 'utf8');
const shellStyleSource = await readFile(shellStylePath, 'utf8');

assert.match(componentSource, /width="460"/, '列设置弹层宽度必须和内容宽度一致，避免下拉框溢出');
assert.match(componentSource, /columnPermissionSnapshot/, '浏览表格必须接收列权限快照。');
assert.match(componentSource, /resolveColumnAccessMode/, '浏览表格必须解析列权限三态。');
assert.match(shellStyleSource, /\.browse-column-panel\s*{[^}]*width:\s*460px;/s, '列设置面板宽度必须固定为 460px');
assert.match(shellStyleSource, /\.browse-column-row\s*{[^}]*grid-template-columns:\s*minmax\(128px,\s*1fr\)\s*44px\s*44px\s*128px;/s, '列设置每行必须使用稳定 grid 列宽');
assert.match(shellStyleSource, /\.browse-column-list\s*{[^}]*overflow-x:\s*hidden;/s, '列设置滚动区不能出现横向溢出');
assert.match(shellStyleSource, /\.browse-column-row-meta\s*{/s, '列设置需要渲染列权限说明区域。');

console.log('browse table preference verification passed');
