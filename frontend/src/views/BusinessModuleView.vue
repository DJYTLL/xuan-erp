<template>
  <ListPageShell :title="moduleConfig.title">
    <template #query>
      <SearchActionBar @reset="resetFilters" @search="search">
        <el-input
          v-model="filters.keyword"
          class="business-query-input"
          :placeholder="moduleConfig.searchPlaceholder"
          clearable
        />
        <el-select v-model="filters.type" class="business-query-select" placeholder="类型" clearable>
          <el-option
            v-for="option in moduleConfig.typeOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
        <el-select v-model="filters.status" class="business-query-select" placeholder="状态" clearable>
          <el-option label="启用" value="启用" />
          <el-option label="草稿" value="草稿" />
          <el-option label="待审核" value="待审核" />
        </el-select>

        <template #actions>
          <PermissionButton
            v-if="canUseAction('import')"
            :permission="permissionFor('import')"
            @click="openModuleAction('导入')"
          >
            导入
          </PermissionButton>
          <PermissionButton
            v-if="canUseAction('export')"
            :permission="permissionFor('export')"
            @click="openModuleAction('导出')"
          >
            导出
          </PermissionButton>
          <PermissionButton
            v-if="canUseAction('create')"
            type="primary"
            :permission="permissionFor('create')"
            @click="openModuleAction('新增')"
          >
            新增
          </PermissionButton>
        </template>
      </SearchActionBar>
    </template>

    <XuanBrowseTable
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      v-model:density="tableDensity"
      :page-code="moduleConfig.pageCode"
      :table-code="moduleConfig.tableCode"
      :tenant-id="browseTenantId"
      :user-id="browseUserId"
      :data="pagedRows"
      :columns="columns"
      :total="filteredRows.length"
      :selected-count="selectedRows.length"
      :actions-width="220"
      @selection-change="selectedRows = $event"
    >
      <template #toolbar-actions>
        <PermissionButton
          v-if="canUseAction('audit')"
          text
          :permission="permissionFor('audit')"
          :disabled-reason="selectedRows.length ? '' : '请先选择记录'"
          @click="openModuleAction('审核')"
        >
          批量审核
        </PermissionButton>
        <PermissionButton
          v-if="canUseAction('delete')"
          text
          type="danger"
          :permission="permissionFor('delete')"
          :disabled-reason="selectedRows.length ? '' : '请先选择记录'"
          @click="openModuleAction('批量删除')"
        >
          批量删除
        </PermissionButton>
      </template>

      <template #cell-status="{ row }">
        <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
      </template>

      <template #cell-amount="{ row }">
        {{ formatAmount(row.amount) }}
      </template>

      <template #empty>
        <AppState type="empty" :title="`${moduleConfig.title}暂无数据`" description="当前筛选条件下没有数据。" />
      </template>

      <template #actions="{ row }">
        <PermissionButton link type="primary" :permission="permissionFor('view')" @click="openRowAction('查看', row)">
          查看
        </PermissionButton>
        <PermissionButton
          v-if="canUseAction('update')"
          link
          type="primary"
          :permission="permissionFor('update')"
          @click="openRowAction('编辑', row)"
        >
          编辑
        </PermissionButton>
        <PermissionButton
          v-if="canUseAction('delete')"
          link
          type="danger"
          :permission="permissionFor('delete')"
          @click="openRowAction('删除', row)"
        >
          删除
        </PermissionButton>
      </template>
    </XuanBrowseTable>
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus/es/components/message/index';
import AppState from '@/framework/components/AppState.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import SearchActionBar from '@/framework/components/SearchActionBar.vue';
import XuanBrowseTable, { type XuanBrowseTableColumn } from '@/framework/components/XuanBrowseTable.vue';
import type { BrowseTableDensity } from '@/framework/components/browseTablePreferences';
import { useAuthStore } from '@/stores/auth';
import { useAuthorizationStore } from '@/stores/authorization';

defineOptions({ name: 'BusinessModuleView' });

type BusinessModuleCode = 'party' | 'warehouse' | 'inventory' | 'sales' | 'finance' | 'document' | 'manufacturing' | 'report';
type ModuleAction = 'view' | 'create' | 'update' | 'delete' | 'audit' | 'import' | 'export';
type BusinessStatus = '启用' | '草稿' | '待审核';

