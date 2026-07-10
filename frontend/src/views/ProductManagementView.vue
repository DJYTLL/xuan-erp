<template>
  <ListPageShell :title="t('nav.products')">
    <template #query>
      <QueryToolbar>
        <el-input v-model="filters.name" class="query-input" placeholder="名称" clearable />
        <el-input v-model="filters.code" class="query-input" placeholder="编码" clearable />
        <el-input v-model="filters.shortName" class="query-input" placeholder="简称" clearable />
        <el-input v-model="filters.barcode" class="query-input" placeholder="条码" clearable />
        <el-select v-model="filters.category" class="query-input" placeholder="分类" clearable>
          <el-option label="分类1" value="分类1" />
          <el-option label="分类2" value="分类2" />
        </el-select>
        <el-select v-model="filters.status" class="query-input" placeholder="全部" clearable>
          <el-option label="全部" value="all" />
          <el-option label="启用" value="enabled" />
        </el-select>

        <template #actions>
          <el-button :icon="RefreshCw" circle @click="resetFilters" />
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button>导入</el-button>
          <el-button>导入结果</el-button>
          <PermissionButton type="primary" permission="product:create" @click="openCreateProduct">新增</PermissionButton>
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
          <el-button text :icon="SlidersHorizontal">列设置</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="column in columnOptions" :key="column.key">
                <el-checkbox v-model="visibleColumns[column.key]">{{ column.label }}</el-checkbox>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button text @click="exportRows">导出</el-button>
        <el-button text type="danger" :disabled="!selectedRows.length" @click="openDeleteProducts(selectedRows)">批量删除</el-button>
      </template>

      <el-table :data="pagedProducts" :size="tableDensity" height="520" border @selection-change="selectedRows = $event">
        <template #empty>
          <AppState type="empty" title="暂无商品" description="当前筛选条件下没有商品数据。" />
        </template>
        <el-table-column type="selection" width="46" />
        <el-table-column v-if="visibleColumns.index" prop="index" label="序号" width="70" />
        <el-table-column v-if="visibleColumns.code" prop="code" label="编码" width="120" />
        <el-table-column v-if="visibleColumns.name" prop="name" label="名称" width="120" />
        <el-table-column v-if="visibleColumns.factoryCode" prop="factoryCode" label="厂家编码" width="140" />
        <el-table-column v-if="visibleColumns.factoryModel" prop="factoryModel" label="厂家型号" width="140" />
        <el-table-column v-if="visibleColumns.factoryName" prop="factoryName" label="厂家名称" width="140" />
        <el-table-column v-if="visibleColumns.supplier" prop="supplier" label="来源供应商" width="150" />
        <el-table-column v-if="visibleColumns.type" label="商品类型" width="120">
          <template #default>
            <el-tag size="small">普通商品</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.category" prop="category" label="分类" width="100" />
        <el-table-column v-if="visibleColumns.unit" prop="unit" label="单位" width="90" />
        <el-table-column v-if="visibleColumns.warehouse" prop="warehouse" label="默认仓库" width="120" />
        <el-table-column v-if="visibleColumns.position" prop="position" label="默认库位" width="120" />
        <el-table-column v-if="visibleColumns.price" prop="price" label="价格" width="90" />
        <el-table-column v-if="visibleColumns.cost" prop="cost" label="成本" width="90" />
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openProductDetail(row)">查看</el-button>
            <el-button link type="primary" @click="openEditProduct(row)">编辑</el-button>
            <el-button link type="danger" @click="openDeleteProducts([row])">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <span>共 {{ products.length }} 条</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          layout="sizes, prev, pager, next, jumper"
          :total="products.length"
          :page-sizes="[10, 20, 50]"
        />
      </template>
    </DataTableShell>

    <DynamicFormDialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增商品' : '编辑商品'"
      :fields="productFields"
      :model="productForm"
      @submit="submitProductForm"
    />
    <DetailDrawer
      v-model="detailVisible"
      title="商品详情"
      :items="productDetailItems"
      :record="productDetailRecord"
      width="560px"
    />
    <BatchConfirmDialog
      v-model="batchVisible"
      title="删除商品确认"
      action-name="确认删除"
      :selected-count="batchRows.length"
      :items="batchRows.map((row) => `${row.code} ${row.name}`)"
      confirm-keyword="DELETE"
      @confirm="confirmProductDelete"
    />
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { RefreshCw, SlidersHorizontal } from 'lucide-vue-next';
import AppState from '@/components/business/AppState.vue';
import BatchConfirmDialog from '@/components/business/BatchConfirmDialog.vue';
import DataTableShell from '@/components/business/DataTableShell.vue';
import DetailDrawer from '@/components/business/DetailDrawer.vue';
import DynamicFormDialog from '@/components/business/DynamicFormDialog.vue';
import type { DynamicFormField } from '@/components/business/DynamicFormDialog.vue';
import ListPageShell from '@/components/business/ListPageShell.vue';
import PermissionButton from '@/components/business/PermissionButton.vue';
import QueryToolbar from '@/components/business/QueryToolbar.vue';

defineOptions({ name: 'ProductManagementView' });

type ProductRow = {
  index: number;
  code: string;
  name: string;
  factoryCode: string;
  factoryModel: string;
  factoryName: string;
  supplier: string;
  category: string;
  unit: string;
  warehouse: string;
  position: string;
  price: number;
  cost: string;
};

type ColumnKey = 'index' | 'code' | 'name' | 'factoryCode' | 'factoryModel' | 'factoryName' | 'supplier' | 'type' | 'category' | 'unit' | 'warehouse' | 'position' | 'price' | 'cost';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const filters = reactive({
  name: '',
  code: '',
  shortName: '',
  barcode: '',
  category: '',
  status: '',
});

