<template>
  <DocumentEditorShell
    title="新增采购单"
    description="用于录入采购订单，支持草稿保存、提交审核、明细金额和结算信息联动。"
    status="草稿"
    status-type="warning"
    :summary-items="summaryItems"
  >
    <template #actions>
      <el-button @click="goBack">返回</el-button>
      <el-button @click="resetDocument">重置</el-button>
      <PermissionButton type="primary" plain permission="procurement:create" no-permission-mode="disable" @click="saveDraft">保存草稿</PermissionButton>
      <PermissionButton type="success" permission="procurement:audit" no-permission-mode="disable" @click="submitForApproval">提交审核</PermissionButton>
    </template>

    <DocumentBasicInfoCard
      v-model="basicInfo"
      :supplier-options="supplierOptions"
      :warehouse-options="warehouseOptions"
      :purchaser-options="purchaserOptions"
      :settlement-account-options="accountOptions"
    />

    <DocumentLineItemsTable
      v-model="lineItems"
      :product-options="productOptions"
      :warehouse-options="warehouseOptions"
      :unit-options="unitOptions"
      :default-warehouse="basicInfo.warehouse"
    />

    <DocumentSettlementCard
      v-model="settlementInfo"
      :payable-amount="payableAmount"
      :settlement-method-options="settlementMethodOptions"
      :account-options="accountOptions"
    />

    <template #summaryActions>
      <PermissionButton permission="procurement:create" no-permission-mode="disable" @click="saveDraft">保存草稿</PermissionButton>
      <PermissionButton type="success" permission="procurement:audit" no-permission-mode="disable" @click="submitForApproval">提交审核</PermissionButton>
    </template>
  </DocumentEditorShell>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus/es/components/message/index';
import DocumentBasicInfoCard, {
  type DocumentBasicInfo,
  type DocumentSelectOption,
} from '@/framework/components-erp/DocumentBasicInfoCard.vue';
import DocumentEditorShell from '@/framework/components-erp/DocumentEditorShell.vue';
import DocumentLineItemsTable, {
  type DocumentLineItem,
  type DocumentProductOption,
} from '@/framework/components-erp/DocumentLineItemsTable.vue';
import DocumentSettlementCard, {
  type DocumentSettlementInfo,
} from '@/framework/components-erp/DocumentSettlementCard.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import { useAuthStore } from '@/stores/auth';

defineOptions({ name: 'PurchaseOrderCreateView' });

const router = useRouter();
const authStore = useAuthStore();

const supplierOptions: DocumentSelectOption[] = [
  { label: '供应商1', value: 'supplier-1' },
  { label: '华东配件供应商', value: 'supplier-east' },
  { label: '华南耗材供应商', value: 'supplier-south' },
];

const warehouseOptions: DocumentSelectOption[] = [
  { label: '主仓 / A-01', value: 'main-a01' },
  { label: '主仓 / B-02', value: 'main-b02' },
  { label: '质检仓 / Q-01', value: 'qc-q01' },
];

const purchaserOptions: DocumentSelectOption[] = [
  { label: '张三', value: 'zhangsan' },
  { label: '李四', value: 'lisi' },
  { label: '王五', value: 'wangwu' },
];

const accountOptions: DocumentSelectOption[] = [
  { label: '应付挂账', value: 'payable' },
  { label: '建设银行 8899', value: 'ccb-8899' },
  { label: '支付宝采购账户', value: 'alipay-purchase' },
];

const settlementMethodOptions: DocumentSelectOption[] = [
  { label: '挂账', value: 'credit' },
  { label: '现结', value: 'cash' },
  { label: '月结', value: 'monthly' },
];

const unitOptions: DocumentSelectOption[] = [
  { label: '件', value: '件' },
  { label: '箱', value: '箱' },
  { label: '套', value: '套' },
];

const productOptions: DocumentProductOption[] = [
  { label: '机油滤芯 A100', value: 'prod-a100', sku: 'A100', spec: '适配 1.6L 发动机', unit: '件', price: 48 },
  { label: '刹车片 B210', value: 'prod-b210', sku: 'B210', spec: '前轮陶瓷片', unit: '套', price: 186 },
  { label: '空气滤芯 C330', value: 'prod-c330', sku: 'C330', spec: '标准型', unit: '件', price: 38 },
];