type BusinessRow = {
  index: number;
  code: string;
  name: string;
  type: string;
  status: BusinessStatus;
  owner: string;
  amount: number;
  updatedAt: string;
};

type BusinessModuleConfig = {
  code: BusinessModuleCode;
  title: string;
  permissionPrefix: string;
  pageCode: string;
  tableCode: string;
  searchPlaceholder: string;
  typeOptions: string[];
  actions: ModuleAction[];
  rows: BusinessRow[];
};

const route = useRoute();
const authStore = useAuthStore();
const authorizationStore = useAuthorizationStore();
const browseTenantId = computed(() => String(authStore.tenantId ?? '0'));
const browseUserId = computed(() => authStore.currentUser?.username || 'anonymous');

const filters = reactive({
  keyword: '',
  type: '',
  status: '',
});
const selectedRows = ref<BusinessRow[]>([]);
const tableDensity = ref<BrowseTableDensity>('default');
const currentPage = ref(1);
const pageSize = ref(10);

const columns: Array<XuanBrowseTableColumn<BusinessRow>> = [
  { key: 'index', title: '序号', width: 70 },
  { key: 'code', title: '编码', minWidth: 150 },
  { key: 'name', title: '名称', minWidth: 170 },
  { key: 'type', title: '类型', width: 120 },
  { key: 'status', title: '状态', width: 100 },
  { key: 'owner', title: '负责人', width: 120 },
  { key: 'amount', title: '金额', width: 120, align: 'right' },
  { key: 'updatedAt', title: '更新时间', width: 170 },
];

const moduleConfigs: Record<BusinessModuleCode, BusinessModuleConfig> = {
  party: {
    code: 'party',
    title: '往来单位',
    permissionPrefix: 'party',
    pageCode: 'party',
    tableCode: 'party-list',
    searchPlaceholder: '搜索单位名称 / 编码 / 联系人',
    typeOptions: ['客户', '供应商', '客户兼供应商'],
    actions: ['view', 'create', 'update', 'delete', 'import', 'export'],
    rows: createRows('PTY', '往来单位', ['客户', '供应商', '客户兼供应商']),
  },
  warehouse: {
    code: 'warehouse',
    title: '仓库资料',
    permissionPrefix: 'warehouse',
    pageCode: 'warehouse',
    tableCode: 'warehouse-list',
    searchPlaceholder: '搜索仓库 / 库位编码',
    typeOptions: ['主仓', '门店仓', '售后仓'],
    actions: ['view', 'create', 'update', 'delete', 'import', 'export'],
    rows: createRows('WHS', '仓库', ['主仓', '门店仓', '售后仓']),
  },
  inventory: {
    code: 'inventory',
    title: '库存管理',
    permissionPrefix: 'inventory',
    pageCode: 'inventory',
    tableCode: 'inventory-list',
    searchPlaceholder: '搜索库存单号 / 商品 / 仓库',
    typeOptions: ['盘点单', '调拨单', '库存调整'],
    actions: ['view', 'create', 'update', 'delete', 'audit', 'import', 'export'],
    rows: createRows('INV', '库存单', ['盘点单', '调拨单', '库存调整']),
  },
  sales: {
    code: 'sales',
    title: '销售管理',
    permissionPrefix: 'sales',
    pageCode: 'sales',
    tableCode: 'sales-list',
    searchPlaceholder: '搜索销售单号 / 客户',
    typeOptions: ['销售单', '销售退货', '收款核销'],
    actions: ['view', 'create', 'update', 'delete', 'audit', 'import', 'export'],
    rows: createRows('SAL', '销售单', ['销售单', '销售退货', '收款核销']),
  },
  finance: {
    code: 'finance',
    title: '财务管理',
    permissionPrefix: 'finance',
    pageCode: 'finance',
    tableCode: 'finance-list',
    searchPlaceholder: '搜索收付款单号 / 往来单位',
    typeOptions: ['应收', '应付', '收款', '付款'],
    actions: ['view', 'create', 'update', 'delete', 'audit', 'import', 'export'],
    rows: createRows('FIN', '财务单', ['应收', '应付', '收款', '付款']),
  },
  document: {
    code: 'document',
    title: '打印管理',
    permissionPrefix: 'document',
    pageCode: 'document',
    tableCode: 'document-list',
    searchPlaceholder: '搜索模板 / 打印任务',
    typeOptions: ['打印模板', '打印日志', '单据快照'],
    actions: ['view', 'create', 'update', 'delete', 'export'],
    rows: createRows('DOC', '打印模板', ['打印模板', '打印日志', '单据快照']),
  },
  manufacturing: {
    code: 'manufacturing',
    title: '组装拆分',
    permissionPrefix: 'manufacturing',
    pageCode: 'manufacturing',
    tableCode: 'manufacturing-list',
    searchPlaceholder: '搜索组装单 / 拆分单 / 模板',
    typeOptions: ['组装单', '拆分单', 'BOM 模板'],
    actions: ['view', 'create', 'update', 'delete', 'audit', 'import', 'export'],
    rows: createRows('MFG', '组装单', ['组装单', '拆分单', 'BOM 模板']),
  },
  report: {
    code: 'report',
    title: '报表中心',
    permissionPrefix: 'query',
    pageCode: 'report',
    tableCode: 'report-list',
    searchPlaceholder: '搜索报表 / 指标 / 数据集',
    typeOptions: ['经营报表', '库存报表', '财务报表'],
    actions: ['view', 'create', 'update', 'delete', 'export'],
    rows: createRows('RPT', '报表', ['经营报表', '库存报表', '财务报表']),
  },
};

