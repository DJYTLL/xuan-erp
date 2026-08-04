<template>
  <ListPageShell title="用户管理">
    <template #query>
      <QueryToolbar>
        <el-input v-model.number="tenantId" class="query-input" placeholder="租户 ID" />
        <el-input v-model="keyword" class="query-input" placeholder="搜索用户名 / 显示名 / 手机号" clearable />
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadUsers" />
          <PermissionButton
            type="primary"
            permission="iam-user:create"
            no-permission-mode="disable"
            :disabled-reason="tenantId <= 0 ? '平台级用户当前不支持页面新增' : ''"
            @click="openCreate"
          >
            新增用户
          </PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <XuanBrowseTable
      v-loading="loading"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      v-model:density="tableDensity"
      :schema="iamUserBrowseTableSchema"
      :tenant-id="browseTenantId"
      :user-id="browseUserId"
      :data="pagedUsers"
      :column-permission-snapshot="iamUserColumnPermissionSnapshot"
      :strict-column-permission-snapshot="strictColumnPermissionSnapshot"
      :total="filteredUsers.length"
      height="100%"
      @row-action="handleUserTableRowAction"
    />

    <DynamicFormDialog
      v-model="userDialogVisible"
      :title="editingUser ? '编辑用户' : '新增用户'"
      description="按用户基础信息维护账号资料"
      :fields="userFormFields"
      :sections="userFormSections"
      :model="userForm"
      size="lg"
      label-position="top"
      :confirm-permission="userDialogConfirmPermission"
      @submit="submitUser"
    />

    <DynamicFormDialog
      v-model="passwordDialogVisible"
      :title="passwordDialogTitle"
      description="按用户登录凭据重置密码"
      :fields="passwordFormFields"
      :model="passwordForm"
      size="sm"
      label-position="top"
      confirm-text="确认重置"
      confirm-permission="iam-user:reset-password"
      @submit="submitResetPassword"
    />

    <DynamicFormDialog
      v-model="grantVisible"
      class="user-grant-dialog"
      :title="grantDialogTitle"
      :render-form="false"
      description="按角色树维护当前用户可分配的角色"
      helper-text="拖动标题栏移动，拖动右下角调整大小"
      variant="workspace"
      workspace-size="lg"
      confirm-text="保存角色"
      :confirm-permission="'iam-user:update'"
      :confirm-disabled-reason="grantReadonly ? '平台级用户当前仅支持查看' : ''"
      @submit="submitRoleGrant"
    >
      <template #body>
        <div class="user-role-dialog">
          <p v-if="grantReadonly" class="grant-readonly-note">平台级用户当前仅支持查看角色分配结果</p>
          <el-input v-model="roleKeyword" placeholder="搜索角色编码 / 名称" clearable />
          <el-checkbox-group v-model="selectedRoleIds" class="role-checkbox-list">
            <el-checkbox
              v-for="role in filteredRoles"
              :key="role.id"
              :label="role.id"
              class="role-checkbox-item"
              :disabled="grantReadonly"
            >
              <span>{{ role.name }}</span>
              <code>{{ role.code }}</code>
            </el-checkbox>
          </el-checkbox-group>
          <AppState
            v-if="!filteredRoles.length"
            type="empty"
            title="暂无可分配角色"
            description="请先在角色授权中创建并启用角色。"
          />
        </div>
      </template>
    </DynamicFormDialog>
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import {
  createIamUser,
  getIamUserRoles,
  listIamRoles,
  listIamUsers,
  resetIamUserPassword,
  setIamUserRoles,
  updateIamUser,
} from '@/api/iamAdmin';
import { createIamUserBrowseTableSchema } from '@/config/iamUserBrowseTableSchema';
import AppState from '@/framework/components/AppState.vue';
import type { BrowseTableDensity } from '@/framework/components/browseTablePreferences';
import type { BrowseTableColumnPermissionSnapshot } from '@/framework/components/browseTableSchema';
import DynamicFormDialog from '@/framework/components/DynamicFormDialog.vue';
import type { DynamicFormField, DynamicFormSection } from '@/framework/components/DynamicFormDialog.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import XuanBrowseTable from '@/framework/components/XuanBrowseTable.vue';
import { useAuthStore } from '@/stores/auth';
import { useAuthorizationStore } from '@/stores/authorization';
import type { IamRole, IamUser } from '@/types/iamAdmin';

