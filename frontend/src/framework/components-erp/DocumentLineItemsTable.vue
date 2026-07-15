<template>
  <DocumentSection title="单据明细" description="商品、仓库、数量、价格和税率在这里维护，金额实时计算。">
    <template #extra>
      <div class="document-line-actions">
        <el-button type="primary" plain @click="addLine">添加明细</el-button>
        <el-button :disabled="!selectedLines.length" @click="removeSelectedLines">删除选中</el-button>
      </div>
    </template>

    <div class="document-line-table">
      <el-table
        :data="model"
        height="360"
        border
        cell-class-name="document-line-table-cell-center"
        header-cell-class-name="document-line-table-cell-center"
        @selection-change="selectedLines = $event"
      >
        <el-table-column type="selection" width="44" fixed="left" />
        <el-table-column label="序号" width="64" fixed="left" align="center" header-align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="商品" min-width="220" align="center" header-align="center">
          <template #header><span class="required-mark">*</span> 商品</template>
          <template #default="{ row }">
            <div class="document-product-cell">
              <el-select v-model="row.productCode" filterable placeholder="选择商品" @change="updateProduct(row)">
                <el-option
                  v-for="option in productOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                >
                  <span>{{ option.label }}</span>
                  <small>{{ option.sku }}</small>
                </el-option>
              </el-select>
              <span v-if="row.productSpec" class="document-line-hint">{{ row.productSpec }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="仓库/库位" min-width="170" align="center" header-align="center">
          <template #header><span class="required-mark">*</span> 仓库/库位</template>
          <template #default="{ row }">
            <el-select v-model="row.warehouse" filterable placeholder="选择库位">
              <el-option v-for="option in warehouseOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="单位" width="110" align="center" header-align="center">
          <template #default="{ row }">
            <el-select v-model="row.unit" placeholder="单位">
              <el-option v-for="option in unitOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="130" align="center" header-align="center">
          <template #header><span class="required-mark">*</span> 数量</template>
          <template #default="{ row }">
            <XuanDecimalInput
              :model-value="row.quantity"
              :scale="4"
              :suffix="row.unit"
              placeholder="0.0000"
              @update:model-value="updateLineNumber(row, 'quantity', $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="价格" width="130" align="center" header-align="center">
          <template #header><span class="required-mark">*</span> 价格</template>
          <template #default="{ row }">
            <XuanDecimalInput
              :model-value="row.price"
              :scale="2"
              prefix="¥"
              placeholder="0.00"
              @update:model-value="updateLineNumber(row, 'price', $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="折扣" width="130" align="center" header-align="center">
          <template #default="{ row }">
            <XuanDecimalInput
              :model-value="row.discount"
              :scale="2"
              prefix="¥"
              placeholder="0.00"
              @update:model-value="updateLineNumber(row, 'discount', $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="税率%" width="116" align="center" header-align="center">
          <template #default="{ row }">
            <XuanDecimalInput
              :model-value="row.taxRate"
              :scale="2"
              suffix="%"
              placeholder="0.00"
              @update:model-value="updateLineNumber(row, 'taxRate', $event, { max: 100 })"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120" align="center" header-align="center">
          <template #default="{ row }">
            <strong>{{ formatAmount(lineTotal(row)) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="160" align="center" header-align="center">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="备注" clearable />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="84" fixed="right" align="center" header-align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeLine($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-button class="document-line-add-bottom" type="primary" plain @click="addLine">添加明细</el-button>
  </DocumentSection>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import DocumentSection from '@/framework/components-erp/DocumentSection.vue';
import XuanDecimalInput from '@/framework/components/XuanDecimalInput.vue';

export type DocumentProductOption = {
  label: string;
  value: string;
  sku: string;
  spec: string;
  unit: string;
  price: number;
};

export type DocumentLineItem = {
  id: number;
  productCode: string;
  productName: string;
  productSpec: string;
  warehouse: string;
  unit: string;
  quantity: number;
  price: number;
  discount: number;
  taxRate: number;
  remark: string;
};

const model = defineModel<DocumentLineItem[]>({ required: true });

const props = withDefaults(defineProps<{
  productOptions: DocumentProductOption[];
  warehouseOptions: Array<{ label: string; value: string }>;
  unitOptions: Array<{ label: string; value: string }>;
  defaultWarehouse?: string;
}>(), {
  productOptions: () => [],
  warehouseOptions: () => [],
  unitOptions: () => [],
  defaultWarehouse: '',
});

const selectedLines = ref<DocumentLineItem[]>([]);
type NumericLineKey = 'quantity' | 'price' | 'discount' | 'taxRate';

function addLine() {
  model.value = [
    ...model.value,
    {
      id: Date.now() + model.value.length,
      productCode: '',
      productName: '',
      productSpec: '',
      warehouse: props.defaultWarehouse,
      unit: '件',
      quantity: 1,
      price: 0,
      discount: 0,
      taxRate: 13,
      remark: '',
    },
  ];
}

function removeLine(index: number) {
  if (model.value.length === 1) {
    model.value = [{ ...model.value[0], productCode: '', productName: '', productSpec: '', quantity: 1, price: 0, discount: 0, remark: '' }];
    return;
  }
  model.value = model.value.filter((_, currentIndex) => currentIndex !== index);
}

function removeSelectedLines() {
  const ids = new Set(selectedLines.value.map((line) => line.id));
  const nextLines = model.value.filter((line) => !ids.has(line.id));
  model.value = nextLines.length ? nextLines : [];
  selectedLines.value = [];
  if (!model.value.length) {
    addLine();
  }
}

function updateProduct(row: DocumentLineItem) {
  const product = props.productOptions.find((option) => option.value === row.productCode);
  if (!product) {
    return;
  }
  row.productName = product.label;
  row.productSpec = product.spec;
  row.unit = product.unit;
  row.price = product.price;
}

function lineTotal(row: DocumentLineItem) {
  return Math.max(row.quantity * row.price - row.discount, 0);
}

function updateLineNumber(
  row: DocumentLineItem,
  key: NumericLineKey,
  value: string,
  options: { min?: number; max?: number } = {},
) {
  const min = options.min ?? 0;
  const parsedValue = Number(value);
  const normalizedValue = Number.isFinite(parsedValue) ? parsedValue : min;
  row[key] = clampNumber(normalizedValue, min, options.max);
}

function clampNumber(value: number, min: number, max?: number) {
  if (typeof max === 'number') {
    return Math.min(Math.max(value, min), max);
  }
  return Math.max(value, min);
}

function formatAmount(value: number) {
  return value.toFixed(2);
}
</script>