const columnOptions: Array<{ key: ColumnKey; label: string }> = [
  { key: 'index', label: '序号' },
  { key: 'code', label: '编码' },
  { key: 'name', label: '名称' },
  { key: 'factoryCode', label: '厂家编码' },
  { key: 'factoryModel', label: '厂家型号' },
  { key: 'factoryName', label: '厂家名称' },
  { key: 'supplier', label: '来源供应商' },
  { key: 'type', label: '商品类型' },
  { key: 'category', label: '分类' },
  { key: 'unit', label: '单位' },
  { key: 'warehouse', label: '默认仓库' },
  { key: 'position', label: '默认库位' },
  { key: 'price', label: '价格' },
  { key: 'cost', label: '成本' },
];

const visibleColumns = reactive<Record<ColumnKey, boolean>>(Object.fromEntries(
  columnOptions.map((column) => [column.key, true]),
) as Record<ColumnKey, boolean>);

const selectedRows = ref<ProductRow[]>([]);
const tableDensity = ref(String(route.query.density || 'default'));
const currentPage = ref(Number(route.query.page || 1));
const pageSize = ref(Number(route.query.pageSize || 10));
const formVisible = ref(false);
const detailVisible = ref(false);
const batchVisible = ref(false);
const formMode = ref<'create' | 'edit'>('create');
const activeProduct = ref<ProductRow | null>(null);
const batchRows = ref<ProductRow[]>([]);

const productForm = reactive<Record<string, unknown>>({
  code: '',
  name: '',
  category: '',
  unit: '',
  warehouse: '',
  price: 0,
  remark: '',
});

const productFields: DynamicFormField[] = [
  { key: 'code', label: '编码', required: true, placeholder: '请输入商品编码' },
  { key: 'name', label: '名称', required: true, placeholder: '请输入商品名称' },
  {
    key: 'category',
    label: '分类',
    component: 'select',
    required: true,
    options: [
      { label: '分类1', value: '分类1' },
      { label: '分类2', value: '分类2' },
    ],
  },
  { key: 'unit', label: '单位', required: true, placeholder: '请输入单位' },
  { key: 'warehouse', label: '默认仓库', placeholder: '请输入默认仓库' },
  { key: 'price', label: '价格', component: 'number', min: 0 },
  { key: 'remark', label: '备注', component: 'textarea', span: 24 },
];

const productDetailItems = [
  { key: 'code', label: '编码' },
  { key: 'name', label: '名称' },
  { key: 'factoryCode', label: '厂家编码' },
  { key: 'factoryModel', label: '厂家型号' },
  { key: 'factoryName', label: '厂家名称' },
  { key: 'supplier', label: '来源供应商' },
  { key: 'category', label: '分类' },
  { key: 'unit', label: '单位' },
  { key: 'warehouse', label: '默认仓库' },
  { key: 'position', label: '默认库位' },
  { key: 'price', label: '价格' },
  { key: 'cost', label: '成本' },
];

const products: ProductRow[] = Array.from({ length: 31 }, (_, index) => {
  const number = index + 1;
  return {
    index: number,
    code: `PROD-${String(32 - number).padStart(4, '0')}`,
    name: `商品${32 - number}`,
    factoryCode: '',
    factoryModel: '',
    factoryName: '',
    supplier: '-',
    category: number < 16 ? '分类1' : '分类2',
    unit: number < 16 ? '件' : '箱',
    warehouse: number % 3 === 0 ? '仓库1' : '-',
    position: number % 3 === 0 ? '库位1' : '-',
    price: 42 - number,
    cost: (38 - number).toFixed(2),
  };
});

const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return products.slice(start, start + pageSize.value);
});

const productDetailRecord = computed<Record<string, unknown>>(() => {
  return activeProduct.value ? { ...activeProduct.value } : {};
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
    name: '',
    code: '',
    shortName: '',
    barcode: '',
    category: '',
    status: '',
  });
}

function search() {
  currentPage.value = 1;
  return filters;
}

function exportRows() {
  const rows = selectedRows.value.length ? selectedRows.value : products;
  ElMessage.success(`已准备导出 ${rows.length} 条商品数据`);
}

function openCreateProduct() {
  formMode.value = 'create';
  activeProduct.value = null;
  Object.assign(productForm, {
    code: '',
    name: '',
    category: '',
    unit: '',
    warehouse: '',
    price: 0,
    remark: '',
  });
  formVisible.value = true;
}

function openEditProduct(row: ProductRow) {
  formMode.value = 'edit';
  activeProduct.value = row;
  Object.assign(productForm, {
    code: row.code,
    name: row.name,
    category: row.category,
    unit: row.unit,
    warehouse: row.warehouse === '-' ? '' : row.warehouse,
    price: row.price,
    remark: '',
  });
  formVisible.value = true;
}

function openProductDetail(row: ProductRow) {
  activeProduct.value = row;
  detailVisible.value = true;
}

function openDeleteProducts(rows: ProductRow[]) {
  if (!rows.length) {
    ElMessage.warning('请先选择商品');
    return;
  }
  batchRows.value = rows;
  batchVisible.value = true;
}

function submitProductForm(value: Record<string, unknown>) {
  Object.assign(productForm, value);
  formVisible.value = false;
  ElMessage.success(formMode.value === 'create' ? '新增商品已保存' : '商品编辑已保存');
}

function confirmProductDelete() {
  const count = batchRows.value.length;
  batchVisible.value = false;
  selectedRows.value = [];
  batchRows.value = [];
  ElMessage.success(`已确认删除 ${count} 条商品`);
}
</script>
