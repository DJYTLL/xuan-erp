<template>
  <ListPageShell title="角色授权">
    <template #query>
      <QueryToolbar>
        <el-input v-model.number="tenantId" class="query-input" placeholder="租户 ID" />
        <el-input v-model="keyword" class="query-input" placeholder="搜索角色编码 / 名称" clearable />
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadRoles" />
          <PermissionButton
            type="primary"
            permission="iam-role:create"
            no-permission-mode="disable"
            :disabled-reason="tenantId <= 0 ? '平台级角色当前不支持页面新增' : ''"
            @click="openCreate"
          >
            新增角色
          </PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <el-table v-loading="loading" :data="filteredRoles" row-key="id" border>
      <el-table-column prop="code" label="角色编码" min-width="180" />
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="description" label="说明" min-width="240" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <PermissionButton
            link
            type="primary"
            permission="iam-role:update"
            no-permission-mode="disable"
            :title="grantActionHint(row)"
            @click="openGrant(row)"
          >
            授权
          </PermissionButton>
          <PermissionButton
            link
            type="primary"
            permission="iam-role:update"
            no-permission-mode="disable"
            :disabled-reason="row.tenantId <= 0 ? '平台级角色当前不支持编辑' : ''"
            @click="openEdit(row)"
          >
            编辑
          </PermissionButton>
          <PermissionButton
            link
            :type="row.enabled ? 'warning' : 'success'"
            permission="iam-role:update"
            no-permission-mode="disable"
            :disabled-reason="row.tenantId <= 0 ? '平台级角色当前不支持启停' : ''"
            @click="toggleRole(row)"
          >
            {{ row.enabled ? '停用' : '启用' }}
          </PermissionButton>
        </template>
      </el-table-column>
    </el-table>

    <DynamicFormDialog
      v-model="dialogVisible"
      :title="editingRole ? '编辑角色' : '新增角色'"
      description="按角色基础信息维护权限对象"
      :fields="roleFormFields"
      :sections="roleFormSections"
      :model="form"
      size="lg"
      label-position="top"
      :confirm-permission="roleDialogConfirmPermission"
      @submit="submitRole"
    />

    <DynamicFormDialog
      v-model="grantVisible"
      class="role-grant-dialog role-grant-dialog--assignment"
      :title="grantDialogTitle"
      :render-form="false"
      description="按菜单树维护当前角色可分配的页面和按钮权限"
      helper-text="拖动标题栏移动，拖动右下角调整大小"
      variant="workspace"
      size="lg"
      workspace-size="lg"
      confirm-text="保存授权"
      :confirm-permission="'iam-role:update'"
      :confirm-disabled-reason="grantReadonly ? '平台级角色当前仅支持查看' : ''"
      @submit="submitGrant"
    >
      <template #body>
        <p v-if="grantReadonly" class="grant-readonly-note">平台级角色当前仅支持查看权限分配结果</p>
        <el-tabs v-model="activeGrantTab" class="grant-tabs">
          <el-tab-pane label="基础权限" name="base">
            <MenuPermissionAssignment
              v-model="selectedPermissionCodes"
              :menus="menus"
              :permissions="grantAvailablePermissions"
              :page-required-permission-map="roleGrantRequiredPermissionMap"
              :readonly="grantReadonly"
            />
          </el-tab-pane>
          <el-tab-pane label="状态动作权限" name="stateAction">
            <div v-if="canViewStateActionRules" class="state-action-panel">
              <QueryToolbar>
                <el-select v-model="selectedStateActionResource" class="query-input" placeholder="资源" filterable>
                  <el-option
                    v-for="resource in stateActionResources"
                    :key="resource"
                    :label="resource"
                    :value="resource"
                  />
                </el-select>
              </QueryToolbar>
              <StateActionPermissionMatrix
                v-model="selectedStateActionRules"
                :states="visibleStateActionStates"
                :actions="visibleStateActionActions"
                :readonly="grantReadonly || !canUpdateStateActionRules"
              />
            </div>
            <el-empty v-else description="缺少状态动作权限查看权限" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </DynamicFormDialog>
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import {
  createIamRole,
  getIamRolePermissions,
  getIamRoleStateActionRules,
  listIamResourceActions,
  listIamResourceStates,
  listIamRoles,
  setIamRoleEnabled,
  setIamRolePermissions,
  setIamRoleStateActionRules,
  updateIamRole,
} from '@/api/iamAdmin';
import DynamicFormDialog from '@/framework/components/DynamicFormDialog.vue';
import type { DynamicFormField, DynamicFormSection } from '@/framework/components/DynamicFormDialog.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import MenuPermissionAssignment from '@/framework/components/MenuPermissionAssignment.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import StateActionPermissionMatrix from '@/framework/components/StateActionPermissionMatrix.vue';
import type { StateActionMatrixRule } from '@/framework/components/StateActionPermissionMatrix.vue';
import { businessPageRequiredPermissionMap } from '@/config/businessPageRequiredPermissions';
import { useAuthStore } from '@/stores/auth';
import { useAuthorizationStore } from '@/stores/authorization';
import type { CurrentMenuNode } from '@/types/auth';
import type {
  IamMenu,
  IamPermission,
  IamResourceAction,
  IamResourceState,
  IamRole,
  IamRolePayload,
} from '@/types/iamAdmin';

