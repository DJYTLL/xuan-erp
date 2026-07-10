<template>
  <ListPageShell :title="pageTitle">
    <template #query>
      <QueryToolbar>
        <el-input v-model="filters.keyword" class="purchase-query-input" placeholder="搜索" clearable />
        <el-select v-model="filters.supplier" class="purchase-query-input" placeholder="供应商" clearable>
          <el-option label="全部供应商" value="all" />
          <el-option label="华东供应商" value="华东供应商" />
          <el-option label="华南供应商" value="华南供应商" />
        </el-select>
        <el-date-picker
          v-model="filters.range"
          class="purchase-date-range"
          type="daterange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
        />

        <template #actions>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <PermissionButton v-if="mode === 'draft'" type="primary" permission="purchase:create" @click="openCreateOrder">新增</PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <DataTableShell>
      <template #toolbar>
        <span v-if="selectedRows.length" class="table-selected-count">已选 {{ selectedRows.length }} 项</span>
        <el-select v-model="tableDensity" class="table-density-select" size="small" placeholder="密度">
          <el-option label="默认" value="default" />
          <el-option label="紧凑" value="small" />
          <el-option label="宽松" value="large" />
        </el-select>
        <el-dropdown trigger="click">
          <el-button text>列设置</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="column in columnOptions" :key="column.key">
                <el-checkbox v-model="visibleColumns[column.key]">{{ column.label }}</el-checkbox>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button text @click="exportRows">导出</el-button>
        <el-button v-if="mode === 'draft'" text type="primary" :disabled="!selectedRows.length" @click="openBatchSubmit">批量提交</el-button>
      </template>

      <el-table :data="pagedOrders" :size="tableDensity" height="520" border @selection-change="selectedRows = $event">
        <template #empty>
          <AppState type="empty" title="暂无采购单" description="当前筛选条件下没有采购单数据。" />
        </template>
        <el-table-column type="selection" width="46" />
        <el-table-column v-if="visibleColumns.index" prop="index" label="序号" width="70" />
        <el-table-column v-if="visibleColumns.orderNo" prop="orderNo" label="单号" width="160" />
        <el-table-column v-if="visibleColumns.supplier" prop="supplier" label="供应商" width="180" />
        <el-table-column v-if="visibleColumns.status" prop="statusText" label="单据状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.statusType" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.buyer" prop="buyer" label="采购员" width="110" />
        <el-table-column v-if="visibleColumns.amount" prop="amount" label="金额" width="120" />
        <el-table-column v-if="visibleColumns.createdAt" prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column label="操作" fixed="right" width="190">
          <template #default="{ row }">
            <el-button link type="primary" @click="openOrderDetail(row)">查看</el-button>
            <el-button v-if="mode === 'draft'" link type="primary" @click="openApproval('submit', row)">提交</el-button>
            <el-dropdown v-if="mode === 'draft'" trigger="click">
              <el-button link type="primary">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="openApproval('approve', row)">审核通过</el-dropdown-item>
                  <el-dropdown-item @click="openApproval('reject', row)">驳回</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <span>共 {{ orders.length }} 条</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          layout="sizes, prev, pager, next"
          :total="orders.length"
          :page-sizes="[10, 20, 50]"
        />
      </template>
    </DataTableShell>

    <DetailDrawer
      v-model="detailVisible"
      title="采购单详情"
      :items="orderDetailItems"
      :record="orderDetailRecord"
      width="560px"
    />
    <ApprovalConfirmDialog
      v-model="approvalVisible"
      :action="approvalAction"
      :target-name="activeOrder?.orderNo || ''"
      :require-remark="approvalAction === 'reject'"
      @confirm="confirmApproval"
    />
    <BatchConfirmDialog
      v-model="batchVisible"
      title="批量提交确认"
      action-name="批量提交"
      tone="primary"
      :selected-count="selectedRows.length"
      :items="selectedRows.map((row) => row.orderNo)"
      @confirm="confirmBatchSubmit"
    />
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import ApprovalConfirmDialog from '@/components/business/ApprovalConfirmDialog.vue';
import AppState from '@/components/business/AppState.vue';
import BatchConfirmDialog from '@/components/business/BatchConfirmDialog.vue';
import DataTableShell from '@/components/business/DataTableShell.vue';
import DetailDrawer from '@/components/business/DetailDrawer.vue';
import ListPageShell from '@/components/business/ListPageShell.vue';
import PermissionButton from '@/components/business/PermissionButton.vue';
import QueryToolbar from '@/components/business/QueryToolbar.vue';

defineOptions({ name: 'PurchaseOrderView' });

type OrderRow = {
  index: number;
  orderNo: string;
  supplier: string;
  statusText: string;
  statusType: '' | 'success' | 'warning';
  buyer: string;
  amount: string;
  createdAt: string;
};

