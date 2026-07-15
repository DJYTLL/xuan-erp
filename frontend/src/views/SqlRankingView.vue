<template>
  <ListPageShell title="SQL 排名" description="通过 xuan-audit 查询 PostgreSQL pg_stat_statements，按数据库查看 SQL 耗时排名。">
    <template #query>
      <SearchActionBar search-text="查询" reset-tooltip="重置查询条件" :loading="loading" @reset="resetFilters" @search="loadRankings">
        <label class="search-action-field sql-database-field">
          <span class="search-action-field__label">数据库</span>
          <el-select v-model="databaseNames" class="database-select" multiple collapse-tags collapse-tags-tooltip>
            <el-option v-for="item in databaseOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="search-action-field sql-sort-field">
          <span class="search-action-field__label">排序</span>
          <el-select v-model="sortBy" class="sort-select">
            <el-option label="总耗时" value="total_exec_time" />
            <el-option label="平均耗时" value="mean_exec_time" />
            <el-option label="最大耗时" value="max_exec_time" />
            <el-option label="调用次数" value="calls" />
            <el-option label="返回行数" value="rows" />
          </el-select>
        </label>
        <label class="search-action-field sql-limit-field">
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

    <el-table v-loading="loading" :data="rankings" row-key="rankingKey" border>
      <el-table-column prop="databaseName" label="数据库" width="150" />
      <el-table-column prop="calls" label="调用次数" width="110" sortable />
      <el-table-column prop="totalExecTime" label="总耗时(ms)" width="130" sortable>
        <template #default="{ row }">{{ formatNumber(row.totalExecTime) }}</template>
      </el-table-column>
      <el-table-column prop="meanExecTime" label="平均(ms)" width="120" sortable>
        <template #default="{ row }">{{ formatNumber(row.meanExecTime) }}</template>
      </el-table-column>
      <el-table-column prop="maxExecTime" label="最大(ms)" width="120" sortable>
        <template #default="{ row }">{{ formatNumber(row.maxExecTime) }}</template>
      </el-table-column>
      <el-table-column prop="rows" label="返回行数" width="110" sortable />
      <el-table-column prop="query" label="SQL 摘要" min-width="360" show-overflow-tooltip>
        <template #default="{ row }">
          <code>{{ row.query }}</code>
        </template>
      </el-table-column>
    </el-table>

    <AppState
      v-if="!loading && errorMessage"
      type="error"
      title="SQL 排名查询失败"
      :description="errorMessage"
    />

    <AppState
      v-else-if="!loading && rankings.length === 0"
      type="empty"
      title="暂无 SQL 排名数据"
      description="请确认目标数据库已启用 pg_stat_statements，并且服务已经产生 SQL 调用。"
    />
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { getHttpErrorMessage } from '@/api/http-error';
import { listSqlRankings } from '@/api/observability';
import AppState from '@/framework/components/AppState.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import SearchActionBar from '@/framework/components/SearchActionBar.vue';
import XuanDecimalInput from '@/framework/components/XuanDecimalInput.vue';
import type { SqlRankingEntry } from '@/types/observability';

defineOptions({ name: 'SqlRankingView' });

const databaseOptions = [
  'xuan_iam',
  'xuan_tenant',
  'xuan_audit',
  'xuan_product',
  'xuan_party',
  'xuan_warehouse',
  'xuan_inventory',
  'xuan_sales',
  'xuan_procurement',
  'xuan_finance',
  'xuan_document',
  'xuan_manufacturing',
  'xuan_query',
];

const databaseNames = ref<string[]>([]);
const sortBy = ref('total_exec_time');
const limit = ref('20');
const loading = ref(false);
const errorMessage = ref('');
const rows = ref<SqlRankingEntry[]>([]);

const rankings = computed(() => rows.value.map((row, index) => ({
  ...row,
  rankingKey: `${row.databaseName}-${index}-${row.query.slice(0, 24)}`,
})));

async function loadRankings() {
  loading.value = true;
  errorMessage.value = '';
  try {
    rows.value = await listSqlRankings({
      databaseNames: normalizeDatabaseNames(databaseNames.value),
      sortBy: sortBy.value,
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
  databaseNames.value = [];
  sortBy.value = 'total_exec_time';
  limit.value = '20';
}

function normalizeDatabaseNames(value: string[]) {
  return value.length ? value : undefined;
}

function normalizeLimit(value: string | number | null | undefined) {
  const parsed = Number.parseInt(String(value ?? ''), 10);
  if (Number.isNaN(parsed)) {
    return 20;
  }

  return Math.min(100, Math.max(1, parsed));
}

function formatNumber(value: number) {
  return Number(value || 0).toFixed(2);
}
</script>

<style scoped>
.database-select {
  width: 100%;
}

.sort-select {
  width: 100%;
}

.limit-input {
  width: 100%;
}

.sql-database-field {
  width: 360px;
  flex-basis: 360px;
}

.sql-sort-field {
  width: 150px;
  flex-basis: 150px;
}

.sql-limit-field {
  width: 96px;
  flex-basis: 96px;
}

code {
  color: #334155;
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 860px) {
  .database-select,
  .sort-select,
  .limit-input {
    width: 100%;
  }
}
</style>
