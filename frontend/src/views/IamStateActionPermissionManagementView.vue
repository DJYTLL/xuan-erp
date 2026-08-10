<template>
  <ListPageShell title="状态动作权限">
    <template #query>
      <QueryToolbar>
        <el-input v-model.number="tenantId" class="query-input" placeholder="租户 ID，0 为平台通用" />
        <el-input v-model="resourceKey" class="query-input" placeholder="资源标识" clearable />
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadData" />
          <PermissionButton type="primary" permission="iam-state-action:create" no-permission-mode="disable" @click="openCreateState">
            新增状态
          </PermissionButton>
          <PermissionButton type="primary" permission="iam-state-action:create" no-permission-mode="disable" @click="openCreateAction">
            新增动作
          </PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="资源状态" name="states">
        <el-table v-loading="loading" :data="states" row-key="id" border>
          <el-table-column prop="resourceKey" label="资源" min-width="150" />
          <el-table-column prop="stateCode" label="状态编码" min-width="130" />
          <el-table-column prop="stateName" label="状态名称" min-width="140" />
          <el-table-column prop="description" label="说明" min-width="220" />
          <el-table-column prop="sortNo" label="排序" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <PermissionButton link type="primary" permission="iam-state-action:update" no-permission-mode="disable" @click="openEditState(row)">
                编辑
              </PermissionButton>
              <PermissionButton link :type="row.enabled ? 'warning' : 'success'" permission="iam-state-action:update" no-permission-mode="disable" @click="toggleState(row)">
                {{ row.enabled ? '停用' : '启用' }}
              </PermissionButton>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="资源动作" name="actions">
        <el-table v-loading="loading" :data="actions" row-key="id" border>
          <el-table-column prop="resourceKey" label="资源" min-width="150" />
          <el-table-column prop="actionCode" label="动作编码" min-width="130" />
          <el-table-column prop="actionName" label="动作名称" min-width="140" />
          <el-table-column prop="permissionCode" label="基础权限码" min-width="170" />
          <el-table-column prop="description" label="说明" min-width="220" />
          <el-table-column prop="sortNo" label="排序" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <PermissionButton link type="primary" permission="iam-state-action:update" no-permission-mode="disable" @click="openEditAction(row)">
                编辑
              </PermissionButton>
              <PermissionButton link :type="row.enabled ? 'warning' : 'success'" permission="iam-state-action:update" no-permission-mode="disable" @click="toggleAction(row)">
                {{ row.enabled ? '停用' : '启用' }}
              </PermissionButton>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <DynamicFormDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :fields="dialogFields"
      :sections="dialogSections"
      :model="form"
      size="lg"
      label-position="top"
      :confirm-permission="editingId ? 'iam-state-action:update' : 'iam-state-action:create'"
      @submit="submitForm"
    />
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import {
  createIamResourceAction,
  createIamResourceState,
  listIamResourceActions,
  listIamResourceStates,
  setIamResourceActionEnabled,
  setIamResourceStateEnabled,
  updateIamResourceAction,
  updateIamResourceState,
} from '@/api/iamAdmin';
import DynamicFormDialog from '@/framework/components/DynamicFormDialog.vue';
import type { DynamicFormField, DynamicFormSection } from '@/framework/components/DynamicFormDialog.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import { useAuthStore } from '@/stores/auth';
import type { IamResourceAction, IamResourceActionPayload, IamResourceState, IamResourceStatePayload } from '@/types/iamAdmin';

defineOptions({ name: 'IamStateActionPermissionManagementView' });

const authStore = useAuthStore();
const loading = ref(false);
const activeTab = ref<'states' | 'actions'>('states');
const tenantId = ref(0);
const resourceKey = ref('');
const states = ref<IamResourceState[]>([]);
const actions = ref<IamResourceAction[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const editingType = ref<'state' | 'action'>('state');
const form = reactive<Record<string, unknown>>({
  resourceKey: '',
  code: '',
  name: '',
  permissionCode: '',
  description: '',
  sortNo: 0,
  enabled: true,
  metadataJson: '{}',
});

const dialogTitle = computed(() => {
  const typeName = editingType.value === 'state' ? '资源状态' : '资源动作';
  return `${editingId.value ? '编辑' : '新增'}${typeName}`;
});

const dialogFields = computed<DynamicFormField[]>(() => {
  const common: DynamicFormField[] = [
    { key: 'resourceKey', label: '资源标识', placeholder: 'sales-order', required: true, disabled: Boolean(editingId.value), span: 12 },
    { key: 'code', label: editingType.value === 'state' ? '状态编码' : '动作编码', placeholder: editingType.value === 'state' ? 'DRAFT' : 'submit', required: true, disabled: Boolean(editingId.value), span: 12 },
    { key: 'name', label: editingType.value === 'state' ? '状态名称' : '动作名称', placeholder: editingType.value === 'state' ? '草稿' : '提交', required: true, span: 12 },
    { key: 'sortNo', label: '排序', component: 'number', placeholder: '10', span: 12 },
  ];
  if (editingType.value === 'action') {
    common.splice(3, 0, { key: 'permissionCode', label: '基础权限码', placeholder: 'sales-order:update', span: 12 });
  }
  return [
    ...common,
    { key: 'description', label: '说明', component: 'textarea', placeholder: '说明这个状态或动作的业务含义', span: 24 },
    { key: 'metadataJson', label: '扩展元数据 JSON', component: 'textarea', placeholder: '{}', span: 24 },
  ];
});

const dialogSections = computed<DynamicFormSection[]>(() => [{ fields: dialogFields.value }]);

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const params = {
      tenantId: Number(tenantId.value || 0),
      resourceKey: resourceKey.value.trim() || undefined,
    };
    [states.value, actions.value] = await Promise.all([
      listIamResourceStates(params),
      listIamResourceActions(params),
    ]);
  } finally {
    loading.value = false;
  }
}

