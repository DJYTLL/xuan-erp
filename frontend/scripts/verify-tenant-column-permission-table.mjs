import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

const tableSource = read('src/framework/components/XuanBrowseTable.vue');
const tableSchemaSource = read('src/framework/components/browseTableSchema.ts');
const tenantSchemaSource = read('src/config/tenantBrowseTableSchema.ts');
const tenantViewSource = read('src/views/TenantManagementView.vue');

const tenantColumns = [
  { key: 'code', title: '租户编码' },
  { key: 'name', title: '租户名称' },
  { key: 'status', title: '状态', formatter: (row) => row.statusLabel },
  { key: 'currentPlanName', title: '当前套餐', formatter: (row) => row.currentPlanName || row.currentPlanCode || '-' },
  { key: 'currentPlanExpiresAt', title: '套餐到期', formatter: (row) => row.currentPlanExpiresAt },
  { key: 'primaryDomain', title: '主域名', formatter: (row) => row.primaryDomain || '-' },
  { key: 'contactName', title: '联系人' },
  { key: 'contactPhone', title: '联系电话', maskType: 'phone' },
  { key: 'provisionedAt', title: '开通完成', formatter: (row) => row.provisionedAt },
  { key: 'permissionSyncStatus', title: '权限同步状态', formatter: (row) => row.permissionSyncStatusLabel },
];

function permissionKey(column) {
  return `tenant::${column.key}`;
}

function resolveColumnAccessMode(column, snapshot, strictMissing = false) {
  return snapshot[permissionKey(column)] || (strictMissing ? 'HIDDEN' : 'VISIBLE');
}

function maskValue(value, maskType = 'generic') {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const text = String(value).trim();
  if (!text) {
    return '-';
  }
  if (maskType === 'phone') {
    const digits = text.replace(/\s+/g, '');
    if (/^\d{11}$/.test(digits)) {
      return `${digits.slice(0, 3)}****${digits.slice(-4)}`;
    }
  }
  if (text.length <= 1) {
    return '*';
  }
  if (text.length <= 4) {
    return `${text.slice(0, 1)}${'*'.repeat(text.length - 1)}`;
  }
  return `${text.slice(0, 2)}***${text.slice(-2)}`;
}

function cellText(row, column, accessMode) {
  const rawValue = column.formatter ? column.formatter(row) : row[column.key];
  if (rawValue === null || rawValue === undefined || rawValue === '') {
    return '-';
  }
  if (accessMode === 'MASKED') {
    return maskValue(rawValue, column.maskType);
  }
  return String(rawValue);
}

function renderTable(row, snapshot, strictMissing = false) {
  return tenantColumns
    .map((column) => ({
      ...column,
      accessMode: resolveColumnAccessMode(column, snapshot, strictMissing),
    }))
    .filter((column) => column.accessMode !== 'HIDDEN')
    .map((column) => ({
      key: column.key,
      title: column.title,
      value: cellText(row, column, column.accessMode),
      accessMode: column.accessMode,
    }));
}

assert(tableSchemaSource.includes('resolveColumnAccessMode'), '浏览表格 schema 工具必须提供列权限访问模式解析。');
assert(tableSchemaSource.includes('maskBrowseTableValue'), '浏览表格 schema 工具必须提供统一脱敏逻辑。');
assert(tableSource.includes('displayedColumns'), 'XuanBrowseTable 必须按 effectiveVisible 渲染实际列。');
assert(tableSource.includes('accessMode !== \'HIDDEN\''), 'XuanBrowseTable 必须隐藏 HIDDEN 列。');
assert(tableSource.includes('resolveBrowseTableCellText(row, column, column.accessMode)'), 'XuanBrowseTable 默认单元格必须按访问模式渲染明文或脱敏值。');
assert(
  tenantViewSource.includes(':column-permission-snapshot="tenantColumnPermissionSnapshot"')
    && tenantViewSource.includes(':strict-column-permission-snapshot="strictColumnPermissionSnapshot"'),
  '租户管理页必须把当前用户 tenant 列权限快照传给 XuanBrowseTable。',
);
assert(
  !tenantViewSource.includes('#cell-status') && !tenantViewSource.includes('#cell-permissionSyncStatus'),
  '租户管理页不能为受列权限保护的列使用绕过默认脱敏逻辑的自定义单元格插槽。',
);

