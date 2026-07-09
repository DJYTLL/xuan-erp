<template>
  <section>
    <h1 class="page-title">{{ t('nav.products') }}</h1>

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
        <PermissionButton type="primary" permission="product:create">新增</PermissionButton>
      </template>
    </QueryToolbar>

    <DataTableShell>
      <template #toolbar>
        <el-button text :icon="SlidersHorizontal">列设置</el-button>
      </template>

      <el-table :data="products" height="520" border>
        <el-table-column prop="index" label="序号" width="70" />
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" width="120" />
        <el-table-column prop="factoryCode" label="厂家编码" width="140" />
        <el-table-column prop="factoryModel" label="厂家型号" width="140" />
        <el-table-column prop="factoryName" label="厂家名称" width="140" />
        <el-table-column prop="supplier" label="来源供应商" width="150" />
        <el-table-column label="商品类型" width="120">
          <template #default>
            <el-tag size="small">普通商品</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="unit" label="单位" width="90" />
        <el-table-column prop="warehouse" label="默认仓库" width="120" />
        <el-table-column prop="position" label="默认库位" width="120" />
        <el-table-column prop="price" label="价格" width="90" />
        <el-table-column prop="cost" label="成本" width="90" />
        <el-table-column label="操作" fixed="right" width="120">
          <template #default>
            <el-button link type="primary">编辑</el-button>
            <el-button link type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <span>共 31 条</span>
        <el-pagination layout="sizes, prev, pager, next, jumper" :total="31" :page-size="10" />
      </template>
    </DataTableShell>
  </section>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { useI18n } from 'vue-i18n';
import { RefreshCw, SlidersHorizontal } from 'lucide-vue-next';
import DataTableShell from '@/components/business/DataTableShell.vue';
import PermissionButton from '@/components/business/PermissionButton.vue';
import QueryToolbar from '@/components/business/QueryToolbar.vue';

const { t } = useI18n();

const filters = reactive({
  name: '',
  code: '',
  shortName: '',
  barcode: '',
  category: '',
  status: '',
});

const products = Array.from({ length: 12 }, (_, index) => {
  const number = index + 1;
  return {
    index: number,
    code: `PROD-${String(31 - number).padStart(4, '0')}`,
    name: `商品${31 - number}`,
    factoryCode: '',
    factoryModel: '',
    factoryName: '',
    supplier: '-',
    category: number < 7 ? '分类1' : '分类2',
    unit: number < 7 ? '件' : '箱',
    warehouse: number % 3 === 0 ? '仓库1' : '-',
    position: number % 3 === 0 ? '库位1' : '-',
    price: 42 - number,
    cost: (38 - number).toFixed(2),
  };
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
  return filters;
}
</script>