function openCreateState() {
  editingType.value = 'state';
  editingId.value = null;
  resetForm();
  dialogVisible.value = true;
}

function openEditState(row: IamResourceState) {
  editingType.value = 'state';
  editingId.value = row.id;
  Object.assign(form, {
    resourceKey: row.resourceKey,
    code: row.stateCode,
    name: row.stateName,
    description: row.description || '',
    sortNo: row.sortNo,
    enabled: row.enabled,
    metadataJson: row.metadataJson || '{}',
  });
  dialogVisible.value = true;
}

function openCreateAction() {
  editingType.value = 'action';
  editingId.value = null;
  resetForm();
  dialogVisible.value = true;
}

function openEditAction(row: IamResourceAction) {
  editingType.value = 'action';
  editingId.value = row.id;
  Object.assign(form, {
    resourceKey: row.resourceKey,
    code: row.actionCode,
    name: row.actionName,
    permissionCode: row.permissionCode || '',
    description: row.description || '',
    sortNo: row.sortNo,
    enabled: row.enabled,
    metadataJson: row.metadataJson || '{}',
  });
  dialogVisible.value = true;
}

async function submitForm(value: Record<string, unknown>) {
  Object.assign(form, value);
  if (editingType.value === 'state') {
    await saveState();
  } else {
    await saveAction();
  }
  dialogVisible.value = false;
  ElMessage.success('状态动作定义已保存');
  await loadData();
}

async function saveState() {
  const payload: IamResourceStatePayload = {
    tenantId: Number(tenantId.value || 0),
    resourceKey: textValue(form.resourceKey),
    stateCode: textValue(form.code),
    stateName: textValue(form.name),
    description: nullableText(form.description),
    sortNo: numberValue(form.sortNo),
    enabled: Boolean(form.enabled ?? true),
    metadataJson: textValue(form.metadataJson) || '{}',
    operator: authStore.username,
  };
  if (editingId.value) {
    await updateIamResourceState(editingId.value, payload);
    return;
  }
  await createIamResourceState(payload);
}

async function saveAction() {
  const payload: IamResourceActionPayload = {
    tenantId: Number(tenantId.value || 0),
    resourceKey: textValue(form.resourceKey),
    actionCode: textValue(form.code),
    actionName: textValue(form.name),
    permissionCode: nullableText(form.permissionCode),
    description: nullableText(form.description),
    sortNo: numberValue(form.sortNo),
    enabled: Boolean(form.enabled ?? true),
    metadataJson: textValue(form.metadataJson) || '{}',
    operator: authStore.username,
  };
  if (editingId.value) {
    await updateIamResourceAction(editingId.value, payload);
    return;
  }
  await createIamResourceAction(payload);
}

async function toggleState(row: IamResourceState) {
  await setIamResourceStateEnabled(row.id, !row.enabled, authStore.username);
  ElMessage.success(row.enabled ? '资源状态已停用' : '资源状态已启用');
  await loadData();
}

async function toggleAction(row: IamResourceAction) {
  await setIamResourceActionEnabled(row.id, !row.enabled, authStore.username);
  ElMessage.success(row.enabled ? '资源动作已停用' : '资源动作已启用');
  await loadData();
}

function resetForm() {
  Object.assign(form, {
    resourceKey: resourceKey.value.trim(),
    code: '',
    name: '',
    permissionCode: '',
    description: '',
    sortNo: 0,
    enabled: true,
    metadataJson: '{}',
  });
}

function textValue(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function nullableText(value: unknown) {
  return textValue(value) || null;
}

function numberValue(value: unknown) {
  const next = Number(value);
  return Number.isFinite(next) ? next : 0;
}
</script>
