<template>
  <ListPageShell title="套餐管理">
    <template #query>
      <QueryToolbar>
        <el-input v-model="keyword" class="query-input" placeholder="搜索套餐编码 / 名称 / 初始化模板" clearable />
        <el-select v-model="statusFilter" class="status-filter" placeholder="状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadPlans" />
          <PermissionButton type="primary" permission="tenant-plan:manage" @click="openCreatePlan">新增套餐</PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <el-table v-loading="loading" :data="filteredPlans" row-key="id" border height="100%">
      <el-table-column prop="code" label="套餐编码" min-width="130" />
      <el-table-column prop="name" label="套餐名称" min-width="150" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="计费周期" width="110">
        <template #default="{ row }">
          <span>{{ billingCycleLabel(row.billingCycle) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="120">
        <template #default="{ row }">
          <span>{{ row.currency }} {{ row.priceAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="初始化模板" min-width="130">
        <template #default="{ row }">
          <span>{{ resolveIamInitTemplateCode(row) || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="额度" min-width="180">
        <template #default="{ row }">
          <span>{{ formatQuota(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortNo" label="排序" width="90" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <PermissionButton link type="primary" permission="tenant-plan:manage" @click="openEditPlan(row)">编辑</PermissionButton>
          <PermissionButton
            v-if="row.status === 'ENABLED'"
            link
            type="warning"
            permission="tenant-plan:manage"
            @click="changePlanStatus(row, 'DISABLED')"
          >
            停用
          </PermissionButton>
          <PermissionButton v-else link type="success" permission="tenant-plan:manage" @click="changePlanStatus(row, 'ENABLED')">启用</PermissionButton>
          <PermissionButton link type="danger" permission="tenant-plan:manage" @click="removePlan(row)">删除</PermissionButton>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="planDialogVisible" :title="editingPlan ? '编辑套餐' : '新增套餐'" width="760px" destroy-on-close>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="套餐编码">
              <el-input v-model.trim="planForm.code" :disabled="Boolean(editingPlan)" placeholder="STANDARD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="套餐名称">
              <el-input v-model.trim="planForm.name" placeholder="标准版" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计费周期">
              <el-select v-model="planForm.billingCycle">
                <el-option label="月付" value="MONTHLY" />
                <el-option label="年付" value="YEARLY" />
                <el-option label="永久" value="PERMANENT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="初始化模板">
              <el-select v-model="planForm.iamInitTemplateCode" placeholder="请选择初始化模板">
                <el-option label="基础版 / 基础模板（basic）" value="basic" />
                <el-option label="标准版 / 标准模板（standard）" value="standard" />
                <el-option label="完整版 / 完整模板（full）" value="full" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="价格">
              <XuanDecimalInput v-model="planForm.priceAmount" :scale="2" align="right" placeholder="0.00" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="币种">
              <el-input v-model.trim="planForm.currency" placeholder="CNY" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序">
              <XuanDecimalInput v-model="planForm.sortNo" :scale="0" input-mode="numeric" align="right" placeholder="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大用户数">
              <XuanDecimalInput v-model="planForm.maxUserCount" :scale="0" input-mode="numeric" align="right" clearable placeholder="不限" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大仓库数">
              <XuanDecimalInput v-model="planForm.maxWarehouseCount" :scale="0" input-mode="numeric" align="right" clearable placeholder="不限" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大存储 GB">
              <XuanDecimalInput v-model="planForm.maxStorageGb" :scale="2" align="right" clearable suffix="GB" placeholder="不限" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="功能开关 JSON">
              <el-input
                v-model.trim="planForm.featureFlagsJson"
                type="textarea"
                :rows="4"
                placeholder='{"modules":["product"],"iamInitTemplateCode":"basic"}'
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model.trim="planForm.remark" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingPlan" @click="submitPlan">保存</el-button>
      </template>
    </el-dialog>
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import {
  createTenantPlan,
  deleteTenantPlan,
  disableTenantPlan,
  enableTenantPlan,
  listTenantPlans,
  updateTenantPlan,
} from '@/api/tenants';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import XuanDecimalInput from '@/framework/components/XuanDecimalInput.vue';
import { useAuthStore } from '@/stores/auth';
import type { BillingCycle, TenantPlan, TenantPlanPayload, TenantPlanStatus } from '@/types/tenant';

defineOptions({ name: 'TenantPlanManagementView' });

type TenantPlanForm = {
  code: string;
  name: string;
  billingCycle: BillingCycle;
  priceAmount: string;
  currency: string;
  maxUserCount: string | null;
  maxWarehouseCount: string | null;
  maxStorageGb: string | null;
  featureFlagsJson: string;
  iamInitTemplateCode: string;
  sortNo: string;
  remark: string;
};

const authStore = useAuthStore();
const loading = ref(false);
const keyword = ref('');
const statusFilter = ref<TenantPlanStatus | ''>('');
const plans = ref<TenantPlan[]>([]);
const planDialogVisible = ref(false);
const submittingPlan = ref(false);
const editingPlan = ref<TenantPlan | null>(null);
const planForm = reactive<TenantPlanForm>(emptyPlanForm());

const filteredPlans = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  return plans.value.filter((plan) => {
    if (statusFilter.value && plan.status !== statusFilter.value) {
      return false;
    }
    if (!value) {
      return true;
    }
    return [
      plan.code,
      plan.name,
      resolveIamInitTemplateCode(plan),
      plan.remark || '',
    ].some((item) => item.toLowerCase().includes(value));
  });
});

onMounted(loadPlans);

async function loadPlans() {
  loading.value = true;
  try {
    plans.value = await listTenantPlans();
  } catch {
    plans.value = [];
    ElMessage.error('租户套餐加载失败，请确认网关已转发 /api/tenant-plans');
  } finally {
    loading.value = false;
  }
}

function emptyPlanForm(): TenantPlanForm {
  return {
    code: '',
    name: '',
    billingCycle: 'MONTHLY',
    priceAmount: '0',
    currency: 'CNY',
    maxUserCount: null,
    maxWarehouseCount: null,
    maxStorageGb: null,
    featureFlagsJson: '{"modules":[],"iamInitTemplateCode":"basic"}',
    iamInitTemplateCode: 'basic',
    sortNo: '0',
    remark: '',
  };
}

function openCreatePlan() {
  editingPlan.value = null;
  Object.assign(planForm, emptyPlanForm());
  planDialogVisible.value = true;
}

function openEditPlan(row: TenantPlan) {
  editingPlan.value = row;
  Object.assign(planForm, {
    code: row.code,
    name: row.name,
    billingCycle: row.billingCycle,
    priceAmount: String(row.priceAmount || 0),
    currency: row.currency,
    maxUserCount: row.maxUserCount === null ? null : String(row.maxUserCount),
    maxWarehouseCount: row.maxWarehouseCount === null ? null : String(row.maxWarehouseCount),
    maxStorageGb: row.maxStorageGb === null ? null : String(row.maxStorageGb),
    featureFlagsJson: row.featureFlagsJson || '{}',
    iamInitTemplateCode: resolveIamInitTemplateCode(row) || 'basic',
    sortNo: String(row.sortNo || 0),
    remark: row.remark || '',
  });
  planDialogVisible.value = true;
}

async function submitPlan() {
  if (!planForm.code.trim() || !planForm.name.trim()) {
    ElMessage.warning('请填写套餐编码和套餐名称');
    return;
  }
  if (!planForm.iamInitTemplateCode) {
    ElMessage.warning('请选择初始化模板');
    return;
  }
  let payload: TenantPlanPayload;
  try {
    payload = normalizePlanPayload(planForm);
  } catch {
    ElMessage.warning('功能开关 JSON 格式不正确');
    return;
  }
  submittingPlan.value = true;
  try {
    if (editingPlan.value) {
      await updateTenantPlan(editingPlan.value.id, payload);
    } else {
      await createTenantPlan(payload);
    }
    planDialogVisible.value = false;
    ElMessage.success('租户套餐已保存');
    await loadPlans();
  } finally {
    submittingPlan.value = false;
  }
}

async function changePlanStatus(row: TenantPlan, status: TenantPlanStatus) {
  const reason = status === 'ENABLED' ? '启用套餐' : '停用套餐';
  if (status === 'ENABLED') {
    await enableTenantPlan(row.id, reason, authStore.username);
  } else {
    await disableTenantPlan(row.id, reason, authStore.username);
  }
  ElMessage.success(status === 'ENABLED' ? '套餐已启用' : '套餐已停用');
  await loadPlans();
}

async function removePlan(row: TenantPlan) {
  await deleteTenantPlan(row.id, '套餐废弃', authStore.username);
  ElMessage.success('套餐已删除');
  await loadPlans();
}

function normalizePlanPayload(form: TenantPlanForm): TenantPlanPayload {
  const featureFlags = parseFeatureFlags(form.featureFlagsJson);
  featureFlags.iamInitTemplateCode = form.iamInitTemplateCode;
  return {
    code: form.code.trim().toUpperCase(),
    name: form.name.trim(),
    billingCycle: form.billingCycle,
    priceAmount: normalizeRequiredNumber(form.priceAmount),
    currency: form.currency.trim() || 'CNY',
    maxUserCount: normalizeNullableInteger(form.maxUserCount),
    maxWarehouseCount: normalizeNullableInteger(form.maxWarehouseCount),
    maxStorageGb: normalizeNullableNumber(form.maxStorageGb),
    featureFlagsJson: JSON.stringify(featureFlags),
    iamInitTemplateCode: form.iamInitTemplateCode,
    sortNo: normalizeRequiredInteger(form.sortNo),
    remark: form.remark.trim(),
  };
}

function normalizeRequiredNumber(value: string | null) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0;
}

function normalizeRequiredInteger(value: string | null) {
  return Math.trunc(normalizeRequiredNumber(value));
}

function normalizeNullableNumber(value: string | null) {
  if (value === null || value.trim() === '') {
    return null;
  }
  return normalizeRequiredNumber(value);
}

function normalizeNullableInteger(value: string | null) {
  const parsed = normalizeNullableNumber(value);
  return parsed === null ? null : Math.trunc(parsed);
}

function parseFeatureFlags(source: string): Record<string, unknown> {
  if (!source.trim()) {
    return {};
  }
  const parsed = JSON.parse(source);
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('feature flags must be object');
  }
  return parsed as Record<string, unknown>;
}

function resolveIamInitTemplateCode(plan: TenantPlan) {
  try {
    const featureFlags = parseFeatureFlags(plan.featureFlagsJson || '{}');
    return typeof featureFlags.iamInitTemplateCode === 'string' ? featureFlags.iamInitTemplateCode : '';
  } catch {
    return '';
  }
}

function billingCycleLabel(value: BillingCycle) {
  const labels: Record<BillingCycle, string> = {
    MONTHLY: '月付',
    YEARLY: '年付',
    PERMANENT: '永久',
  };
  return labels[value] || value;
}

function formatQuota(plan: TenantPlan) {
  return [
    `用户 ${formatLimit(plan.maxUserCount)}`,
    `仓库 ${formatLimit(plan.maxWarehouseCount)}`,
    `存储 ${formatLimit(plan.maxStorageGb)}GB`,
  ].join(' / ');
}

function formatLimit(value: number | string | null) {
  if (value === null || value === undefined || value === '') {
    return '不限';
  }
  return String(value);
}
</script>

<style scoped>
.status-filter {
  width: 130px;
}

@media (max-width: 720px) {
  .status-filter {
    width: 100%;
  }
}
</style>
