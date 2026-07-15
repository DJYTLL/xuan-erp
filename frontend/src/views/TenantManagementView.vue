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

    <el-table v-loading="loading" :data="filteredTenants" row-key="id" border height="100%">
      <el-table-column prop="code" label="租户编码" min-width="150" />
      <el-table-column prop="name" label="租户名称" min-width="180" />
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="resolveTenantStatus(row.status).type" effect="plain">
            {{ resolveTenantStatus(row.status).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前套餐" min-width="140">
        <template #default="{ row }">
          <span>{{ row.currentPlanName || row.currentPlanCode || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="套餐到期" min-width="180">
        <template #default="{ row }">
          <span>{{ formatDateTime(row.currentPlanExpiresAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="主域名" min-width="180">
        <template #default="{ row }">
          <span>{{ row.primaryDomain || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="联系人" min-width="150">
        <template #default="{ row }">
          <span>{{ row.contactName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="联系电话" min-width="150">
        <template #default="{ row }">
          <span>{{ row.contactPhone || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开通完成" min-width="180">
        <template #default="{ row }">
          <span>{{ formatDateTime(row.provisionedAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <PermissionButton link type="primary" permission="tenant:update" no-permission-mode="disable" @click="openEditTenant(row)">编辑</PermissionButton>
          <el-button link type="primary" @click="openPlanAdjustment(row)">套餐调整</el-button>
          <el-button link type="primary" @click="openProvisionDrawer(row)">初始化任务</el-button>
          <el-button
            v-if="row.status === 'PROVISIONING'"
            link
            type="warning"
            @click="openProvisionDrawer(row)"
          >
            查看进度
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #pagination>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadTenants"
        @current-change="loadTenants"
      />
    </template>

    <el-dialog v-model="createDialogVisible" title="创建租户" width="760px" destroy-on-close>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="租户编码">
              <el-input v-model.trim="createForm.code" placeholder="acme" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="租户名称">
              <el-input v-model.trim="createForm.name" placeholder="玄云演示租户" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="租户套餐">
              <el-select
                v-model="createForm.planId"
                :loading="tenantPlanLoading"
                placeholder="请选择租户套餐"
                clearable
                filterable
              >
                <el-option
                  v-for="plan in enabledTenantPlans"
                  :key="plan.id"
                  :label="`${plan.name}（${plan.code}）`"
                  :value="plan.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="套餐到期时间">
              <el-date-picker
                v-model="createForm.planExpiresAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ssZ"
                placeholder="不设置到期时间"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model.trim="createForm.contactName" placeholder="系统管理员" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model.trim="createForm.contactPhone" placeholder="13800000000" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理员账号">
              <el-input v-model.trim="createForm.adminUsername" placeholder="admin" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理员初始密码">
              <el-input v-model="createForm.adminPassword" type="password" show-password placeholder="123456" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理员显示名">
              <el-input v-model.trim="createForm.adminDisplayName" placeholder="租户管理员" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理员邮箱">
              <el-input v-model.trim="createForm.adminEmail" placeholder="admin@example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理员手机号">
              <el-input v-model.trim="createForm.adminPhone" placeholder="13800000000" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="幂等键">
              <el-input v-model.trim="createForm.idempotencyKey" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model.trim="createForm.remark" type="textarea" :rows="3" placeholder="租户用途或来源" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingCreate" @click="submitCreateTenant">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑租户" width="760px" destroy-on-close>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="租户编码">
              <el-input v-model="editForm.code" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="租户名称">
              <el-input v-model.trim="editForm.name" placeholder="玄云演示租户" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="租户套餐">
              <el-select
                v-model="editForm.planId"
                :loading="tenantPlanLoading"
                placeholder="请选择租户套餐"
                filterable
              >
                <el-option
                  v-for="plan in enabledTenantPlans"
                  :key="plan.id"
                  :label="`${plan.name}（${plan.code}）`"
                  :value="plan.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="套餐到期时间">
              <el-date-picker
                v-model="editForm.planExpiresAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ssZ"
                placeholder="不设置到期时间"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model.trim="editForm.contactName" placeholder="系统管理员" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model.trim="editForm.contactPhone" placeholder="13800000000" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="幂等键">
              <el-input v-model.trim="editForm.idempotencyKey" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="套餐变更原因">
              <el-input v-model.trim="editForm.planChangeReason" placeholder="编辑租户资料调整套餐" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model.trim="editForm.remark" type="textarea" :rows="3" placeholder="租户用途或来源" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingEdit" @click="submitEditTenant">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="planAdjustmentDialogVisible" title="套餐调整" width="640px" destroy-on-close>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="租户">
              <el-input :model-value="planAdjustmentTenantLabel" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="当前套餐">
              <el-input :model-value="planAdjustmentCurrentPlanLabel" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="新套餐">
              <el-select
                v-model="planAdjustmentForm.planId"
                :loading="tenantPlanLoading"
                placeholder="请选择租户套餐"
                filterable
              >
                <el-option
                  v-for="plan in enabledTenantPlans"
                  :key="plan.id"
                  :label="`${plan.name}（${plan.code}）`"
                  :value="plan.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效时间">
              <el-date-picker
                v-model="planAdjustmentForm.effectiveAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ssZ"
                placeholder="立即生效"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="套餐到期时间">
              <el-date-picker
                v-model="planAdjustmentForm.expiresAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ssZ"
                placeholder="不设置到期时间"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作人">
              <el-input v-model.trim="planAdjustmentForm.assignedBy" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="变更原因">
              <el-input
                v-model.trim="planAdjustmentForm.changeReason"
                type="textarea"
                :rows="3"
                placeholder="例如续费、升级套餐、调整试用期"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="planAdjustmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingPlanAdjustment" @click="submitPlanAdjustment">保存</el-button>
      </template>
    </el-dialog>

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
          <el-tag v-if="selectedTenant" :type="tenantStatusMeta[selectedTenant.status]?.type || 'info'" effect="plain">
            {{ tenantStatusMeta[selectedTenant.status]?.label || selectedTenant.status }}
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

    <el-dialog v-model="taskRetryDialogVisible" title="失败重试" width="520px">
      <el-form label-position="top">
        <el-form-item label="任务">
          <el-input :model-value="retryTaskLabel" disabled />
        </el-form-item>
        <el-form-item label="步骤键">
          <el-input v-model.trim="taskRetryForm.stepKey" disabled />
        </el-form-item>
        <el-form-item label="重试原因">
          <el-input v-model.trim="taskRetryForm.reason" type="textarea" :rows="3" placeholder="请输入人工重试原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskRetryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingTaskRetry" @click="submitTaskRetry">提交重试</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="outboxRetryDialogVisible" title="Outbox 事件回放" width="520px">
      <el-form label-position="top">
        <el-form-item label="事件 ID">
          <el-input-number v-model="outboxRetryForm.eventId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="回放原因">
          <el-input v-model.trim="outboxRetryForm.reason" type="textarea" :rows="3" placeholder="请输入回放原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outboxRetryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingOutboxRetry" @click="submitOutboxRetry">提交回放</el-button>
      </template>
    </el-dialog>
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import {
  createTenantPlanAssignment,
  createTenant,
  listTenantPlans,
  listTenantProvisionTasks,
  listTenants,
  retryTenantOutboxEvent,
  retryTenantProvisionTask,
  updateTenant,
  updateTenantPlanAssignment,
} from '@/api/tenants';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
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
  planId: number | null;
  effectiveAt: string;
  expiresAt: string;
  assignedBy: string;
  changeReason: string;
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

const tenantStatusMeta: Record<TenantStatus, { label: string; type: TagType }> = {
  PROVISIONING: { label: '初始化中', type: 'warning' },
  PROVISIONED: { label: '已开通', type: 'primary' },
  ENABLED: { label: '启用', type: 'success' },
  SUSPENDED: { label: '暂停', type: 'warning' },
  DISABLED: { label: '停用', type: 'info' },
};

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
const loading = ref(false);
const keyword = ref('');
const statusFilter = ref<TenantStatus | ''>('');
const pageNum = ref(1);
const pageSize = ref(20);
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

const createForm = reactive<CreateTenantPayload>(emptyCreateForm());
const editForm = reactive<EditTenantForm>(emptyEditForm());
const planAdjustmentForm = reactive<PlanAdjustmentForm>(emptyPlanAdjustmentForm());
const taskRetryForm = reactive({
  stepKey: '',
  reason: '',
});
const outboxRetryForm = reactive({
  eventId: 1,
  reason: '',
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

onMounted(async () => {
  await Promise.all([loadTenants(), loadTenantPlans()]);
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
    ElMessage.error('租户列表加载失败，请确认网关已转发 /api/tenants');
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
    ElMessage.error('租户套餐加载失败，请确认网关已转发 /api/tenant-plans');
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
    planId: null,
    effectiveAt: '',
    expiresAt: '',
    assignedBy: authStore.username,
    changeReason: '',
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

async function submitCreateTenant() {
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

async function submitEditTenant() {
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
    }
    editDialogVisible.value = false;
    ElMessage.success('租户资料已保存');
    await loadTenants();
  } finally {
    submittingEdit.value = false;
  }
}

function openPlanAdjustment(row: Tenant) {
  planAdjustmentTenant.value = row;
  Object.assign(planAdjustmentForm, {
    tenantId: row.id,
    previousPlanId: row.currentPlanId,
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

async function submitPlanAdjustment() {
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
    planAdjustmentDialogVisible.value = false;
    ElMessage.success('租户套餐已调整');
    await loadTenants();
  } finally {
    submittingPlanAdjustment.value = false;
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
    stepKey: step.stepKey,
    reason: '',
  });
  taskRetryDialogVisible.value = true;
}

async function submitTaskRetry() {
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

async function submitOutboxRetry() {
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

function createIdempotencyKey() {
  return `tenant-create-${Date.now()}`;
}

function createUpdateIdempotencyKey(tenantId: number) {
  return `tenant-update-${tenantId}-${Date.now()}`;
}

function hasPlanChanged(tenant: Tenant) {
  return editForm.planId !== tenant.currentPlanId || (editForm.planExpiresAt || '') !== (tenant.currentPlanExpiresAt || '');
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}

function resolveTenantStatus(status: TenantStatus) {
  return tenantStatusMeta[status] || { label: status, type: 'info' as TagType };
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