defineOptions({ name: 'IamUserManagementView' });

const authStore = useAuthStore();
const authorizationStore = useAuthorizationStore();
const loading = ref(false);
const userDialogVisible = ref(false);
const passwordDialogVisible = ref(false);
const grantVisible = ref(false);
const grantReadonly = ref(false);
const keyword = ref('');
const roleKeyword = ref('');
const tenantId = ref(authStore.tenantId || 0);
const currentPage = ref(1);
const pageSize = ref(20);
const tableDensity = ref<BrowseTableDensity>('default');
const users = ref<IamUser[]>([]);
const roles = ref<IamRole[]>([]);
const editingUser = ref<IamUser | null>(null);
const passwordUser = ref<IamUser | null>(null);
const grantingUser = ref<IamUser | null>(null);
const selectedRoleIds = ref<number[]>([]);
const isTenantReady = computed(() => Number.isFinite(Number(tenantId.value)) && tenantId.value >= 0);
const userForm = reactive<Record<string, unknown>>({
  username: '',
  initialPassword: '',
  displayName: '',
  email: '',
  phone: '',
  enabled: true,
  remark: '',
});
const passwordForm = reactive<Record<string, unknown>>({
  newPassword: '',
});

const filteredUsers = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return users.value;
  }
  return users.value.filter((user) => [
    user.username,
    user.displayName || '',
    user.phone || '',
    user.email || '',
  ].some((item) => item.toLowerCase().includes(value)));
});

const pagedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredUsers.value.slice(start, start + pageSize.value);
});

const browseTenantId = computed(() => String(tenantId.value || 0));
const browseUserId = computed(() => String(authStore.currentUser?.userId || authStore.username || 'anonymous'));
const iamUserRawColumnPermissions = computed(() => authorizationStore.columnPermissions['iam-user'] || {});
const strictColumnPermissionSnapshot = computed(() => authorizationStore.isLoaded && Number(authStore.tenantId || 0) > 0);

const iamUserColumnPermissionSnapshot = computed<BrowseTableColumnPermissionSnapshot>(() => {
  const userPermissions = iamUserRawColumnPermissions.value;
  return Object.fromEntries(
    Object.entries(userPermissions)
      .filter(([, accessMode]) => (
        accessMode === 'VISIBLE'
        || accessMode === 'MASKED'
        || accessMode === 'HIDDEN'
      ))
      .map(([columnKey, accessMode]) => [`iam-user::${columnKey}`, accessMode]),
  );
});

const iamUserBrowseTableSchema = computed(() => createIamUserBrowseTableSchema({
  pageCode: 'iam-user-management',
  tableCode: 'iam-user-list',
  actionsWidth: 300,
  defaultDensity: 'default',
  defaultPageSize: 20,
  toolbar: {
    showDensity: true,
    showColumnSetting: true,
    actions: [],
  },
  rowActions: [
    {
      key: 'edit',
      label: '编辑',
      type: 'primary',
      permission: 'iam-user:update',
      noPermissionMode: 'disable',
      disabledReason: (row) => (row.tenantId <= 0 ? '平台级用户当前不支持编辑' : ''),
    },
    {
      key: 'role-grant',
      label: '分配角色',
      type: 'primary',
      permission: 'iam-user:update',
      noPermissionMode: 'disable',
    },
    {
      key: 'reset-password',
      label: '重置密码',
      type: 'warning',
      permission: 'iam-user:reset-password',
      noPermissionMode: 'disable',
      disabledReason: (row) => (row.tenantId <= 0 ? '平台级用户当前不支持重置密码' : ''),
    },
  ],
}));

const enabledRoles = computed(() => roles.value.filter((role) => role.enabled !== false));

const filteredRoles = computed(() => {
  const value = roleKeyword.value.trim().toLowerCase();
  if (!value) {
    return enabledRoles.value;
  }
  return enabledRoles.value.filter((role) => [role.code, role.name]
    .some((item) => item.toLowerCase().includes(value)));
});

const grantDialogTitle = computed(() => {
  if (!grantingUser.value) {
    return '分配角色';
  }
  return `分配角色 - ${grantingUser.value.displayName || grantingUser.value.username}`;
});

const passwordDialogTitle = computed(() => {
  if (!passwordUser.value) {
    return '重置密码';
  }
  return `重置密码 - ${passwordUser.value.displayName || passwordUser.value.username}`;
});

