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
      <el-table-column label="列权限模板" min-width="180">
        <template #default="{ row }">
          <span>{{ formatColumnPermissionTemplates(row) }}</span>
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

    <DynamicFormDialog
      v-model="planDialogVisible"
      :title="editingPlan ? '编辑套餐' : '新增套餐'"
      description="套餐决定租户可使用的页面权限模板、列权限模板和资源额度。"
      :fields="planFormFields"
      :sections="planFormSections"
      :model="planForm"
      size="lg"
      variant="workspace"
      workspace-size="md"
      label-position="top"
      :confirm-permission="'tenant-plan:manage'"
      :loading="submittingPlan"
      @submit="submitPlan"
    />
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import { listIamColumnPermissionTemplates } from '@/api/iamAdmin';
import {
  createTenantPlan,
  deleteTenantPlan,
  disableTenantPlan,
  enableTenantPlan,
  listTenantPlans,
  updateTenantPlan,
} from '@/api/tenants';
import DynamicFormDialog from '@/framework/components/DynamicFormDialog.vue';
import type { DynamicFormField, DynamicFormSection } from '@/framework/components/DynamicFormDialog.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import { useAuthorizationStore } from '@/stores/authorization';
import { useAuthStore } from '@/stores/auth';
import type { IamColumnPermissionTemplate } from '@/types/iamAdmin';
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
  columnPermissionTemplateCodes: string[];
  defaultColumnPermissionTemplateCode: string;
  sortNo: string;
  remark: string;
};

const authStore = useAuthStore();
const authorizationStore = useAuthorizationStore();
const loading = ref(false);
const keyword = ref('');
const statusFilter = ref<TenantPlanStatus | ''>('');
const plans = ref<TenantPlan[]>([]);
const columnPermissionTemplates = ref<IamColumnPermissionTemplate[]>([]);
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

const columnPermissionTemplateOptions = computed(() => columnPermissionTemplates.value
  .filter((template) => template.tenantId === 0 && template.enabled)
  .map((template) => ({
    label: template.name,
    value: template.code,
    description: `${template.code}${template.description ? ` · ${template.description}` : ''}`,
  })));

const selectedColumnPermissionTemplateOptions = computed(() => {
  const selectedCodes = new Set(planForm.columnPermissionTemplateCodes);
  return columnPermissionTemplateOptions.value.filter((option) => selectedCodes.has(String(option.value)));
});

const planFormFields = computed<DynamicFormField[]>(() => [
  {
    key: 'code',
    label: '套餐编码',
    placeholder: 'STANDARD',
    disabled: Boolean(editingPlan.value),
    required: true,
    span: 12,
  },
  {
    key: 'name',
    label: '套餐名称',
    placeholder: '标准版',
    required: true,
    span: 12,
  },
  {
    key: 'billingCycle',
    label: '计费周期',
    component: 'select',
    options: [
      { label: '月付', value: 'MONTHLY' },
      { label: '年付', value: 'YEARLY' },
      { label: '永久', value: 'PERMANENT' },
    ],
    span: 12,
  },
  {
    key: 'iamInitTemplateCode',
    label: '初始化模板',
    component: 'select',
    placeholder: '请选择初始化模板',
    required: true,
    description: '控制租户开通后可看到哪些页面、菜单和按钮。',
    options: [
      { label: '基础版 / 基础模板（basic）', value: 'basic' },
      { label: '标准版 / 标准模板（standard）', value: 'standard' },
      { label: '完整版 / 完整模板（full）', value: 'full' },
    ],
    span: 12,
  },
  {
    key: 'columnPermissionTemplateCodes',
    label: '列权限模板',
    component: 'checkbox-group',
    description: '控制该套餐下租户最多可分配哪些页面字段。角色列权限只能在这里继续缩小。',
    optionStyle: 'card',
    options: columnPermissionTemplateOptions.value,
    span: 24,
  },
  {
    key: 'defaultColumnPermissionTemplateCode',
    label: '默认列权限模板',
    component: 'select',
    placeholder: '默认使用第一个已选模板',
    description: '租户首次初始化时使用的默认列权限模板，必须来自上方已选择模板。',
    clearable: true,
    options: selectedColumnPermissionTemplateOptions.value,
    span: 24,
  },
  { key: 'priceAmount', label: '价格', component: 'number', scale: 2, align: 'right', placeholder: '0.00', span: 8 },
  { key: 'currency', label: '币种', placeholder: 'CNY', span: 8 },
  { key: 'sortNo', label: '排序', component: 'number', scale: 0, inputMode: 'numeric', align: 'right', placeholder: '0', span: 8 },
  { key: 'maxUserCount', label: '最大用户数', component: 'number', scale: 0, inputMode: 'numeric', align: 'right', clearable: true, placeholder: '不限', span: 8 },
  { key: 'maxWarehouseCount', label: '最大仓库数', component: 'number', scale: 0, inputMode: 'numeric', align: 'right', clearable: true, placeholder: '不限', span: 8 },
  { key: 'maxStorageGb', label: '最大存储 GB', component: 'number', scale: 2, align: 'right', clearable: true, suffix: 'GB', placeholder: '不限', span: 8 },
  {
    key: 'featureFlagsJson',
    label: '高级配置 JSON',
    component: 'textarea',
    placeholder: '{"modules":["product"],"iamInitTemplateCode":"basic","columnPermissionTemplateCodes":["tenant_readonly_masked"]}',
    description: '保存时会自动同步初始化模板和列权限模板字段；通常只需要调整 modules 等扩展项。',
    rows: 3,
    span: 24,
  },
  { key: 'remark', label: '备注', component: 'textarea', rows: 3, span: 24 },
]);

