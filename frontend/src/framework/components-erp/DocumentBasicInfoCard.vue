<template>
  <DocumentSection title="基本信息" description="维护单据头信息，必填项用于审核和后续入库流转。">
    <el-form label-position="top" class="document-basic-grid">
      <el-form-item label="单号">
        <el-input v-model="model.documentNo" disabled />
      </el-form-item>
      <el-form-item label="单据时间" required>
        <el-date-picker
          v-model="model.documentTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择单据时间"
        />
      </el-form-item>
      <el-form-item label="供应商" required>
        <el-select v-model="model.supplier" filterable placeholder="请选择供应商">
          <el-option v-for="option in supplierOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="默认仓库" required>
        <el-select v-model="model.warehouse" filterable placeholder="请选择默认仓库">
          <el-option v-for="option in warehouseOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="采购员">
        <el-select v-model="model.purchaser" filterable placeholder="请选择采购员">
          <el-option v-for="option in purchaserOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="结算账户">
        <el-select v-model="model.settlementAccount" filterable placeholder="请选择结算账户">
          <el-option v-for="option in settlementAccountOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="制单人">
        <el-input v-model="model.creator" disabled />
      </el-form-item>
      <el-form-item label="最后修改人">
        <el-input v-model="model.lastModifier" disabled />
      </el-form-item>
      <el-form-item label="预计到货">
        <el-date-picker
          v-model="model.expectedArrivalDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择预计到货日期"
        />
      </el-form-item>
      <el-form-item label="外部单号">
        <el-input v-model="model.externalNo" placeholder="供应商单号/合同号" clearable />
      </el-form-item>
      <el-form-item label="备注" class="document-basic-remark">
        <el-input
          v-model="model.remark"
          class="document-basic-remark-input"
          type="textarea"
          maxlength="200"
          show-word-limit
          placeholder="可填写采购约定、运输要求、对账说明"
          :rows="1"
        />
      </el-form-item>
    </el-form>
  </DocumentSection>
</template>

<script setup lang="ts">
import DocumentSection from '@/framework/components-erp/DocumentSection.vue';

export type DocumentSelectOption = {
  label: string;
  value: string;
};

export type DocumentBasicInfo = {
  documentNo: string;
  documentTime: string;
  supplier: string;
  warehouse: string;
  purchaser: string;
  settlementAccount: string;
  creator: string;
  lastModifier: string;
  expectedArrivalDate: string;
  externalNo: string;
  remark: string;
};

const model = defineModel<DocumentBasicInfo>({ required: true });

withDefaults(defineProps<{
  supplierOptions: DocumentSelectOption[];
  warehouseOptions: DocumentSelectOption[];
  purchaserOptions: DocumentSelectOption[];
  settlementAccountOptions: DocumentSelectOption[];
}>(), {
  supplierOptions: () => [],
  warehouseOptions: () => [],
  purchaserOptions: () => [],
  settlementAccountOptions: () => [],
});
</script>