defineOptions({ name: 'IamRoleManagementView' });

const authStore = useAuthStore();
const authorizationStore = useAuthorizationStore();
const loading = ref(false);
const dialogVisible = ref(false);
const grantVisible = ref(false);
const keyword = ref('');
const tenantId = ref(authStore.tenantId || 0);
const roles = ref<IamRole[]>([]);
const menus = ref<IamMenu[]>([]);
const permissions = ref<IamPermission[]>([]);
const editingRole = ref<IamRole | null>(null);
const grantingRole = ref<IamRole | null>(null);
const grantReadonly = ref(false);
const selectedPermissionCodes = ref<string[]>([]);
const grantAvailablePermissionCodes = ref<string[]>([]);
const activeGrantTab = ref('base');
const resourceStates = ref<IamResourceState[]>([]);
const resourceActions = ref<IamResourceAction[]>([]);
const selectedStateActionRules = ref<StateActionMatrixRule[]>([]);
const selectedStateActionResource = ref('');
const form = reactive<Record<string, unknown>>({
  code: '',
  name: '',
  description: '',
});
const roleGrantRequiredPermissionMap = businessPageRequiredPermissionMap;

const filteredRoles = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return roles.value;
  }
  return roles.value.filter((role) => [role.code, role.name].some((item) => item.toLowerCase().includes(value)));
});

const grantAvailablePermissions = computed(() => {
  if (grantReadonly.value) {
    return permissions.value;
  }
  const availableCodeSet = new Set(grantAvailablePermissionCodes.value);
  return permissions.value.filter((permission) => availableCodeSet.has(permission.code));
});

const grantDialogTitle = computed(() => {
  const prefix = grantReadonly.value ? '角色授权（只读）' : '角色授权';
  return grantingRole.value ? `${prefix} - ${grantingRole.value.name}` : prefix;
});

const roleDialogConfirmPermission = computed(() => (editingRole.value ? 'iam-role:update' : 'iam-role:create'));
const canViewStateActionRules = computed(() => authorizationStore.hasButtonPermission('iam-state-action:view'));
const canUpdateStateActionRules = computed(() => authorizationStore.hasButtonPermission('iam-state-action:update'));
const stateActionResources = computed(() => {
  const resources = [
    ...resourceStates.value.map((state) => state.resourceKey),
    ...resourceActions.value.map((action) => action.resourceKey),
  ];
  return [...new Set(resources)].sort((left, right) => left.localeCompare(right));
});
const visibleStateActionStates = computed(() => resourceStates.value.filter((state) => state.resourceKey === selectedStateActionResource.value));
const visibleStateActionActions = computed(() => resourceActions.value.filter((action) => action.resourceKey === selectedStateActionResource.value));

const roleFormFields = computed<DynamicFormField[]>(() => [
  {
    key: 'code',
    label: '角色编码',
    placeholder: 'tenant_operator',
    required: !editingRole.value,
    disabled: Boolean(editingRole.value),
    span: 12,
  },
  {
    key: 'name',
    label: '角色名称',
    placeholder: '租户操作员',
    required: true,
    span: 12,
  },
  {
    key: 'description',
    label: '说明',
    component: 'textarea',
    placeholder: '角色用途说明',
    span: 24,
  },
]);

const roleFormSections = computed<DynamicFormSection[]>(() => [{
  fields: roleFormFields.value,
}]);

onMounted(async () => {
  await Promise.all([loadRoles(), loadMenus()]);
});

async function loadRoles() {
  loading.value = true;
  try {
    roles.value = await listIamRoles(Number(tenantId.value || 0));
  } finally {
    loading.value = false;
  }
}

async function loadMenus() {
  if (!authorizationStore.isLoaded) {
    await authorizationStore.refreshCurrentAuthorizationContext();
  }
  menus.value = flattenCurrentMenus(authorizationStore.menus);
}

function resetForm() {
  Object.assign(form, {
    code: '',
    name: '',
    description: '',
  });
}

function openCreate() {
  if (!ensureBusinessTenantReady()) {
    return;
  }
  editingRole.value = null;
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row: IamRole) {
  if (!ensureWritableRole(row, '平台级角色当前不支持编辑')) {
    return;
  }
  editingRole.value = row;
  Object.assign(form, {
    code: row.code,
    name: row.name,
    description: row.description || '',
  });
  dialogVisible.value = true;
}