type ColumnKey = 'index' | 'orderNo' | 'supplier' | 'status' | 'buyer' | 'amount' | 'createdAt';

const route = useRoute();
const router = useRouter();
const mode = computed(() => String(route.meta.purchaseMode || 'draft'));
const pageTitle = computed(() => String(route.meta.title || '采购单'));

const filters = reactive({
  keyword: '',
  supplier: '',
  range: '',
});

const columnOptions: Array<{ key: ColumnKey; label: string }> = [
  { key: 'index', label: '序号' },
  { key: 'orderNo', label: '单号' },
  { key: 'supplier', label: '供应商' },
  { key: 'status', label: '单据状态' },
  { key: 'buyer', label: '采购员' },
  { key: 'amount', label: '金额' },
  { key: 'createdAt', label: '创建时间' },
];

const orderDetailItems = [
  { key: 'orderNo', label: '单号' },
  { key: 'supplier', label: '供应商' },
  { key: 'statusText', label: '单据状态' },
  { key: 'buyer', label: '采购员' },
  { key: 'amount', label: '金额' },
  { key: 'createdAt', label: '创建时间' },
];

const visibleColumns = reactive<Record<ColumnKey, boolean>>(Object.fromEntries(
  columnOptions.map((column) => [column.key, true]),
) as Record<ColumnKey, boolean>);

const selectedRows = ref<OrderRow[]>([]);
const tableDensity = ref(String(route.query.density || 'default'));
const currentPage = ref(Number(route.query.page || 1));
const pageSize = ref(Number(route.query.pageSize || 10));
const detailVisible = ref(false);
const approvalVisible = ref(false);
const batchVisible = ref(false);
const activeOrder = ref<OrderRow | null>(null);
const approvalAction = ref<'approve' | 'reject' | 'submit'>('submit');

const statusMap: Record<string, { text: string; type: '' | 'success' | 'warning' }> = {
  draft: { text: '草稿', type: 'warning' },
  approved: { text: '已审核', type: 'success' },
  returnDraft: { text: '退货草稿', type: 'warning' },
  returnApproved: { text: '退货已审核', type: 'success' },
};

const orders = computed<OrderRow[]>(() => {
  const status = statusMap[mode.value] || statusMap.draft;
  return Array.from({ length: 26 }, (_, index) => ({
    index: index + 1,
    orderNo: `PO-${mode.value.toUpperCase()}-${String(index + 1).padStart(4, '0')}`,
    supplier: index % 2 === 0 ? '华东供应商' : '华南供应商',
    statusText: status.text,
    statusType: status.type,
    buyer: index % 2 === 0 ? '张三' : '李四',
    amount: `¥${(1860 + index * 328).toLocaleString()}`,
    createdAt: `2026-07-${String(10 + (index % 18)).padStart(2, '0')} 09:30`,
  }));
});

const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return orders.value.slice(start, start + pageSize.value);
});

const orderDetailRecord = computed<Record<string, unknown>>(() => {
  return activeOrder.value ? { ...activeOrder.value } : {};
});

watch([currentPage, pageSize, tableDensity], () => {
  router.replace({
    query: {
      ...route.query,
      page: String(currentPage.value),
      pageSize: String(pageSize.value),
      density: tableDensity.value,
    },
  });
});

function resetFilters() {
  Object.assign(filters, {
    keyword: '',
    supplier: '',
    range: '',
  });
}

function search() {
  currentPage.value = 1;
  return filters;
}

function exportRows() {
  const rows = selectedRows.value.length ? selectedRows.value : orders.value;
  ElMessage.success(`已准备导出 ${rows.length} 条采购单数据`);
}

function openCreateOrder() {
  ElMessage.info('新增采购单后续可接入动态表单弹窗或单据编辑页');
}

function openOrderDetail(row: OrderRow) {
  activeOrder.value = row;
  detailVisible.value = true;
}

function openApproval(action: 'approve' | 'reject' | 'submit', row: OrderRow) {
  approvalAction.value = action;
  activeOrder.value = row;
  approvalVisible.value = true;
}

function confirmApproval(value: { action: 'approve' | 'reject' | 'submit'; remark: string }) {
  approvalVisible.value = false;
  const actionText = value.action === 'approve' ? '审核通过' : value.action === 'reject' ? '驳回' : '提交';
  ElMessage.success(`${activeOrder.value?.orderNo || '采购单'}已${actionText}`);
}

function openBatchSubmit() {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择采购单');
    return;
  }
  batchVisible.value = true;
}

function confirmBatchSubmit() {
  const count = selectedRows.value.length;
  batchVisible.value = false;
  selectedRows.value = [];
  ElMessage.success(`已批量提交 ${count} 张采购单`);
}
</script>