const planFormSections = computed<DynamicFormSection[]>(() => [
  {
    title: '基础信息',
    description: '用于识别套餐、控制计费周期，并绑定租户初始化时的页面权限模板。',
    fields: [
      planFormFields.value[0],
      planFormFields.value[1],
      planFormFields.value[2],
      planFormFields.value[3],
    ],
  },
  {
    title: '列权限边界',
    description: '这里是租户可用字段权限的上限；租户下的角色只能在该范围内再收窄。',
    fields: [
      planFormFields.value[4],
      planFormFields.value[5],
    ],
  },
  {
    title: '计费与额度',
    description: '套餐价格、排序和资源上限。留空的额度表示不限制。',
    fields: [
      planFormFields.value[6],
      planFormFields.value[7],
      planFormFields.value[8],
      planFormFields.value[9],
      planFormFields.value[10],
      planFormFields.value[11],
    ],
  },
  {
    title: '高级配置',
    description: '面向后续扩展的原始功能标记，普通维护只需要关注上面的结构化字段。',
    fields: [
      planFormFields.value[12],
      planFormFields.value[13],
    ],
  },
]);

onMounted(async () => {
  await Promise.all([loadPlans(), loadColumnPermissionTemplates()]);
});

async function loadPlans() {
  loading.value = true;
  try {
    plans.value = await listTenantPlans();
  } catch {
    plans.value = [];
    // 真实错误原因统一由 HTTP 拦截器展示，这里只清空列表。
  } finally {
    loading.value = false;
  }
}

async function loadColumnPermissionTemplates() {
  try {
    columnPermissionTemplates.value = await listIamColumnPermissionTemplates({ tenantId: 0, enabled: true });
  } catch {
    columnPermissionTemplates.value = [];
    // 真实错误原因统一由 HTTP 拦截器展示。
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
    featureFlagsJson: '{"modules":[],"iamInitTemplateCode":"basic","columnPermissionTemplateCodes":["tenant_readonly_masked"],"defaultColumnPermissionTemplateCode":"tenant_readonly_masked"}',
    iamInitTemplateCode: 'basic',
    columnPermissionTemplateCodes: ['tenant_readonly_masked'],
    defaultColumnPermissionTemplateCode: 'tenant_readonly_masked',
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
    columnPermissionTemplateCodes: resolveColumnPermissionTemplateCodes(row),
    defaultColumnPermissionTemplateCode: resolveDefaultColumnPermissionTemplateCode(row),
    sortNo: String(row.sortNo || 0),
    remark: row.remark || '',
  });
  planDialogVisible.value = true;
}

async function submitPlan(value: Record<string, unknown>) {
  Object.assign(planForm, value);
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
    await refreshCurrentAuthorizationAfterPlanChange();
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
  await refreshCurrentAuthorizationAfterPlanChange();
}

async function removePlan(row: TenantPlan) {
  await deleteTenantPlan(row.id, '套餐废弃', authStore.username);
  ElMessage.success('套餐已删除');
  await loadPlans();
  await refreshCurrentAuthorizationAfterPlanChange();
}

async function refreshCurrentAuthorizationAfterPlanChange() {
  if (Number(authStore.tenantId || 0) > 0) {
    await authorizationStore.refreshCurrentAuthorizationContext();
  }
}

function normalizePlanPayload(form: TenantPlanForm): TenantPlanPayload {
  const featureFlags = parseFeatureFlags(form.featureFlagsJson);
  featureFlags.iamInitTemplateCode = form.iamInitTemplateCode;
  const selectedColumnTemplateCodes = normalizeStringList(form.columnPermissionTemplateCodes);
  featureFlags.columnPermissionTemplateCodes = selectedColumnTemplateCodes;
  const defaultColumnTemplateCode = selectedColumnTemplateCodes.includes(form.defaultColumnPermissionTemplateCode)
    ? form.defaultColumnPermissionTemplateCode
    : selectedColumnTemplateCodes[0] || '';
  if (defaultColumnTemplateCode) {
    featureFlags.defaultColumnPermissionTemplateCode = defaultColumnTemplateCode;
  } else {
    delete featureFlags.defaultColumnPermissionTemplateCode;
  }
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

function resolveColumnPermissionTemplateCodes(plan: TenantPlan) {
  try {
    const featureFlags = parseFeatureFlags(plan.featureFlagsJson || '{}');
    return normalizeStringList(featureFlags.columnPermissionTemplateCodes);
  } catch {
    return [];
  }
}

function resolveDefaultColumnPermissionTemplateCode(plan: TenantPlan) {
  try {
    const featureFlags = parseFeatureFlags(plan.featureFlagsJson || '{}');
    return typeof featureFlags.defaultColumnPermissionTemplateCode === 'string' ? featureFlags.defaultColumnPermissionTemplateCode : '';
  } catch {
    return '';
  }
}

function normalizeStringList(value: unknown) {
  if (!Array.isArray(value)) {
    return [];
  }
  return Array.from(new Set(value
    .filter((item): item is string => typeof item === 'string')
    .map((item) => item.trim())
    .filter(Boolean)));
}

function formatColumnPermissionTemplates(plan: TenantPlan) {
  const codes = resolveColumnPermissionTemplateCodes(plan);
  if (!codes.length) {
    return '-';
  }
  const nameByCode = new Map(columnPermissionTemplates.value.map((template) => [template.code, template.name]));
  return codes.map((code) => nameByCode.get(code) || code).join('、');
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
