<template>
  <ListPageShell title="租户列表">
    <template #query>
      <QueryToolbar>
        <el-input v-model="keyword" class="query-input" placeholder="搜索租户编码 / 名称 / 联系人" clearable />
        <el-select v-model="statusFilter" class="status-filter" placeholder="状态" clearable>
          <el-option v-for="item in tenantStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadTenants" />
          <PermissionButton type="primary" permission="tenant:create" @click="openCreateTenant">创建租户</PermissionButton>
          <PermissionButton
            permission="tenant-provision:manage"
            no-permission-mode="disable"
            @click="openOutboxRetry"
          >
            Outbox 回放
          </PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <XuanBrowseTable
      v-loading="loading"
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      v-model:density="tableDensity"
      :schema="tenantBrowseTableSchema"
      :tenant-id="browseTenantId"
      :user-id="browseUserId"
      :data="filteredTenants"
      :column-permission-snapshot="tenantColumnPermissionSnapshot"
      :strict-column-permission-snapshot="strictColumnPermissionSnapshot"
      :total="total"
      height="100%"
      @row-action="handleTenantRowAction"
    />

    <DynamicFormDialog
      v-model="createDialogVisible"
      title="创建租户"
      :fields="createTenantFields"
      :sections="createTenantSections"
      :model="createForm"
      size="lg"
      label-position="top"
      confirm-permission="tenant:create"
      :loading="submittingCreate"
      @submit="submitCreateTenant"
    />

    <DynamicFormDialog
      v-model="editDialogVisible"
      title="编辑租户"
      :fields="editTenantFields"
      :sections="editTenantSections"
      :model="editForm"
      size="lg"
      label-position="top"
      confirm-permission="tenant:update"
      :loading="submittingEdit"
      @submit="submitEditTenant"
    />

    <DynamicFormDialog
      v-model="planAdjustmentDialogVisible"
      title="套餐调整"
      :fields="planAdjustmentFields"
      :sections="planAdjustmentSections"
      :model="planAdjustmentForm"
      size="md"
      label-position="top"
      confirm-permission="tenant-plan:assign"
      :loading="submittingPlanAdjustment"
      @submit="submitPlanAdjustment"
    />

    <DynamicFormDialog
      v-model="adminPasswordDialogVisible"
      :title="adminPasswordDialogTitle"
      description="重置当前租户 admin 管理员账号的登录密码"
      :fields="adminPasswordFields"
      :sections="adminPasswordSections"
      :model="adminPasswordForm"
      size="sm"
      label-position="top"
      confirm-text="确认重置"
      confirm-permission="tenant:admin-password:reset"
      :loading="submittingAdminPassword"
      @submit="submitAdminPasswordReset"
    />

    <el-drawer
      v-model="provisionDrawerVisible"
      :title="selectedTenant ? `${selectedTenant.name} 初始化任务` : '初始化任务'"
      size="720px"
      destroy-on-close
    >
      <section class="provision-summary">
        <div>
          <span>租户</span>
          <strong>{{ selectedTenant?.code || '-' }}</strong>
        </div>
        <div>
          <span>状态</span>
          <el-tag v-if="selectedTenant" :type="resolveTenantStatus(selectedTenant.status).type" effect="plain">
            {{ resolveTenantStatus(selectedTenant.status).label }}
          </el-tag>
        </div>
        <el-button :icon="RefreshCw" circle @click="loadProvisionTasks" />
      </section>

      <el-empty v-if="!provisionLoading && !provisionTasks.length" description="暂无初始化任务" />

      <el-timeline v-else v-loading="provisionLoading" class="provision-timeline">
        <el-timeline-item
          v-for="task in provisionTasks"
          :key="task.id"
          :type="provisionTaskStatusMeta[task.status]?.type || 'info'"
          :timestamp="task.taskKey"
        >
          <section class="task-panel">
            <header>
              <div>
                <strong>{{ task.taskType }}</strong>
                <span>#{{ task.id }}</span>
              </div>
              <el-tag :type="provisionTaskStatusMeta[task.status]?.type || 'info'" effect="plain">
                {{ provisionTaskStatusMeta[task.status]?.label || task.status }}
              </el-tag>
            </header>
            <div v-if="resolveProvisionErrorText(task)" class="provision-error">
              <span>失败原因</span>
              <strong>{{ resolveProvisionErrorText(task) }}</strong>
            </div>
            <el-table :data="task.steps" row-key="id" size="small" border>
              <el-table-column prop="sequenceNo" label="#" width="58" />
              <el-table-column prop="stepName" label="步骤" min-width="150" />
              <el-table-column prop="stepKey" label="步骤键" min-width="160" />
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="resolveProvisionStepStatus(row.status).type" effect="plain">
                    {{ resolveProvisionStepStatus(row.status).label }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="失败原因" min-width="180">
                <template #default="{ row }">
                  <span class="provision-error-text">{{ resolveProvisionErrorText(row) || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <PermissionButton
                    v-if="task.status === 'FAILED' || row.status === 'FAILED'"
                    link
                    type="primary"
                    permission="tenant-provision:manage"
                    no-permission-mode="disable"
                    @click="openTaskRetry(task, row)"
                  >
                    失败重试
                  </PermissionButton>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>

    <DynamicFormDialog
      v-model="taskRetryDialogVisible"
      title="失败重试"
      :fields="taskRetryFields"
      :sections="taskRetrySections"
      :model="taskRetryForm"
      size="sm"
      label-position="top"
      confirm-text="提交重试"
      confirm-permission="tenant-provision:manage"
      :loading="submittingTaskRetry"
      @submit="submitTaskRetry"
    />

    <DynamicFormDialog
      v-model="outboxRetryDialogVisible"
      title="Outbox 事件回放"
      :fields="outboxRetryFields"
      :sections="outboxRetrySections"
      :model="outboxRetryForm"
      size="sm"
      label-position="top"
      confirm-text="提交回放"
      confirm-permission="tenant-provision:manage"
      :loading="submittingOutboxRetry"
      @submit="submitOutboxRetry"
    />

    <DynamicFormDialog
      v-model="tenantActionReasonDialogVisible"
      :title="tenantActionReasonDialogTitle"
      :description="tenantActionReasonDialogDescription"
      :fields="tenantActionReasonFields"
      :model="tenantActionReasonForm"
      size="sm"
      label-position="top"
      :confirm-text="tenantActionReasonConfirmText"
      :confirm-permission="tenantActionReasonConfirmPermission"
      @submit="submitTenantActionReason"
    />
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import { resetIamTenantAdminPassword } from '@/api/iamAdmin';
import {
  createTenantPlanAssignment,
  createTenant,
  deleteTenant,
  disableTenant,
  enableTenant,
  listTenantPlans,
  listTenantProvisionTasks,
  repairTenantPermissionSync,
  listTenants,
  retryTenantOutboxEvent,
  retryTenantProvisionTask,
  updateTenant,
  updateTenantPlanAssignment,
} from '@/api/tenants';
import {
  createTenantBrowseTableSchema,
  resolveTenantPermissionSyncStatus,
  resolveTenantStatus,
} from '@/config/tenantBrowseTableSchema';
import DynamicFormDialog from '@/framework/components/DynamicFormDialog.vue';
import type { DynamicFormField, DynamicFormSection } from '@/framework/components/DynamicFormDialog.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import XuanBrowseTable from '@/framework/components/XuanBrowseTable.vue';
import type { BrowseTableDensity } from '@/framework/components/browseTablePreferences';
import type { BrowseTableColumnPermissionSnapshot } from '@/framework/components/browseTableSchema';
import { useAuthorizationStore } from '@/stores/authorization';
import { useAuthStore } from '@/stores/auth';
import type {
  CreateTenantPayload,
  Tenant,
  TenantPlan,
  TenantPlanAssignmentPayload,
  TenantProvisionTask,
  TenantProvisionTaskStep,
  TenantProvisionTaskStatus,
  TenantProvisionTaskStepStatus,
  TenantStatus,
  UpdateTenantPayload,
} from '@/types/tenant';

defineOptions({ name: 'TenantManagementView' });

type TagType = 'success' | 'warning' | 'info' | 'primary' | 'danger';

type PlanAdjustmentForm = {
  tenantId: number | null;
  previousPlanId: number | null;
  tenantLabel: string;
  currentPlanLabel: string;
  planId: number | null;
  effectiveAt: string;
  expiresAt: string;
  assignedBy: string;
  changeReason: string;
};

type TenantActionReasonForm = {
  reason: string;
};

type AdminPasswordResetForm = {
  tenantLabel: string;
  adminUsername: string;
  newPassword: string;
  confirmPassword: string;
  operator: string;
};

type EditTenantForm = UpdateTenantPayload & {
  tenantId: number | null;
  code: string;
  planId: number | null;
  planExpiresAt: string;
  planChangeReason: string;
};

const tenantStatusOptions: Array<{ value: TenantStatus; label: string }> = [
  { value: 'PROVISIONING', label: '初始化中' },
  { value: 'PROVISIONED', label: '已开通' },
  { value: 'ENABLED', label: '启用' },
  { value: 'SUSPENDED', label: '暂停' },
  { value: 'DISABLED', label: '停用' },
];

const provisionTaskStatusMeta: Record<TenantProvisionTaskStatus, { label: string; type: TagType }> = {
  PENDING: { label: '待执行', type: 'info' },
  RUNNING: { label: '执行中', type: 'primary' },
  SUCCEEDED: { label: '成功', type: 'success' },
  FAILED: { label: '失败', type: 'danger' },
  CANCELED: { label: '已取消', type: 'info' },
};

const provisionStepStatusMeta: Record<TenantProvisionTaskStepStatus, { label: string; type: TagType }> = {
  PENDING: { label: '待执行', type: 'info' },
  RUNNING: { label: '执行中', type: 'primary' },
  SUCCEEDED: { label: '成功', type: 'success' },
  FAILED: { label: '失败', type: 'danger' },
  SKIPPED: { label: '跳过', type: 'info' },
};

const authStore = useAuthStore();
const authorizationStore = useAuthorizationStore();
const browseTenantId = computed(() => String(authStore.tenantId ?? '0'));
const browseUserId = computed(() => authStore.currentUser?.username || 'anonymous');
const tenantRawColumnPermissions = computed(() => authorizationStore.columnPermissions.tenant || {});
const strictColumnPermissionSnapshot = computed(() => authorizationStore.isLoaded && Number(authStore.tenantId || 0) > 0);
const loading = ref(false);
const keyword = ref('');
const statusFilter = ref<TenantStatus | ''>('');
const pageNum = ref(1);
const pageSize = ref(20);
const tableDensity = ref<BrowseTableDensity>('default');
const total = ref(0);
const tenants = ref<Tenant[]>([]);
const tenantPlans = ref<TenantPlan[]>([]);
const tenantPlanLoading = ref(false);
const createDialogVisible = ref(false);
const submittingCreate = ref(false);
const editDialogVisible = ref(false);
const submittingEdit = ref(false);
const editingTenant = ref<Tenant | null>(null);
const planAdjustmentDialogVisible = ref(false);
const submittingPlanAdjustment = ref(false);
const planAdjustmentTenant = ref<Tenant | null>(null);
const adminPasswordDialogVisible = ref(false);
const submittingAdminPassword = ref(false);
const adminPasswordTenant = ref<Tenant | null>(null);
const lifecycleSubmittingTenantId = ref<number | null>(null);
const permissionSyncRepairingTenantId = ref<number | null>(null);
const provisionDrawerVisible = ref(false);
const provisionLoading = ref(false);
const selectedTenant = ref<Tenant | null>(null);
const provisionTasks = ref<TenantProvisionTask[]>([]);
const taskRetryDialogVisible = ref(false);
const submittingTaskRetry = ref(false);
const retryingTask = ref<TenantProvisionTask | null>(null);
const retryingStep = ref<TenantProvisionTaskStep | null>(null);
const outboxRetryDialogVisible = ref(false);
const submittingOutboxRetry = ref(false);
const tenantActionReasonDialogVisible = ref(false);
const tenantActionReasonDialogAction = ref<'enable' | 'disable' | 'delete'>('enable');
const tenantActionReasonDialogConfirmPermission = ref('tenant:enable');
const tenantActionReasonDialogTitle = ref('租户启用');
const tenantActionReasonDialogDescription = ref('请输入启用原因');
let tenantActionReasonResolver: ((value: string | null) => void) | null = null;
const tenantActionReasonConfirmText = computed(() => (
  tenantActionReasonDialogAction.value === 'delete'
    ? '删除'
    : tenantActionReasonDialogAction.value === 'disable'
      ? '停用'
      : '启用'
));
const tenantActionReasonConfirmPermission = computed(() => tenantActionReasonDialogConfirmPermission.value);

const createForm = reactive<CreateTenantPayload>(emptyCreateForm());
const editForm = reactive<EditTenantForm>(emptyEditForm());
const planAdjustmentForm = reactive<PlanAdjustmentForm>(emptyPlanAdjustmentForm());
const adminPasswordForm = reactive<AdminPasswordResetForm>(emptyAdminPasswordResetForm());
const taskRetryForm = reactive({
  taskLabel: '',
  stepKey: '',
  reason: '',
});
const outboxRetryForm = reactive({
  eventId: 1,
  reason: '',
});
const tenantActionReasonForm = reactive<TenantActionReasonForm>({
  reason: '',
});

watch(tenantActionReasonDialogVisible, (visible) => {
  if (!visible && tenantActionReasonResolver) {
    tenantActionReasonResolver(null);
    tenantActionReasonResolver = null;
  }
});

const filteredTenants = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  return tenants.value.filter((tenant) => {
    if (statusFilter.value && tenant.status !== statusFilter.value) {
      return false;
    }
    if (!value) {
      return true;
    }
    return [
      tenant.code,
      tenant.name,
      tenant.contactName || '',
      tenant.contactPhone || '',
      tenant.currentPlanName || '',
      tenant.primaryDomain || '',
    ].some((item) => item.toLowerCase().includes(value));
  });
});

const tenantColumnPermissionSnapshot = computed<BrowseTableColumnPermissionSnapshot>(() => {
  const tenantPermissions = tenantRawColumnPermissions.value;
  return Object.fromEntries(
    Object.entries(tenantPermissions)
      .filter(([, accessMode]) => (
        accessMode === 'VISIBLE'
        || accessMode === 'MASKED'
        || accessMode === 'HIDDEN'
      ))
      .map(([columnKey, accessMode]) => [`tenant::${columnKey}`, accessMode]),
  );
});

const tenantBrowseTableSchema = computed(() => createTenantBrowseTableSchema({
  pageCode: 'tenant-management',
  tableCode: 'tenant-list',
  actionsWidth: 640,
  defaultDensity: 'default',
  defaultPageSize: 20,
  toolbar: {
    showDensity: true,
    showColumnSetting: true,
    actions: [],
  },
  rowActions: [
    { key: 'edit', label: '编辑', type: 'primary', permission: 'tenant:update', noPermissionMode: 'disable' },
    {
      key: 'reset-admin-password',
      label: '重置管理员密码',
      type: 'warning',
      permission: 'tenant:admin-password:reset',
      noPermissionMode: 'disable',
    },
    { key: 'plan-adjustment', label: '套餐调整', type: 'primary', permission: 'tenant-plan:assign', noPermissionMode: 'disable' },
    {
      key: 'permission-sync-repair',
      label: '立即修复',
      type: 'warning',
      permission: 'tenant-plan:assign',
      noPermissionMode: 'disable',
      visible: (row) => needsPermissionSyncRepair(row),
      disabled: (row) => permissionSyncRepairingTenantId.value === row.id,
      disabledReason: (row) => permissionSyncRepairingTenantId.value === row.id ? '权限同步修复中' : '',
    },
    {
      key: 'enable',
      label: '启用',
      type: 'success',
      permission: 'tenant:enable',
      noPermissionMode: 'disable',
      stateResource: 'tenant',
      stateCode: (row) => row.status,
      stateAction: 'enable',
      stateNoPermissionReason: '当前租户状态不可执行该动作',
      visible: (row) => canEnableTenant(row),
      disabledReason: (row) => resolveTenantActionDisabledReason(row),
    },
    {
      key: 'disable',
      label: '停用',
      type: 'warning',
      permission: 'tenant:disable',
      noPermissionMode: 'disable',
      stateResource: 'tenant',
      stateCode: (row) => row.status,
      stateAction: 'disable',
      stateNoPermissionReason: '当前租户状态不可执行该动作',
      visible: (row) => canDisableTenant(row),
      disabledReason: (row) => resolveTenantActionDisabledReason(row),
    },
    {
      key: 'delete',
      label: '删除',
      type: 'danger',
      permission: 'tenant:delete',
      noPermissionMode: 'disable',
      stateResource: 'tenant',
      stateCode: (row) => row.status,
      stateAction: 'delete',
      stateNoPermissionReason: '当前租户状态不可执行该动作',
      visible: (row) => canDeleteTenant(row),
      disabledReason: (row) => resolveTenantActionDisabledReason(row),
    },
    {
      key: 'provision-task',
      label: '初始化任务',
      type: 'primary',
      permission: 'tenant-provision:view',
      noPermissionMode: 'disable',
    },
    {
      key: 'provision-progress',
      label: '查看进度',
      type: 'warning',
      permission: 'tenant-provision:view',
      noPermissionMode: 'disable',
      visible: (row) => row.status === 'PROVISIONING',
    },
  ],
  emptyState: {
    title: '暂无租户',
    description: '当前筛选条件下没有租户数据。',
  },
}));

const enabledTenantPlans = computed(() => tenantPlans.value.filter((plan) => plan.status === 'ENABLED'));

const retryTaskLabel = computed(() => {
  if (!retryingTask.value) {
    return '-';
  }
  return `${retryingTask.value.taskType} #${retryingTask.value.id}`;
});

const planAdjustmentTenantLabel = computed(() => {
  if (!planAdjustmentTenant.value) {
    return '-';
  }
  return `${planAdjustmentTenant.value.name}（${planAdjustmentTenant.value.code}）`;
});

const planAdjustmentCurrentPlanLabel = computed(() => {
  const tenant = planAdjustmentTenant.value;
  if (!tenant) {
    return '-';
  }
  return tenant.currentPlanName || tenant.currentPlanCode || '未绑定套餐';
});

const adminPasswordDialogTitle = computed(() => (
  adminPasswordTenant.value ? `重置管理员密码 - ${tenantLabel(adminPasswordTenant.value)}` : '重置管理员密码'
));

const tenantPlanSelectOptions = computed(() => enabledTenantPlans.value.map((plan) => ({
  label: `${plan.name}（${plan.code}）`,
  value: plan.id,
})));

const createTenantFields = computed<DynamicFormField[]>(() => [
  { key: 'code', label: '租户编码', placeholder: 'acme', required: true, span: 12 },
  { key: 'name', label: '租户名称', placeholder: '玄云演示租户', required: true, span: 12 },
  {
    key: 'planId',
    label: '租户套餐',
    component: 'select',
    placeholder: '请选择租户套餐',
    clearable: true,
    options: tenantPlanSelectOptions.value,
    span: 12,
  },
  {
    key: 'planExpiresAt',
    label: '套餐到期时间',
    component: 'datetime',
    placeholder: '不设置到期时间',
    clearable: true,
    span: 12,
  },
  { key: 'contactName', label: '联系人', placeholder: '系统管理员', span: 12 },
  { key: 'contactPhone', label: '联系电话', placeholder: '13800000000', span: 12 },
  { key: 'adminUsername', label: '管理员账号', placeholder: 'admin', required: true, span: 12 },
  { key: 'adminPassword', label: '管理员初始密码', component: 'password', placeholder: '123456', required: true, span: 12 },
  { key: 'adminDisplayName', label: '管理员显示名', placeholder: '租户管理员', span: 12 },
  { key: 'adminEmail', label: '管理员邮箱', placeholder: 'admin@example.com', span: 12 },
  { key: 'adminPhone', label: '管理员手机号', placeholder: '13800000000', span: 12 },
  { key: 'idempotencyKey', label: '幂等键', span: 12 },
  { key: 'remark', label: '备注', component: 'textarea', placeholder: '租户用途或来源', rows: 3, span: 24 },
]);

const editTenantFields = computed<DynamicFormField[]>(() => [
  { key: 'code', label: '租户编码', disabled: true, span: 12 },
  { key: 'name', label: '租户名称', placeholder: '玄云演示租户', required: true, span: 12 },
  {
    key: 'planId',
    label: '租户套餐',
    component: 'select',
    placeholder: '请选择租户套餐',
    clearable: true,
    options: tenantPlanSelectOptions.value,
    span: 12,
  },
  {
    key: 'planExpiresAt',
    label: '套餐到期时间',
    component: 'datetime',
    placeholder: '不设置到期时间',
    clearable: true,
    span: 12,
  },
  { key: 'contactName', label: '联系人', placeholder: '系统管理员', span: 12 },
  { key: 'contactPhone', label: '联系电话', placeholder: '13800000000', span: 12 },
  { key: 'idempotencyKey', label: '幂等键', span: 12 },
  { key: 'planChangeReason', label: '套餐变更原因', component: 'textarea', placeholder: '编辑租户资料调整套餐', rows: 3, span: 12 },
  { key: 'remark', label: '备注', component: 'textarea', placeholder: '租户用途或来源', rows: 3, span: 24 },
]);

const planAdjustmentFields = computed<DynamicFormField[]>(() => [
  { key: 'tenantLabel', label: '租户', disabled: true, span: 12 },
  { key: 'currentPlanLabel', label: '当前套餐', disabled: true, span: 12 },
  {
    key: 'planId',
    label: '新套餐',
    component: 'select',
    placeholder: '请选择租户套餐',
    clearable: true,
    options: tenantPlanSelectOptions.value,
    span: 12,
  },
  {
    key: 'effectiveAt',
    label: '生效时间',
    component: 'datetime',
    placeholder: '立即生效',
    clearable: true,
    span: 12,
  },
  {
    key: 'expiresAt',
    label: '套餐到期时间',
    component: 'datetime',
    placeholder: '不设置到期时间',
    clearable: true,
    span: 12,
  },
  { key: 'assignedBy', label: '操作人', span: 12 },
  { key: 'changeReason', label: '变更原因', component: 'textarea', placeholder: '例如续费、升级套餐、调整试用期', rows: 3, span: 24 },
]);

const adminPasswordFields = computed<DynamicFormField[]>(() => [
  { key: 'tenantLabel', label: '租户', disabled: true, span: 24 },
  { key: 'adminUsername', label: '管理员账号', disabled: true, span: 12 },
  { key: 'operator', label: '操作人', disabled: true, span: 12 },
  { key: 'newPassword', label: '新密码', component: 'password', placeholder: '请输入新密码', required: true, span: 12 },
  { key: 'confirmPassword', label: '确认新密码', component: 'password', placeholder: '请再次输入新密码', required: true, span: 12 },
]);

const taskRetryFields = computed<DynamicFormField[]>(() => [
  { key: 'taskLabel', label: '任务', disabled: true, span: 12 },
  { key: 'stepKey', label: '步骤键', disabled: true, span: 12 },
  { key: 'reason', label: '重试原因', component: 'textarea', placeholder: '请输入人工重试原因', rows: 3, span: 24 },
]);

const outboxRetryFields = computed<DynamicFormField[]>(() => [
  { key: 'eventId', label: '事件 ID', component: 'number', scale: 0, inputMode: 'numeric', align: 'right', span: 12 },
  { key: 'reason', label: '回放原因', component: 'textarea', placeholder: '请输入回放原因', rows: 3, span: 24 },
]);

const tenantActionReasonFields = computed<DynamicFormField[]>(() => [
  { key: 'reason', label: '原因', component: 'textarea', placeholder: '请输入处理原因', rows: 3, required: true, span: 24 },
]);

const createTenantSections = computed<DynamicFormSection[]>(() => [{ fields: createTenantFields.value }]);
const editTenantSections = computed<DynamicFormSection[]>(() => [{ fields: editTenantFields.value }]);
const planAdjustmentSections = computed<DynamicFormSection[]>(() => [{ fields: planAdjustmentFields.value }]);
const adminPasswordSections = computed<DynamicFormSection[]>(() => [{ fields: adminPasswordFields.value }]);
const taskRetrySections = computed<DynamicFormSection[]>(() => [{ fields: taskRetryFields.value }]);
const outboxRetrySections = computed<DynamicFormSection[]>(() => [{ fields: outboxRetryFields.value }]);
let tenantPageWatchReady = false;

onMounted(async () => {
  await Promise.all([loadTenants(), loadTenantPlans()]);
  tenantPageWatchReady = true;
});

watch([pageNum, pageSize], ([nextPageNum, nextPageSize], [previousPageNum, previousPageSize]) => {
  if (!tenantPageWatchReady) {
    return;
  }
  if (nextPageNum === previousPageNum && nextPageSize === previousPageSize) {
    return;
  }
  void loadTenants();
});

async function loadTenants() {
  loading.value = true;
  try {
    const page = await listTenants(pageNum.value, pageSize.value);
    tenants.value = page.records;
    total.value = page.total;
    pageNum.value = page.pageNum;
    pageSize.value = page.pageSize;
  } catch {
    tenants.value = [];
    total.value = 0;
    // 真实错误原因统一由 HTTP 拦截器展示，这里只清空当前页数据。
  } finally {
    loading.value = false;
  }
}

async function loadTenantPlans() {
  tenantPlanLoading.value = true;
  try {
    tenantPlans.value = await listTenantPlans();
  } catch {
    tenantPlans.value = [];
    // 真实错误原因统一由 HTTP 拦截器展示，这里只清空套餐列表。
  } finally {
    tenantPlanLoading.value = false;
  }
}

function emptyCreateForm(): CreateTenantPayload {
  return {
    code: '',
    name: '',
    contactName: '',
    contactPhone: '',
    remark: '',
    idempotencyKey: createIdempotencyKey(),
    adminUsername: 'admin',
    adminPassword: '',
    adminDisplayName: '租户管理员',
    adminEmail: '',
    adminPhone: '',
    planId: null,
    planExpiresAt: '',
  };
}

function emptyPlanAdjustmentForm(): PlanAdjustmentForm {
  return {
    tenantId: null,
    previousPlanId: null,
    tenantLabel: '',
    currentPlanLabel: '',
    planId: null,
    effectiveAt: '',
    expiresAt: '',
    assignedBy: authStore.username,
    changeReason: '',
  };
}

function emptyAdminPasswordResetForm(): AdminPasswordResetForm {
  return {
    tenantLabel: '',
    adminUsername: 'admin',
    newPassword: '',
    confirmPassword: '',
    operator: authStore.username,
  };
}

function emptyEditForm(): EditTenantForm {
  return {
    tenantId: null,
    code: '',
    name: '',
    contactName: '',
    contactPhone: '',
    remark: '',
    idempotencyKey: '',
    planId: null,
    planExpiresAt: '',
    planChangeReason: '',
  };
}

function openCreateTenant() {
  Object.assign(createForm, emptyCreateForm());
  if (!tenantPlans.value.length) {
    void loadTenantPlans();
  }
  createDialogVisible.value = true;
}

function handleTenantRowAction({ actionKey, row }: { actionKey: string; row: Tenant }) {
  if (actionKey === 'edit') {
    openEditTenant(row);
    return;
  }
  if (actionKey === 'reset-admin-password') {
    openAdminPasswordReset(row);
    return;
  }
  if (actionKey === 'plan-adjustment') {
    openPlanAdjustment(row);
    return;
  }
  if (actionKey === 'permission-sync-repair') {
    void submitPermissionSyncRepair(row);
    return;
  }
  if (actionKey === 'enable') {
    void submitEnableTenant(row);
    return;
  }
  if (actionKey === 'disable') {
    void submitDisableTenant(row);
    return;
  }
  if (actionKey === 'delete') {
    void submitDeleteTenant(row);
    return;
  }
  if (actionKey === 'provision-task' || actionKey === 'provision-progress') {
    void openProvisionDrawer(row);
  }
}

function openAdminPasswordReset(row: Tenant) {
  adminPasswordTenant.value = row;
  Object.assign(adminPasswordForm, {
    tenantLabel: tenantLabel(row),
    adminUsername: 'admin',
    newPassword: '',
    confirmPassword: '',
    operator: authStore.username,
  });
  adminPasswordDialogVisible.value = true;
}

async function submitAdminPasswordReset(value: Record<string, unknown>) {
  Object.assign(adminPasswordForm, value);
  const tenant = adminPasswordTenant.value;
  if (!tenant) {
    return;
  }
  if (!adminPasswordForm.newPassword.trim()) {
    ElMessage.warning('请填写新密码');
    return;
  }
  if (adminPasswordForm.newPassword !== adminPasswordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }
  submittingAdminPassword.value = true;
  try {
    await resetIamTenantAdminPassword(tenant.id, {
      newPassword: adminPasswordForm.newPassword,
      operator: authStore.username,
    });
    adminPasswordDialogVisible.value = false;
    ElMessage.success('租户 admin 管理员密码已重置');
  } finally {
    submittingAdminPassword.value = false;
  }
}

async function submitCreateTenant(value: Record<string, unknown>) {
  Object.assign(createForm, value);
  if (!createForm.code?.trim() || !createForm.name?.trim()) {
    ElMessage.warning('请填写租户编码和租户名称');
    return;
  }
  submittingCreate.value = true;
  try {
    const createdTenant = await createTenant(normalizeCreatePayload(createForm));
    createDialogVisible.value = false;
    ElMessage.success('租户已创建，初始化任务已启动');
    await loadTenants();
    selectedTenant.value = createdTenant;
    provisionDrawerVisible.value = true;
    await loadProvisionTasks();
  } finally {
    submittingCreate.value = false;
  }
}

function openEditTenant(row: Tenant) {
  editingTenant.value = row;
  Object.assign(editForm, {
    tenantId: row.id,
    code: row.code,
    name: row.name,
    contactName: row.contactName || '',
    contactPhone: row.contactPhone || '',
    remark: row.remark || '',
    idempotencyKey: createUpdateIdempotencyKey(row.id),
    planId: row.currentPlanId,
    planExpiresAt: row.currentPlanExpiresAt || '',
    planChangeReason: '',
  });
  if (!tenantPlans.value.length) {
    void loadTenantPlans();
  }
  editDialogVisible.value = true;
}

async function submitEditTenant(value: Record<string, unknown>) {
  Object.assign(editForm, value);
  const tenant = editingTenant.value;
  if (!tenant || !editForm.tenantId) {
    return;
  }
  if (!editForm.name.trim()) {
    ElMessage.warning('请填写租户名称');
    return;
  }
  const planChanged = hasPlanChanged(tenant);
  if (planChanged && !editForm.planId) {
    ElMessage.warning('请选择租户套餐');
    return;
  }
  if (planChanged && !editForm.planChangeReason.trim()) {
    ElMessage.warning('请填写套餐变更原因');
    return;
  }
  submittingEdit.value = true;
  try {
    await updateTenant(tenant.id, normalizeUpdatePayload(editForm));
    if (planChanged) {
      const payload = normalizePlanAssignmentPayload({
        tenantId: tenant.id,
        previousPlanId: tenant.currentPlanId,
        tenantLabel: `${tenant.name}（${tenant.code}）`,
        currentPlanLabel: tenant.currentPlanName || tenant.currentPlanCode || '未绑定套餐',
        planId: editForm.planId,
        effectiveAt: '',
        expiresAt: editForm.planExpiresAt,
        assignedBy: authStore.username,
        changeReason: editForm.planChangeReason,
      });
      if (tenant.currentPlanAssignmentId) {
        await updateTenantPlanAssignment(tenant.currentPlanAssignmentId, payload);
      } else {
        await createTenantPlanAssignment(payload);
      }
      await refreshCurrentAuthorizationIfTenantAffected(tenant.id);
    }
    editDialogVisible.value = false;
    ElMessage.success('租户资料已保存');
    await loadTenants();
  } finally {
    submittingEdit.value = false;
  }
}

async function refreshCurrentAuthorizationIfTenantAffected(tenantId: number | null | undefined) {
  if (!Number.isFinite(Number(tenantId)) || Number(tenantId) !== authStore.tenantId) {
    return;
  }
  await authorizationStore.refreshCurrentAuthorizationContext();
}

function openPlanAdjustment(row: Tenant) {
  planAdjustmentTenant.value = row;
  Object.assign(planAdjustmentForm, {
    tenantId: row.id,
    previousPlanId: row.currentPlanId,
    tenantLabel: `${row.name}（${row.code}）`,
    currentPlanLabel: row.currentPlanName || row.currentPlanCode || '未绑定套餐',
    planId: row.currentPlanId,
    effectiveAt: '',
    expiresAt: row.currentPlanExpiresAt || '',
    assignedBy: authStore.username,
    changeReason: '',
  });
  if (!tenantPlans.value.length) {
    void loadTenantPlans();
  }
  planAdjustmentDialogVisible.value = true;
}

async function submitPlanAdjustment(value: Record<string, unknown>) {
  Object.assign(planAdjustmentForm, value);
  if (!planAdjustmentTenant.value || !planAdjustmentForm.tenantId) {
    return;
  }
  if (!planAdjustmentForm.planId) {
    ElMessage.warning('请选择租户套餐');
    return;
  }
  if (!planAdjustmentForm.changeReason.trim()) {
    ElMessage.warning('请填写套餐变更原因');
    return;
  }
  const payload = normalizePlanAssignmentPayload(planAdjustmentForm);
  submittingPlanAdjustment.value = true;
  try {
    if (planAdjustmentTenant.value.currentPlanAssignmentId) {
      await updateTenantPlanAssignment(planAdjustmentTenant.value.currentPlanAssignmentId, payload);
    } else {
      await createTenantPlanAssignment(payload);
    }
    await refreshCurrentAuthorizationIfTenantAffected(planAdjustmentTenant.value.id);
    planAdjustmentDialogVisible.value = false;
    ElMessage.success('租户套餐已调整');
    await loadTenants();
  } finally {
    submittingPlanAdjustment.value = false;
  }
}

async function submitPermissionSyncRepair(row: Tenant) {
  permissionSyncRepairingTenantId.value = row.id;
  try {
    await repairTenantPermissionSync(row.id);
    await refreshCurrentAuthorizationIfTenantAffected(row.id);
    ElMessage.success('权限同步已修复');
    await loadTenants();
  } finally {
    permissionSyncRepairingTenantId.value = null;
  }
}

async function submitEnableTenant(row: Tenant) {
  const reason = await requestTenantActionReason(row, '启用', '租户启用');
  if (!reason) {
    return;
  }
  lifecycleSubmittingTenantId.value = row.id;
  try {
    await enableTenant(row.id, {
      reason,
      operator: authStore.username,
      idempotencyKey: createLifecycleIdempotencyKey(row.id, 'enable'),
    });
    ElMessage.success('租户已启用');
    await loadTenants();
  } catch {
    // http 拦截器已展示后端返回的真实错误。
  } finally {
    lifecycleSubmittingTenantId.value = null;
  }
}

async function submitDisableTenant(row: Tenant) {
  const reason = await requestTenantActionReason(row, '停用', '租户停用');
  if (!reason) {
    return;
  }
  lifecycleSubmittingTenantId.value = row.id;
  try {
    await disableTenant(row.id, {
      reason,
      operator: authStore.username,
      idempotencyKey: createLifecycleIdempotencyKey(row.id, 'disable'),
    });
    ElMessage.success('租户已停用');
    await loadTenants();
  } catch {
    // http 拦截器已展示后端返回的真实错误。
  } finally {
    lifecycleSubmittingTenantId.value = null;
  }
}

async function submitDeleteTenant(row: Tenant) {
  const reason = await requestTenantActionReason(row, '删除', '租户删除');
  if (!reason) {
    return;
  }
  lifecycleSubmittingTenantId.value = row.id;
  try {
    await deleteTenant(row.id, {
      reason,
      operator: authStore.username,
      idempotencyKey: createLifecycleIdempotencyKey(row.id, 'delete'),
    });
    if (selectedTenant.value?.id === row.id) {
      provisionDrawerVisible.value = false;
      selectedTenant.value = null;
      provisionTasks.value = [];
    }
    ElMessage.success('租户已删除');
    await loadTenants();
  } catch {
    // http 拦截器已展示后端返回的真实错误。
  } finally {
    lifecycleSubmittingTenantId.value = null;
  }
}

async function openProvisionDrawer(row: Tenant) {
  selectedTenant.value = row;
  provisionDrawerVisible.value = true;
  await loadProvisionTasks();
}

async function loadProvisionTasks() {
  if (!selectedTenant.value) {
    return;
  }
  provisionLoading.value = true;
  try {
    provisionTasks.value = await listTenantProvisionTasks(selectedTenant.value.id);
  } catch {
    provisionTasks.value = [];
    ElMessage.error('初始化任务加载失败，请稍后重试');
  } finally {
    provisionLoading.value = false;
  }
}

function openTaskRetry(task: TenantProvisionTask, step: TenantProvisionTaskStep) {
  retryingTask.value = task;
  retryingStep.value = step;
  Object.assign(taskRetryForm, {
    taskLabel: `${task.taskType} #${task.id}`,
    stepKey: step.stepKey,
    reason: '',
  });
  taskRetryDialogVisible.value = true;
}

async function submitTaskRetry(value: Record<string, unknown>) {
  Object.assign(taskRetryForm, value);
  if (!retryingTask.value || !retryingStep.value) {
    return;
  }
  if (!taskRetryForm.reason.trim()) {
    ElMessage.warning('请填写重试原因');
    return;
  }
  submittingTaskRetry.value = true;
  try {
    await retryTenantProvisionTask(retryingTask.value.id, {
      stepKey: taskRetryForm.stepKey,
      operator: authStore.username,
      reason: taskRetryForm.reason,
    });
    taskRetryDialogVisible.value = false;
    ElMessage.success('初始化任务已提交重试');
    await Promise.all([loadProvisionTasks(), loadTenants()]);
  } finally {
    submittingTaskRetry.value = false;
  }
}

function openOutboxRetry() {
  Object.assign(outboxRetryForm, {
    eventId: 1,
    reason: '',
  });
  outboxRetryDialogVisible.value = true;
}

async function submitOutboxRetry(value: Record<string, unknown>) {
  Object.assign(outboxRetryForm, value);
  if (!outboxRetryForm.eventId || outboxRetryForm.eventId < 1) {
    ElMessage.warning('请填写有效事件 ID');
    return;
  }
  if (!outboxRetryForm.reason.trim()) {
    ElMessage.warning('请填写回放原因');
    return;
  }
  submittingOutboxRetry.value = true;
  try {
    await retryTenantOutboxEvent(outboxRetryForm.eventId, {
      operator: authStore.username,
      reason: outboxRetryForm.reason,
    });
    outboxRetryDialogVisible.value = false;
    ElMessage.success('Outbox 事件已提交回放');
  } finally {
    submittingOutboxRetry.value = false;
  }
}

function normalizeCreatePayload(payload: CreateTenantPayload): CreateTenantPayload {
  return Object.fromEntries(
    Object.entries(payload)
      .map(([key, value]) => [key, typeof value === 'string' ? value.trim() : value])
      .filter(([, value]) => value !== ''),
  ) as CreateTenantPayload;
}

function normalizeUpdatePayload(payload: EditTenantForm): UpdateTenantPayload {
  return Object.fromEntries(
    Object.entries({
      name: payload.name,
      contactName: payload.contactName,
      contactPhone: payload.contactPhone,
      remark: payload.remark,
      idempotencyKey: payload.idempotencyKey,
    })
      .map(([key, value]) => [key, typeof value === 'string' ? value.trim() : value])
      .filter(([, value]) => value !== ''),
  ) as UpdateTenantPayload;
}

function normalizePlanAssignmentPayload(form: PlanAdjustmentForm): TenantPlanAssignmentPayload {
  const payload: TenantPlanAssignmentPayload = {
    tenantId: form.tenantId as number,
    previousPlanId: form.previousPlanId,
    planId: form.planId as number,
    status: 'ACTIVE',
    effectiveAt: form.effectiveAt || null,
    expiresAt: form.expiresAt || null,
    assignedAt: new Date().toISOString(),
    assignedBy: form.assignedBy.trim() || authStore.username,
    changeReason: form.changeReason.trim(),
    source: 'TENANT_MANAGEMENT',
  };
  return payload;
}

function tenantLabel(tenant: Tenant) {
  return `${tenant.name}（${tenant.code}）`;
}

function createIdempotencyKey() {
  return `tenant-create-${Date.now()}`;
}

function createUpdateIdempotencyKey(tenantId: number) {
  return `tenant-update-${tenantId}-${Date.now()}`;
}

function createLifecycleIdempotencyKey(tenantId: number, action: 'enable' | 'disable' | 'delete') {
  return `tenant-${action}-${tenantId}-${Date.now()}`;
}

function hasPlanChanged(tenant: Tenant) {
  return editForm.planId !== tenant.currentPlanId || (editForm.planExpiresAt || '') !== (tenant.currentPlanExpiresAt || '');
}

async function requestTenantActionReason(row: Tenant, actionLabel: string, defaultReason: string): Promise<string | null> {
  tenantActionReasonDialogAction.value = actionLabel === '启用' ? 'enable' : actionLabel === '停用' ? 'disable' : 'delete';
  tenantActionReasonDialogConfirmPermission.value = `tenant:${tenantActionReasonDialogAction.value}`;
  tenantActionReasonDialogTitle.value = `${actionLabel}租户 ${row.name}（${row.code}）`;
  tenantActionReasonDialogDescription.value = `请输入${actionLabel}原因`;
  tenantActionReasonForm.reason = defaultReason;
  tenantActionReasonDialogVisible.value = true;
  return new Promise<string | null>((resolve) => {
    tenantActionReasonResolver = resolve;
  });
}

async function submitTenantActionReason(value: Record<string, unknown>) {
  Object.assign(tenantActionReasonForm, value);
  const reason = tenantActionReasonForm.reason.trim();
  if (!reason) {
    ElMessage.warning(`${tenantActionReasonDialogAction.value === 'delete' ? '删除' : tenantActionReasonDialogAction.value === 'disable' ? '停用' : '启用'}原因不能为空`);
    return;
  }
  const resolve = tenantActionReasonResolver;
  tenantActionReasonResolver = null;
  tenantActionReasonDialogVisible.value = false;
  resolve?.(reason);
}

function canEnableTenant(tenant: Tenant) {
  return tenant.status === 'PROVISIONED' || tenant.status === 'DISABLED';
}

function canDisableTenant(tenant: Tenant) {
  return tenant.status === 'ENABLED';
}

function canDeleteTenant(tenant: Tenant) {
  return tenant.status !== 'ENABLED';
}

function resolveTenantActionDisabledReason(tenant: Tenant) {
  return lifecycleSubmittingTenantId.value === tenant.id ? '租户操作提交中' : '';
}

function needsPermissionSyncRepair(tenant: Tenant) {
  return resolveTenantPermissionSyncStatus(tenant.permissionSyncStatus).label === '待修复';
}

function resolveProvisionStepStatus(status: TenantProvisionTaskStepStatus) {
  return provisionStepStatusMeta[status] || { label: status, type: 'info' as TagType };
}

function resolveProvisionErrorText(item: Pick<TenantProvisionTask | TenantProvisionTaskStep, 'lastErrorCode' | 'lastErrorMessage'>) {
  return item.lastErrorMessage || item.lastErrorCode || '';
}
</script>

<style scoped>
.status-filter {
  width: 150px;
}

.provision-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(120px, auto) auto;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  padding: 12px;
  background: var(--xuan-panel);
}

.provision-summary div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.provision-summary span {
  color: var(--xuan-muted);
  font-size: 12px;
}

.provision-summary strong {
  color: var(--xuan-text);
}

.provision-timeline {
  padding: 4px 4px 4px 0;
}

.task-panel {
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--xuan-panel);
}

.task-panel header {
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--xuan-border);
}

.task-panel header div {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.task-panel header span {
  color: var(--xuan-muted);
  font-size: 12px;
}

.provision-error {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--xuan-border);
  background: rgba(245, 108, 108, 0.08);
}

.provision-error span {
  color: var(--xuan-muted);
  font-size: 12px;
}

.provision-error strong,
.provision-error-text {
  color: var(--el-color-danger);
  font-weight: 500;
  overflow-wrap: anywhere;
}

@media (max-width: 720px) {
  .status-filter {
    width: 100%;
  }

  .provision-summary {
    grid-template-columns: 1fr auto;
  }
}
</style>
