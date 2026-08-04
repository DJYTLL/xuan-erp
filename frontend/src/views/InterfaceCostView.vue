<template>
  <ListPageShell title="接口耗时" description="通过 xuan-audit 查询 SkyWalking OAP，定位接口耗时、trace 和 span 明细。">
    <template #query>
      <SearchActionBar search-text="查询" reset-tooltip="重置查询条件" :loading="loading" @reset="resetFilters" @search="loadTraces">
        <label class="search-action-field interface-service-field">
          <span class="search-action-field__label">服务</span>
          <el-input v-model="serviceName" class="service-input" placeholder="例如 xuan-iam" clearable />
        </label>
        <label class="search-action-field interface-endpoint-field">
          <span class="search-action-field__label">接口</span>
          <el-input v-model="endpointName" class="endpoint-input" placeholder="接口路径 / endpoint" clearable />
        </label>
        <label class="search-action-field interface-time-field">
          <span class="search-action-field__label">时间区间</span>
          <XuanDateTimeRangePicker
            v-model="timeRange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            custom-class="time-range"
          />
        </label>
        <label class="search-action-field interface-limit-field">
          <span class="search-action-field__label">条数</span>
          <XuanDecimalInput
            v-model="limit"
            class="limit-input"
            :scale="0"
            input-mode="numeric"
            placeholder="20"
          />
        </label>
        <template #actions>
          <PermissionButton :icon="ExternalLink" permission="audit:interface-cost:view" no-permission-mode="disable" @click="openSkyWalking">SkyWalking</PermissionButton>
        </template>
      </SearchActionBar>
    </template>

    <el-table v-loading="loading" :data="traces" row-key="traceId" border>
      <el-table-column prop="serviceName" label="服务" min-width="140" />
      <el-table-column prop="endpointName" label="接口 / Endpoint" min-width="260" show-overflow-tooltip />
      <el-table-column prop="durationMs" label="耗时(ms)" width="120" sortable />
      <el-table-column prop="startTime" label="开始时间" min-width="170" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.error ? 'danger' : 'success'">{{ row.error ? '异常' : '正常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="traceId" label="Trace ID" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <PermissionButton link type="primary" permission="audit:interface-cost:view" no-permission-mode="disable" @click="openTrace(row.traceId)">Span 明细</PermissionButton>
        </template>
      </el-table-column>
    </el-table>

    <AppState
      v-if="!loading && errorMessage"
      type="error"
      title="接口耗时查询失败"
      :description="errorMessage"
    />

    <AppState
      v-else-if="!loading && traces.length === 0"
      type="empty"
      title="暂无接口耗时数据"
      description="请确认服务已通过 SkyWalking Java Agent 上报到 OAP，并选择正确的时间范围。"
    />

    <el-drawer v-model="detailVisible" title="Trace Span 明细" size="720px">
      <div class="trace-detail-header">
        <span>Trace ID</span>
        <code>{{ currentTrace?.traceId }}</code>
      </div>
      <el-table v-loading="detailLoading" :data="currentTrace?.spans || []" row-key="spanId" border>
        <el-table-column prop="spanId" label="Span" width="90" />
        <el-table-column prop="parentSpanId" label="Parent" width="90" />
        <el-table-column prop="serviceName" label="服务" min-width="130" />
        <el-table-column prop="endpointName" label="Endpoint" min-width="220" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="110" sortable />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.error ? 'danger' : 'success'">{{ row.error ? '异常' : '正常' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </ListPageShell>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { ExternalLink } from 'lucide-vue-next';
import { getHttpErrorMessage } from '@/api/http-error';
import { getInterfaceTrace, listInterfaceTraces } from '@/api/observability';
import AppState from '@/framework/components/AppState.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import SearchActionBar from '@/framework/components/SearchActionBar.vue';
import XuanDateTimeRangePicker, { type DateRangeValue } from '@/framework/components/XuanDateTimeRangePicker.vue';
import XuanDecimalInput from '@/framework/components/XuanDecimalInput.vue';
import type { InterfaceTraceDetail, InterfaceTraceSummary } from '@/types/observability';

defineOptions({ name: 'InterfaceCostView' });

const serviceName = ref('');
const endpointName = ref('');
const timeRange = ref<DateRangeValue>(null);
const limit = ref('20');
const loading = ref(false);
const errorMessage = ref('');
const detailLoading = ref(false);
const detailVisible = ref(false);
const traces = ref<InterfaceTraceSummary[]>([]);
const currentTrace = ref<InterfaceTraceDetail | null>(null);

async function loadTraces() {
  loading.value = true;
  errorMessage.value = '';
  try {
    traces.value = await listInterfaceTraces({
      serviceName: serviceName.value || undefined,
      endpointName: endpointName.value || undefined,
      startTime: getTimeRangeText(0),
      endTime: getTimeRangeText(1),
      limit: normalizeLimit(limit.value),
    });
  } catch (error) {
    traces.value = [];
    errorMessage.value = getHttpErrorMessage(error);
  } finally {
    loading.value = false;
  }
}

async function openTrace(traceId: string) {
  if (!traceId) {
    ElMessage.warning('当前记录缺少 traceId');
    return;
  }
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    currentTrace.value = await getInterfaceTrace(traceId);
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error));
    detailVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
}

function resetFilters() {
  serviceName.value = '';
  endpointName.value = '';
  timeRange.value = null;
  limit.value = '20';
}

function getTimeRangeText(index: 0 | 1) {
  const value = timeRange.value?.[index];
  return typeof value === 'string' ? value : undefined;
}

function normalizeLimit(value: string | number | null | undefined) {
  const parsed = Number.parseInt(String(value ?? ''), 10);
  if (Number.isNaN(parsed)) {
    return 20;
  }

  return Math.min(100, Math.max(1, parsed));
}

function openSkyWalking() {
  window.open('http://duaoyunxuan.top:9026', '_blank', 'noopener,noreferrer');
}
</script>

<style scoped>
.time-range {
  width: 100%;
}

.limit-input {
  width: 100%;
}

.service-input,
.endpoint-input {
  width: 100%;
}

.interface-service-field {
  width: 210px;
  flex-basis: 210px;
}

.interface-endpoint-field {
  width: 280px;
  flex-basis: 280px;
}

.interface-time-field {
  width: 430px;
  flex-basis: 430px;
}

.interface-time-field :deep(.xuan-date-time-range-picker.el-range-editor) {
  width: 430px !important;
  min-width: 430px !important;
}

.interface-limit-field {
  width: 96px;
  flex-basis: 96px;
}

.trace-detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: var(--xuan-muted);
}

code {
  border-radius: 4px;
  padding: 2px 6px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 12px;
}

@media (max-width: 860px) {
  .time-range {
    width: 100%;
  }

  .limit-input {
    width: 100%;
  }

  .interface-time-field :deep(.xuan-date-time-range-picker.el-range-editor) {
    width: 100% !important;
    min-width: 0 !important;
  }
}
</style>