const moduleCode = computed<BusinessModuleCode>(() => {
  const pathCode = route.path.replace(/^\//, '') as BusinessModuleCode;
  return pathCode in moduleConfigs ? pathCode : 'inventory';
});
const moduleConfig = computed(() => moduleConfigs[moduleCode.value]);
const filteredRows = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase();
  return moduleConfig.value.rows.filter((row) => {
    const matchKeyword = !keyword
      || row.code.toLowerCase().includes(keyword)
      || row.name.toLowerCase().includes(keyword)
      || row.owner.toLowerCase().includes(keyword);
    const matchType = !filters.type || row.type === filters.type;
    const matchStatus = !filters.status || row.status === filters.status;
    return matchKeyword && matchType && matchStatus;
  });
});
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

watch(
  () => route.path,
  () => {
    resetFilters();
    selectedRows.value = [];
    currentPage.value = 1;
  },
);

function createRows(prefix: string, namePrefix: string, types: string[]): BusinessRow[] {
  return Array.from({ length: 18 }, (_, index) => ({
    index: index + 1,
    code: `${prefix}-${String(index + 1).padStart(4, '0')}`,
    name: `${namePrefix}${index + 1}`,
    type: types[index % types.length],
    status: index % 5 === 0 ? '待审核' : index % 3 === 0 ? '草稿' : '启用',
    owner: ['张三', '李四', '王五'][index % 3],
    amount: 1200 + index * 328,
    updatedAt: `2026-07-${String(10 + (index % 18)).padStart(2, '0')} 09:30`,
  }));
}

function resetFilters() {
  filters.keyword = '';
  filters.type = '';
  filters.status = '';
  currentPage.value = 1;
}

function search() {
  currentPage.value = 1;
}

function permissionFor(action: ModuleAction) {
  return `${moduleConfig.value.permissionPrefix}:${action}`;
}

function canUseAction(action: ModuleAction) {
  return moduleConfig.value.actions.includes(action) && authorizationStore.hasButtonPermission(permissionFor(action));
}

function openModuleAction(actionName: string) {
  ElMessage.success(`${moduleConfig.value.title}${actionName}`);
}

function openRowAction(actionName: string, row: BusinessRow) {
  ElMessage.success(`${actionName} ${row.code}`);
}

function statusTagType(status: BusinessStatus) {
  if (status === '启用') {
    return 'success';
  }
  if (status === '待审核') {
    return 'warning';
  }
  return 'info';
}

function formatAmount(amount: number) {
  return amount.toLocaleString('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  });
}
</script>

<style scoped>
.business-query-input {
  width: 420px;
}

.business-query-select {
  width: 180px;
}
</style>
