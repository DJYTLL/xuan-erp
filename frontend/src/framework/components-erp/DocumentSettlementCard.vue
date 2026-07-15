<template>
  <DocumentSection title="结算信息" description="记录付款方式、优惠、发票和收货信息，便于后续对账。">
    <el-form label-position="top" class="document-settlement-grid">
      <el-form-item label="结算方式">
        <el-select v-model="model.settlementMethod" placeholder="请选择结算方式">
          <el-option v-for="option in settlementMethodOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="付款账户">
        <el-select v-model="model.paymentAccount" placeholder="请选择付款账户">
          <el-option v-for="option in accountOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="整单优惠">
        <XuanDecimalInput
          :model-value="model.discountAmount"
          class="document-settlement-number"
          :scale="2"
          prefix="¥"
          placeholder="0.00"
          @update:model-value="updateSettlementNumber('discountAmount', $event)"
        />
      </el-form-item>
      <el-form-item label="本次付款">
        <XuanDecimalInput
          :model-value="model.paidAmount"
          class="document-settlement-number"
          :scale="2"
          prefix="¥"
          placeholder="0.00"
          @update:model-value="updateSettlementNumber('paidAmount', $event)"
        />
      </el-form-item>
      <el-form-item label="发票类型">
        <el-select v-model="model.invoiceType" placeholder="请选择发票类型">
          <el-option label="不开票" value="none" />
          <el-option label="普通发票" value="normal" />
          <el-option label="专用发票" value="special" />
        </el-select>
      </el-form-item>
      <el-form-item label="应付金额">
        <el-input :model-value="payableAmountText" disabled />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="model.contact" placeholder="供应商联系人" clearable />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="model.contactPhone" placeholder="联系电话" clearable />
      </el-form-item>
      <el-form-item label="收货地址" class="document-settlement-address">
        <el-input v-model="model.deliveryAddress" placeholder="请输入收货地址" clearable />
      </el-form-item>
    </el-form>
  </DocumentSection>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import DocumentSection from '@/framework/components-erp/DocumentSection.vue';
import XuanDecimalInput from '@/framework/components/XuanDecimalInput.vue';

export type DocumentSettlementInfo = {
  settlementMethod: string;
  paymentAccount: string;
  discountAmount: number;
  paidAmount: number;
  invoiceType: string;
  contact: string;
  contactPhone: string;
  deliveryAddress: string;
};

const model = defineModel<DocumentSettlementInfo>({ required: true });

const props = withDefaults(defineProps<{
  payableAmount: number;
  settlementMethodOptions: Array<{ label: string; value: string }>;
  accountOptions: Array<{ label: string; value: string }>;
}>(), {
  payableAmount: 0,
  settlementMethodOptions: () => [],
  accountOptions: () => [],
});

const payableAmountText = computed(() => `¥ ${props.payableAmount.toFixed(2)}`);

function updateSettlementNumber(key: 'discountAmount' | 'paidAmount', value: string) {
  const parsedValue = Number(value);
  model.value[key] = Number.isFinite(parsedValue) ? Math.max(parsedValue, 0) : 0;
}
</script>
