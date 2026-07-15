<template>
  <ListPageShell title="审计日志" description="查询 audit_log 中的登录、操作和跨租户审计记录，定位业务行为证据。">
    <template #query>
      <SearchActionBar search-text="查询" reset-tooltip="重置查询条件" :loading="loading" @reset="resetFilters" @search="loadLogs">
        <label class="search-action-field audit-tenant-field">
          <span class="search-action-field__label">租户ID</span>
          <XuanDecimalInput
            v-model="tenantId"
            class="tenant-input"
            :scale="0"
            input-mode="numeric"
            placeholder="1001"
          />
        </label>
        <label class="search-action-field audit-actor-field">
          <span class="search-action-field__label">操作人</span>
          <el-input v-model="actorUsername" class="actor-input" placeholder="用户名" clearable />
        </label>
        <label class="search-action-field audit-action-field">
          <span class="search-action-field__label">动作</span>
          <el-input v-model="action" class="action-input" placeholder="iam:auth:login-success" clearable />
        </label>
        <label class="search-action-field audit-entity-field">
          <span class="search-action-field__label">实体</span>
          <el-input v-model="entityType" class="entity-input" placeholder="IamUser" clearable />
        </label>
        <label class="search-action-field audit-status-field">
          <span class="search-action-field__label">状态</span>
          <el-select v-model="status" class="status-select" clearable placeholder="全部">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </label>
        <label class="search-action-field audit-time-field">
          <span class="search-action-field__label">时间区间</span>
          <XuanDateTimeRangePicker
            v-model="timeRange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            custom-class="time-range"
          />
        </label>
        <label class="search-action-field audit-limit-field">
          <span class="search-action-field__label">条数</span>
          <XuanDecimalInput
            v-model="limit"
            class="limit-input"
            :scale="0"
            input-mode="numeric"
            placeholder="20"
          />
        </label>
      </SearchActionBar>
    </template>

    <el-table v-loading="loading" :data="rows" row-key="id" border>
      <el-table-column label="时间" min-width="180">
        <template #default="{ row }">
          {{ formatChinaDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'FAILED' ? 'danger' : 'success'">{{ row.status === 'FAILED' ? '失败' : '成功' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="tenantId" label="租户" width="90" />
      <el-table-column prop="actorUsername" label="操作人" min-width="120" show-overflow-tooltip />
      <el-table-column prop="action" label="动作" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <code>{{ row.action }}</code>
        </template>
      </el-table-column>
      <el-table-column label="实体" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.entityType }}</span>
          <span v-if="row.entityId" class="muted-text">#{{ row.entityId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="请求" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.method" class="method-pill">{{ row.method }}</span>
          <span>{{ row.path || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="errorCode" label="错误码" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <AppState
      v-if="!loading && errorMessage"
      type="error"
      title="审计日志查询失败"
      :description="errorMessage"
    />

    <AppState
      v-else-if="!loading && rows.length === 0"
      type="empty"
      title="暂无审计日志"
      description="请调整查询条件，或确认业务服务已经通过 xuan-audit 写入 audit_log。"
    />

    <el-drawer v-model="detailVisible" title="审计日志详情" size="680px">
      <dl v-if="currentLog" class="audit-detail-list">
        <div v-for="item in detailItems" :key="item.label" class="audit-detail-item">
          <dt>{{ item.label }}</dt>
          <dd>{{ item.value || '-' }}</dd>
        </div>
      </dl>
    </el-drawer>
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { getHttpErrorMessage } from '@/api/http-error';
import { listAuditLogs } from '@/api/auditLogs';
import AppState from '@/framework/components/AppState.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import SearchActionBar from '@/framework/components/SearchActionBar.vue';
import XuanDateTimeRangePicker, { type DateRangeValue } from '@/framework/components/XuanDateTimeRangePicker.vue';
import XuanDecimalInput from '@/framework/components/XuanDecimalInput.vue';
import type { AuditLogEntry, AuditLogStatus } from '@/types/auditLog';

defineOptions({ name: 'AuditLogQueryView' });

const tenantId = ref('');
const actorUsername = ref('');
const action = ref('');
const entityType = ref('');
const status = ref<AuditLogStatus | ''>('');
const timeRange = ref<DateRangeValue>(null);
const limit = ref('20');
const loading = ref(false);
const errorMessage = ref('');
const rows = ref<AuditLogEntry[]>([]);
const detailVisible = ref(false);
const currentLog = ref<AuditLogEntry | null>(null);

const detailItems = computed(() => {
  const log = currentLog.value;
  if (!log) {
    return [];
  }
  return [
    { label: 'ID', value: String(log.id) },
    { label: '租户', value: String(log.tenantId) },
    { label: '操作人', value: log.actorUsername },
    { label: '操作人ID', value: formatOptional(log.actorUserId) },
    { label: '动作', value: log.action },
    { label: '实体类型', value: log.entityType },
    { label: '实体ID', value: log.entityId },
    { label: '状态', value: log.status },
    { label: '请求ID', value: log.requestId },
    { label: '客户端IP', value: log.clientIp },
    { label: 'User-Agent', value: log.userAgent },
    { label: '耗时(ms)', value: formatOptional(log.durationMs) },
    { label: '方法', value: log.method },
    { label: '路径', value: log.path },
    { label: 'HTTP状态', value: formatOptional(log.httpStatus) },
    { label: '错误码', value: log.errorCode },
    { label: '错误信息', value: log.errorMessage },
    { label: '授权租户', value: formatOptional(log.authTenantId) },
    { label: '跨租户', value: log.crossTenant ? '是' : '否' },
    { label: '详情', value: log.detail },
    { label: '创建时间', value: formatChinaDateTime(log.createdAt) },
  ];
});

async function loadLogs() {
  loading.value = true;
  errorMessage.value = '';
  try {
    rows.value = await listAuditLogs({
      tenantId: normalizeInteger(tenantId.value),
      actorUsername: actorUsername.value || undefined,
      action: action.value || undefined,
      entityType: entityType.value || undefined,
      status: status.value || undefined,
      startTime: normalizeDateTime(getTimeRangeText(0)),
      endTime: normalizeDateTime(getTimeRangeText(1)),
      limit: normalizeLimit(limit.value),
    });
  } catch (error) {
    rows.value = [];
    errorMessage.value = getHttpErrorMessage(error);
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  tenantId.value = '';
  actorUsername.value = '';
  action.value = '';
  entityType.value = '';
  status.value = '';
  timeRange.value = null;
  limit.value = '20';
}

function openDetail(log: AuditLogEntry) {
  currentLog.value = log;
  detailVisible.value = true;
}

function getTimeRangeText(index: 0 | 1) {
  const value = timeRange.value?.[index];
  return typeof value === 'string' ? value : undefined;
}

function normalizeDateTime(value?: string) {
  if (!value) {
    return undefined;
  }
  return `${value.replace(' ', 'T')}+08:00`;
}

function normalizeInteger(value: string | number | null | undefined) {
  const parsed = Number.parseInt(String(value ?? ''), 10);
  return Number.isNaN(parsed) ? undefined : parsed;
}

function normalizeLimit(value: string | number | null | undefined) {
  const parsed = Number.parseInt(String(value ?? ''), 10);
  if (Number.isNaN(parsed)) {
    return 20;
  }
  return Math.min(100, Math.max(1, parsed));
}

function formatOptional(value?: number) {
  return value === undefined || value === null ? undefined : String(value);
}

function formatChinaDateTime(value?: string) {
  if (!value) {
    return undefined;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const parts = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).formatToParts(date);

  const getPart = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? '';
  return `${getPart('year')}-${getPart('month')}-${getPart('day')} ${getPart('hour')}:${getPart('minute')}:${getPart('second')}`;
}
</script>

<style scoped>
.tenant-input,
.actor-input,
.action-input,
.entity-input,
.status-select,
.time-range,
.limit-input {
  width: 100%;
}

.audit-tenant-field {
  width: 110px;
  flex-basis: 110px;
}

.audit-actor-field {
  width: 150px;
  flex-basis: 150px;
}

.audit-action-field {
  width: 240px;
  flex-basis: 240px;
}

.audit-entity-field {
  width: 140px;
  flex-basis: 140px;
}

.audit-status-field {
  width: 110px;
  flex-basis: 110px;
}

.audit-time-field {
  width: 430px;
  flex-basis: 430px;
}

.audit-time-field :deep(.xuan-date-time-range-picker.el-range-editor) {
  width: 430px !important;
  min-width: 430px !important;
}

.audit-limit-field {
  width: 96px;
  flex-basis: 96px;
}

code {
  color: #334155;
  font-size: 12px;
  white-space: nowrap;
}

.muted-text {
  margin-left: 4px;
  color: var(--xuan-muted);
}

.method-pill {
  display: inline-flex;
  align-items: center;
  height: 20px;
  margin-right: 6px;
  border-radius: 4px;
  padding: 0 6px;
  color: #166534;
  background: #dcfce7;
  font-size: 12px;
  font-weight: 600;
}

.audit-detail-list {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 0;
  margin: 0;
}

.audit-detail-item {
  display: contents;
}

.audit-detail-item dt,
.audit-detail-item dd {
  border-bottom: 1px solid var(--xuan-border);
  padding: 10px 0;
}

.audit-detail-item dt {
  color: var(--xuan-muted);
}

.audit-detail-item dd {
  margin: 0;
  color: var(--xuan-text);
  overflow-wrap: anywhere;
}

@media (max-width: 860px) {
  .audit-time-field :deep(.xuan-date-time-range-picker.el-range-editor) {
    width: 100% !important;
    min-width: 0 !important;
  }
}
</style>
