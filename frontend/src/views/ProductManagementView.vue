<template>
  <ListPageShell :title="t('nav.products')">
    <template #query>
      <SearchActionBar @reset="resetFilters" @search="search">
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
          <el-button>导入</el-button>
          <el-button>导入结果</el-button>
          <PermissionButton type="primary" permission="product:create" @click="openCreateProduct">新增</PermissionButton>
        </template>
      </SearchActionBar>
    </template>

    <XuanBrowseTable
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      v-model:density="tableDensity"
      page-code="inventory-products"
      table-code="product-list"
      :tenant-id="browseTenantId"
      :user-id="browseUserId"
      :data="pagedProducts"
      :columns="productColumns"
      :total="products.length"
      :selected-count="selectedRows.length"
      :actions-width="180"
      @selection-change="selectedRows = $event"
    >
      <template #toolbar-actions>
        <el-button text @click="exportRows">导出</el-button>
        <el-button text type="danger" :disabled="!selectedRows.length" @click="openDeleteProducts(selectedRows)">批量删除</el-button>
      </template>

      <template #cell-type>
        <el-tag size="small">普通商品</el-tag>
      </template>

      <template #empty>
        <AppState type="empty" title="暂无商品" description="当前筛选条件下没有商品数据。" />
      </template>

      <template #actions="{ row }">
        <el-button link type="primary" @click="openProductDetail(row)">查看</el-button>
        <el-button link type="primary" @click="openEditProduct(row)">编辑</el-button>
        <el-button link type="danger" @click="openDeleteProducts([row])">删除</el-button>
      </template>
    </XuanBrowseTable>

    <DynamicFormDialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增商品' : '编辑商品'"
      description="按业务使用顺序维护商品资料、库存策略、价格与扩展字段"
      helper-text="拖动标题栏移动，拖动四角调整大小"
      :fields="productFields"
      :sections="productFormSections"
      :model="productForm"
      :custom-fields="productCustomFields"
      variant="workspace"
      width="96vw"
      label-width="0"
      label-position="top"
      show-custom-fields
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
import { ElMessage } from 'element-plus/es/components/message/index';
import AppState from '@/framework/components/AppState.vue';
import BatchConfirmDialog from '@/framework/components/BatchConfirmDialog.vue';
import DetailDrawer from '@/framework/components/DetailDrawer.vue';
import DynamicFormDialog from '@/framework/components/DynamicFormDialog.vue';
import type { DynamicCustomField, DynamicFormField, DynamicFormSection } from '@/framework/components/DynamicFormDialog.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import SearchActionBar from '@/framework/components/SearchActionBar.vue';
import XuanBrowseTable, { type XuanBrowseTableColumn } from '@/framework/components/XuanBrowseTable.vue';
import type { BrowseTableDensity } from '@/framework/components/browseTablePreferences';
import { useAuthStore } from '@/stores/auth';

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

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const browseTenantId = computed(() => String(authStore.tenantId ?? '0'));
const browseUserId = computed(() => authStore.currentUser?.username || 'anonymous');

const filters = reactive({
  name: '',
  code: '',
  shortName: '',
  barcode: '',
  category: '',
  status: '',
});

const productColumns: Array<XuanBrowseTableColumn<ProductRow>> = [
  { key: 'index', title: '序号', width: 70 },
  { key: 'code', title: '编码', width: 120 },
  { key: 'name', title: '名称', width: 120 },
  { key: 'factoryCode', title: '厂家编码', width: 140 },
  { key: 'factoryModel', title: '厂家型号', width: 140 },
  { key: 'factoryName', title: '厂家名称', width: 140 },
  { key: 'supplier', title: '来源供应商', width: 150 },
  { key: 'type', title: '商品类型', width: 120 },
  { key: 'category', title: '分类', width: 100 },
  { key: 'unit', title: '单位', width: 90 },
  { key: 'warehouse', title: '默认仓库', width: 120 },
  { key: 'position', title: '默认库位', width: 120 },
  { key: 'price', title: '价格', width: 90, align: 'right' },
  { key: 'cost', title: '成本', width: 90, align: 'right' },
];

const selectedRows = ref<ProductRow[]>([]);
const tableDensity = ref<BrowseTableDensity>((route.query.density as BrowseTableDensity) || 'default');
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
  position: '',
  stockStrategy: '',
  taxRate: '',
  vip2Price: '',
  retailPrice: '',
  wholesalePrice: '',
  vipPrice: '',
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

const productFormSections: DynamicFormSection[] = [
  {
    title: '商品资料',
    fields: [
      { key: 'code', label: '商品编码', required: true, placeholder: '请输入商品编码' },
      { key: 'name', label: '商品名称', required: true, placeholder: '请输入商品名称' },
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
      { key: 'position', label: '默认库位', placeholder: '请输入默认库位' },
      { key: 'taxRate', label: '税率', placeholder: '税率' },
    ],
  },
  {
    title: '客户类别售价',
    fields: [
      { key: 'vip2Price', label: 'vip2', placeholder: '' },
      { key: 'retailPrice', label: '零售客户', placeholder: '' },
      { key: 'wholesalePrice', label: '批发客户', placeholder: '' },
      { key: 'vipPrice', label: 'VIP客户', placeholder: '' },
    ],
  },
];

const productCustomFields = ref<DynamicCustomField[]>([]);

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
    position: '',
    stockStrategy: '',
    taxRate: '',
    vip2Price: '',
    retailPrice: '',
    wholesalePrice: '',
    vipPrice: '',
    price: 0,
    remark: '',
  });
  productCustomFields.value = [];
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
    position: row.position === '-' ? '' : row.position,
    stockStrategy: '',
    taxRate: '',
    vip2Price: '',
    retailPrice: row.price,
    wholesalePrice: '',
    vipPrice: '',
    price: row.price,
    remark: '',
  });
  productCustomFields.value = [];
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
  productCustomFields.value = Array.isArray(value.customFields) ? value.customFields as DynamicCustomField[] : [];
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