const basicInfo = ref<DocumentBasicInfo>(createDefaultBasicInfo());
const lineItems = ref<DocumentLineItem[]>([createDefaultLineItem()]);
const settlementInfo = ref<DocumentSettlementInfo>(createDefaultSettlementInfo());

const commodityAmount = computed(() => lineItems.value.reduce((total, line) => total + line.quantity * line.price, 0));
const lineDiscountAmount = computed(() => lineItems.value.reduce((total, line) => total + line.discount, 0));
const taxAmount = computed(() => lineItems.value.reduce((total, line) => {
  const taxableAmount = Math.max(line.quantity * line.price - line.discount, 0);
  return total + taxableAmount * (line.taxRate / 100);
}, 0));
const payableAmount = computed(() => Math.max(commodityAmount.value - lineDiscountAmount.value - settlementInfo.value.discountAmount + taxAmount.value, 0));
const arrearsAmount = computed(() => Math.max(payableAmount.value - settlementInfo.value.paidAmount, 0));

const summaryItems = computed(() => [
  { label: '商品合计', value: formatCurrency(commodityAmount.value) },
  { label: '明细优惠', value: formatCurrency(lineDiscountAmount.value) },
  { label: '税额', value: formatCurrency(taxAmount.value) },
  { label: '整单优惠', value: formatCurrency(settlementInfo.value.discountAmount) },
  { label: '应付金额', value: formatCurrency(payableAmount.value), strong: true },
  { label: '未付金额', value: formatCurrency(arrearsAmount.value), strong: true },
]);

function createDefaultBasicInfo(): DocumentBasicInfo {
  return {
    documentNo: 'PO202607100002',
    documentTime: formatCurrentDateTime(),
    supplier: 'supplier-1',
    warehouse: 'main-a01',
    purchaser: 'zhangsan',
    settlementAccount: 'payable',
    creator: authStore.username || 'admin',
    lastModifier: authStore.username || 'admin',
    expectedArrivalDate: '2026-07-15',
    externalNo: '',
    remark: '',
  };
}

function formatCurrentDateTime() {
  const now = new Date();
  return `${now.getFullYear()}-${padDatePart(now.getMonth() + 1)}-${padDatePart(now.getDate())} ${padDatePart(now.getHours())}:${padDatePart(now.getMinutes())}:${padDatePart(now.getSeconds())}`;
}

function padDatePart(value: number) {
  return String(value).padStart(2, '0');
}

function createDefaultLineItem(): DocumentLineItem {
  return {
    id: Date.now(),
    productCode: '',
    productName: '',
    productSpec: '',
    warehouse: 'main-a01',
    unit: '件',
    quantity: 1,
    price: 0,
    discount: 0,
    taxRate: 13,
    remark: '',
  };
}

function createDefaultSettlementInfo(): DocumentSettlementInfo {
  return {
    settlementMethod: 'credit',
    paymentAccount: 'payable',
    discountAmount: 0,
    paidAmount: 0,
    invoiceType: 'normal',
    contact: '供应商联系人',
    contactPhone: '13800000000',
    deliveryAddress: '默认收货地址',
  };
}

function formatCurrency(value: number) {
  return `¥ ${value.toFixed(2)}`;
}

function validateDocument() {
  if (!basicInfo.value.supplier) {
    ElMessage.warning('请选择供应商');
    return false;
  }
  const validLines = lineItems.value.filter((line) => line.productCode && line.quantity > 0 && line.price >= 0);
  if (!validLines.length) {
    ElMessage.warning('请至少录入一条有效明细');
    return false;
  }
  return true;
}

function saveDraft() {
  if (!validateDocument()) {
    return;
  }
  ElMessage.success('采购单草稿已保存');
}

function submitForApproval() {
  if (!validateDocument()) {
    return;
  }
  ElMessage.success('采购单已提交审核');
}

function resetDocument() {
  basicInfo.value = createDefaultBasicInfo();
  lineItems.value = [createDefaultLineItem()];
  settlementInfo.value = createDefaultSettlementInfo();
}

function goBack() {
  router.push('/purchase/orders/draft');
}
</script>