for (const column of tenantColumns) {
  assert(
    tenantSchemaSource.includes(`columnKey: '${column.key}'`),
    `租户管理表格 schema 必须登记 ${column.key} 列权限。`,
  );
}

const row = {
  code: 'tenant-006',
  name: '第六租户',
  statusLabel: '启用',
  currentPlanCode: 'basic',
  currentPlanName: '基础套餐',
  currentPlanExpiresAt: '2026-09-01 10:00:00',
  primaryDomain: 'tenant006.example.com',
  contactName: '张三',
  contactPhone: '13800000000',
  provisionedAt: '2026-08-01 10:00:00',
  permissionSyncStatusLabel: '正常',
};

const allVisibleSnapshot = Object.fromEntries(tenantColumns.map((column) => [permissionKey(column), 'VISIBLE']));
const allVisibleCells = renderTable(row, allVisibleSnapshot, true);
assert.deepEqual(
  allVisibleCells.map((cell) => cell.key),
  tenantColumns.map((column) => column.key),
  '全部 VISIBLE 时租户管理表格应展示全部已授权列。',
);
assert.equal(allVisibleCells.find((cell) => cell.key === 'contactPhone')?.value, '13800000000', 'VISIBLE 手机号应明文展示。');
assert.equal(allVisibleCells.find((cell) => cell.key === 'primaryDomain')?.value, 'tenant006.example.com', 'VISIBLE 主域名应明文展示。');

const restrictedSnapshot = {
  'tenant::code': 'VISIBLE',
  'tenant::name': 'VISIBLE',
  'tenant::status': 'VISIBLE',
  'tenant::contactPhone': 'MASKED',
  'tenant::currentPlanName': 'HIDDEN',
  'tenant::currentPlanExpiresAt': 'HIDDEN',
  'tenant::primaryDomain': 'HIDDEN',
  'tenant::contactName': 'HIDDEN',
  'tenant::provisionedAt': 'HIDDEN',
  'tenant::permissionSyncStatus': 'MASKED',
};
const restrictedCells = renderTable(row, restrictedSnapshot, true);
assert.deepEqual(
  restrictedCells.map((cell) => cell.key),
  ['code', 'name', 'status', 'contactPhone', 'permissionSyncStatus'],
  '受限快照下 HIDDEN 列不应出现在租户管理表格中。',
);
assert.equal(restrictedCells.find((cell) => cell.key === 'contactPhone')?.value, '138****0000', 'MASKED 手机号应按电话规则脱敏。');
assert.equal(restrictedCells.find((cell) => cell.key === 'permissionSyncStatus')?.value, '正*', 'MASKED 普通文本应按通用规则脱敏。');
assert(!restrictedCells.some((cell) => cell.value === '13800000000'), '受限快照下不能出现未脱敏手机号。');
assert(!restrictedCells.some((cell) => cell.value === 'tenant006.example.com'), '受限快照下不能出现被隐藏的主域名。');
assert(!restrictedCells.some((cell) => cell.value === '张三'), '受限快照下不能出现被隐藏的联系人。');

const missingRuleCells = renderTable(row, {
  'tenant::code': 'VISIBLE',
  'tenant::name': 'VISIBLE',
}, true);
assert.deepEqual(
  missingRuleCells.map((cell) => cell.key),
  ['code', 'name'],
  '严格快照下未登记的 tenant 列必须默认隐藏，不能因为缺规则回退明文。',
);

console.log('Tenant column permission table verification passed.');
