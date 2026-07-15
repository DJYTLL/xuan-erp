import { readFileSync } from 'node:fs';

function read(path) {
  return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

function assertIncludes(content, expected, message) {
  if (!content.includes(expected)) {
    throw new Error(message);
  }
}

const view = read('src/views/AuditLogQueryView.vue');
const api = read('src/api/auditLogs.ts');
const types = read('src/types/auditLog.ts');
const router = read('src/router/index.ts');
const navigation = read('src/config/navigation.ts');
const messages = read('src/i18n/messages.ts');

for (const component of [
  'ListPageShell',
  'SearchActionBar',
  'XuanDateTimeRangePicker',
  'XuanDecimalInput',
  'AppState',
  'el-table',
]) {
  assertIncludes(view, component, `审计日志查询页必须复用本地组件或现有 Element Plus 表格：${component}`);
}

assertIncludes(api, "'/api/audit/logs'", '审计日志查询 API 必须对接 GET /api/audit/logs');
assertIncludes(api, 'status: params.status || undefined', '审计日志查询 API 必须支持状态过滤');
assertIncludes(types, 'export interface AuditLogEntry', '必须定义 AuditLogEntry 类型');
assertIncludes(types, 'export interface AuditLogQueryParams', '必须定义 AuditLogQueryParams 类型');
assertIncludes(router, "name: 'audit-logs'", '必须注册 audit-logs 路由');
assertIncludes(router, "permission: 'audit:log:view'", '审计日志页必须绑定 audit:log:view 权限');
assertIncludes(navigation, "'audit-logs'", '导航元数据必须包含 audit-logs');
assertIncludes(messages, 'auditLogs', 'i18n 必须包含审计日志标题');
assertIncludes(view, 'formatChinaDateTime', '审计日志页必须将后端标准时间格式化为中国本地时间展示');
assertIncludes(view, '{{ formatChinaDateTime(row.createdAt) }}', '审计日志表格时间列必须展示中国时间格式');
assertIncludes(view, "value: formatChinaDateTime(log.createdAt)", '审计日志详情创建时间必须展示中国时间格式');

console.log('OK: audit log query page wiring verified');