async function submitRole(value: Record<string, unknown>) {
  Object.assign(form, value);
  if (editingRole.value) {
    await updateIamRole(editingRole.value.id, rolePayload(editingRole.value.tenantId, value));
  } else {
    if (!ensureBusinessTenantReady()) {
      return;
    }
    await createIamRole(rolePayload(Number(tenantId.value), value));
  }
  dialogVisible.value = false;
  ElMessage.success('角色已保存');
  await loadRoles();
}

async function toggleRole(row: IamRole) {
  if (!ensureWritableRole(row, '平台级角色当前不支持启停')) {
    return;
  }
  await setIamRoleEnabled(row.id, !row.enabled);
  ElMessage.success(row.enabled ? '角色已停用' : '角色已启用');
  await loadRoles();
}

function rolePayload(targetTenantId: number, value: Record<string, unknown>): IamRolePayload {
  return {
    tenantId: targetTenantId,
    code: textValue(value.code),
    name: textValue(value.name),
    description: normalizeOptionalText(value.description),
  };
}

function isReadonlyGrantRole(row: IamRole) {
  return row.tenantId <= 0;
}

function grantActionHint(row: IamRole) {
  return isReadonlyGrantRole(row) ? '平台级角色当前仅支持查看权限分配结果' : '';
}

async function openGrant(row: IamRole) {
  grantingRole.value = row;
  grantReadonly.value = row.tenantId <= 0;
  activeGrantTab.value = 'base';
  const grant = await getIamRolePermissions(row.tenantId, row.id);
  grantAvailablePermissionCodes.value = grant.availablePermissionCodes || [];
  permissions.value = grant.availablePermissions || [];
  selectedPermissionCodes.value = grant.permissionCodes;
  selectedPermissionCodes.value = selectedPermissionCodes.value.filter((code) => grantAvailablePermissionCodes.value.includes(code));
  await loadStateActionGrant(row);
  grantVisible.value = true;
}

async function submitGrant() {
  if (!grantingRole.value) {
    return;
  }
  if (grantReadonly.value) {
    ElMessage.warning('平台级角色当前仅支持查看权限分配结果');
    return;
  }
  await setIamRolePermissions(
    grantingRole.value.tenantId,
    grantingRole.value.id,
    selectedPermissionCodes.value,
    authStore.username,
  );
  if (canUpdateStateActionRules.value) {
    await setIamRoleStateActionRules(
      grantingRole.value.id,
      {
        tenantId: grantingRole.value.tenantId,
        rules: selectedStateActionRules.value,
        operator: authStore.username,
      },
    );
  }
  await authorizationStore.refreshCurrentAuthorizationContext();
  grantVisible.value = false;
  ElMessage.success('角色授权已保存');
}

async function loadStateActionGrant(row: IamRole) {
  resourceStates.value = [];
  resourceActions.value = [];
  selectedStateActionRules.value = [];
  selectedStateActionResource.value = '';
  if (!canViewStateActionRules.value) {
    return;
  }
  const [states, actions, rules] = await Promise.all([
    listIamResourceStates({ tenantId: row.tenantId, enabled: true }),
    listIamResourceActions({ tenantId: row.tenantId, enabled: true }),
    getIamRoleStateActionRules(row.tenantId, row.id),
  ]);
  resourceStates.value = states;
  resourceActions.value = actions;
  selectedStateActionRules.value = rules.map((rule) => ({
    resourceKey: rule.resourceKey,
    stateCode: rule.stateCode,
    actionCode: rule.actionCode,
  }));
  selectedStateActionResource.value = stateActionResources.value[0] || '';
}

function ensureBusinessTenantReady() {
  if (Number.isFinite(Number(tenantId.value)) && Number(tenantId.value) > 0) {
    return true;
  }
  ElMessage.warning('请输入有效业务租户 ID');
  return false;
}

function ensureWritableRole(role: IamRole, message: string) {
  if (role.tenantId > 0) {
    return true;
  }
  ElMessage.warning(message);
  return false;
}

function normalizeOptionalText(value: unknown) {
  const trimmed = textValue(value);
  return trimmed || null;
}

function textValue(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function flattenCurrentMenus(sourceMenus: CurrentMenuNode[]) {
  const nextId = { value: 1 };
  return flattenCurrentMenuNodes(sourceMenus, null, nextId);
}

function flattenCurrentMenuNodes(sourceMenus: CurrentMenuNode[], parentId: number | null, nextId: { value: number }): IamMenu[] {
  return sourceMenus.flatMap((menu) => {
    const id = nextId.value++;
    return [
      {
        id,
        code: menu.code,
        parentId,
        title: menu.title,
        i18nKey: menu.i18nKey,
        path: menu.path,
        icon: menu.icon,
        permissionCode: menu.permissionCode,
        sortNo: menu.sortNo,
        enabled: true,
      },
      ...flattenCurrentMenuNodes(menu.children || [], id, nextId),
    ];
  });
}
</script>

<style scoped>
.grant-readonly-note {
  margin: 0 0 12px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 10px 12px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 13px;
}

.grant-tabs {
  min-height: 520px;
}

.state-action-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

</style>