const userDialogConfirmPermission = computed(() => (editingUser.value ? 'iam-user:update' : 'iam-user:create'));

const userFormFields = computed<DynamicFormField[]>(() => {
  const fields: DynamicFormField[] = [
    {
      key: 'username',
      label: '用户名',
      placeholder: 'buyer',
      required: !editingUser.value,
      disabled: Boolean(editingUser.value),
      span: 12,
    },
  ];
  if (!editingUser.value) {
    fields.push({
      key: 'initialPassword',
      label: '初始密码',
      component: 'password',
      placeholder: '至少 6 位',
      required: true,
      span: 12,
    });
  }
  fields.push(
    { key: 'displayName', label: '显示名', placeholder: '采购员', span: 12 },
    { key: 'phone', label: '手机号', placeholder: '13900000000', span: 12 },
    { key: 'email', label: '邮箱', placeholder: 'buyer@example.com', span: 12 },
  );
  if (editingUser.value) {
    fields.push({
      key: 'enabled',
      label: '状态',
      component: 'switch',
      activeText: '启用',
      inactiveText: '停用',
      span: 12,
    });
  }
  fields.push({ key: 'remark', label: '备注', component: 'textarea', placeholder: '用户职责说明', span: 24 });
  return fields;
});

const userFormSections = computed<DynamicFormSection[]>(() => [{
  fields: userFormFields.value,
}]);

const passwordFormFields: DynamicFormField[] = [{
  key: 'newPassword',
  label: '新密码',
  component: 'password',
  placeholder: '至少 6 位',
  required: true,
  span: 24,
}];

onMounted(async () => {
  if (!isTenantReady.value) {
    return;
  }
  await Promise.all([loadUsers(), loadRoles()]);
});

watch([keyword, tenantId, pageSize], () => {
  currentPage.value = 1;
});

async function loadUsers() {
  if (!ensureTenantReady()) {
    users.value = [];
    return;
  }
  loading.value = true;
  try {
    users.value = await listIamUsers(Number(tenantId.value || 0));
  } finally {
    loading.value = false;
  }
}

async function loadRoles() {
  if (!ensureTenantReady()) {
    roles.value = [];
    return;
  }
  roles.value = await listIamRoles(Number(tenantId.value || 0));
}

function resetUserForm() {
  Object.assign(userForm, {
    username: '',
    initialPassword: '',
    displayName: '',
    email: '',
    phone: '',
    enabled: true,
    remark: '',
  });
}

function openCreate() {
  if (!ensureBusinessTenantReady()) {
    return;
  }
  editingUser.value = null;
  resetUserForm();
  userDialogVisible.value = true;
}

function handleUserTableRowAction({ actionKey, row }: { actionKey: string; row: IamUser }) {
  switch (actionKey) {
    case 'edit':
      openEdit(row);
      return;
    case 'role-grant':
      void openRoleGrant(row);
      return;
    case 'reset-password':
      openResetPassword(row);
      break;
  }
}

function openEdit(user: IamUser) {
  if (!ensureWritableUser(user, '平台级用户当前不支持编辑')) {
    return;
  }
  editingUser.value = user;
  Object.assign(userForm, {
    username: user.username,
    initialPassword: '',
    displayName: user.displayName || '',
    email: user.email || '',
    phone: user.phone || '',
    enabled: user.enabled,
    remark: user.remark || '',
  });
  userDialogVisible.value = true;
}

async function submitUser(value: Record<string, unknown>) {
  Object.assign(userForm, value);
  if (editingUser.value) {
    await submitUserUpdate(value);
    return;
  }
  await submitUserCreate(value);
}

async function submitUserCreate(value: Record<string, unknown>) {
  if (!ensureBusinessTenantReady()) {
    return;
  }
  const username = textValue(value.username);
  const initialPassword = textValue(value.initialPassword);
  if (!username) {
    ElMessage.warning('请输入用户名');
    return;
  }
  if (initialPassword.length < 6) {
    ElMessage.warning('初始密码至少 6 位');
    return;
  }
  await createIamUser({
    tenantId: Number(tenantId.value),
    username,
    initialPassword,
    displayName: normalizeOptionalText(value.displayName),
    email: normalizeOptionalText(value.email),
    phone: normalizeOptionalText(value.phone),
    remark: normalizeOptionalText(value.remark),
    operator: authStore.username,
  });
  userDialogVisible.value = false;
  ElMessage.success('用户已创建');
  await loadUsers();
}

async function submitUserUpdate(value: Record<string, unknown>) {
  const user = editingUser.value;
  if (!user || !ensureWritableUser(user, '平台级用户当前不支持编辑')) {
    return;
  }
  await updateIamUser(user.id, {
    tenantId: user.tenantId,
    displayName: normalizeOptionalText(value.displayName),
    email: normalizeOptionalText(value.email),
    phone: normalizeOptionalText(value.phone),
    enabled: value.enabled === false ? false : true,
    remark: normalizeOptionalText(value.remark),
    operator: authStore.username,
  });
  userDialogVisible.value = false;
  ElMessage.success('用户信息已保存');
  await loadUsers();
  await refreshCurrentAuthorizationIfNeeded(user);
}

function openResetPassword(user: IamUser) {
  if (!ensureWritableUser(user, '平台级用户当前不支持重置密码')) {
    return;
  }
  passwordUser.value = user;
  passwordForm.newPassword = '';
  passwordDialogVisible.value = true;
}

async function submitResetPassword(value: Record<string, unknown>) {
  Object.assign(passwordForm, value);
  const user = passwordUser.value;
  if (!user || !ensureWritableUser(user, '平台级用户当前不支持重置密码')) {
    return;
  }
  const newPassword = textValue(value.newPassword);
  if (newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位');
    return;
  }
  await resetIamUserPassword(user.id, {
    tenantId: user.tenantId,
    newPassword,
    operator: authStore.username,
  });
  passwordDialogVisible.value = false;
  ElMessage.success('用户密码已重置');
  await loadUsers();
  await refreshCurrentAuthorizationIfNeeded(user);
}

async function openRoleGrant(user: IamUser) {
  grantingUser.value = user;
  grantReadonly.value = user.tenantId <= 0;
  roleKeyword.value = '';
  if (!roles.value.length) {
    await loadRoles();
  }
  const grant = await getIamUserRoles(user.tenantId, user.id);
  selectedRoleIds.value = grant.roleIds;
  grantVisible.value = true;
}

async function submitRoleGrant() {
  if (!grantingUser.value) {
    return;
  }
  if (grantReadonly.value) {
    ElMessage.warning('平台级用户当前仅支持查看角色分配结果');
    return;
  }
  const user = grantingUser.value;
  await setIamUserRoles(user.tenantId, user.id, selectedRoleIds.value, authStore.username);
  grantVisible.value = false;
  ElMessage.success('用户角色已保存');
  await loadUsers();
  if (user.id === authStore.currentUser?.userId && user.tenantId === authStore.tenantId) {
    await authorizationStore.refreshCurrentAuthorizationContext();
  }
}

function ensureTenantReady() {
  if (isTenantReady.value) {
    return true;
  }
  ElMessage.warning('请输入有效租户 ID');
  return false;
}

function ensureBusinessTenantReady() {
  if (Number.isFinite(Number(tenantId.value)) && Number(tenantId.value) > 0) {
    return true;
  }
  ElMessage.warning('请输入有效业务租户 ID');
  return false;
}

function ensureWritableUser(user: IamUser, message: string) {
  if (user.tenantId > 0) {
    return true;
  }
  ElMessage.warning(message);
  return false;
}

function normalizeOptionalText(value: unknown) {
  const trimmed = textValue(value);
  return trimmed || undefined;
}

function textValue(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

async function refreshCurrentAuthorizationIfNeeded(user: IamUser) {
  if (user.id !== authStore.currentUser?.userId || user.tenantId !== authStore.tenantId) {
    return;
  }
  await authStore.refreshSession();
  await authorizationStore.refreshCurrentAuthorizationContext();
}
</script>

<style scoped>
.user-role-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.grant-readonly-note {
  margin: 0;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 10px 12px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 13px;
}

.role-checkbox-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 8px 12px;
  flex: 1;
  min-height: 0;
  max-height: none;
  overflow: auto;
}

.role-checkbox-item {
  align-items: center;
  height: auto;
  min-height: 34px;
  margin-right: 0;
}

.role-checkbox-item span {
  margin-right: 8px;
  font-weight: 600;
}

code {
  border-radius: 4px;
  padding: 2px 5px;
  color: #b45309;
  background: #fff7ed;
  font-size: 12px;
}
</style>
