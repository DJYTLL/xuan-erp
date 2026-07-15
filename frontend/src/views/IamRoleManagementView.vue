<template>
  <ListPageShell title="角色授权">
    <template #query>
      <QueryToolbar>
        <el-input v-model.number="tenantId" class="query-input" placeholder="租户 ID" />
        <el-input v-model="keyword" class="query-input" placeholder="搜索角色编码 / 名称" clearable />
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadRoles" />
          <PermissionButton type="primary" permission="iam:create" @click="openCreate">新增角色</PermissionButton>
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
          <el-button
            link
            type="primary"
            :title="grantActionHint(row)"
            @click="openGrant(row)"
          >
            授权
          </el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.enabled ? 'warning' : 'success'" @click="toggleRole(row)">
            {{ row.enabled ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingRole ? '编辑角色' : '新增角色'" width="640px">
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="租户 ID">
              <el-input-number v-model="form.tenantId" :disabled="Boolean(editingRole)" :min="0" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色编码">
              <el-input v-model="form.code" :disabled="Boolean(editingRole)" placeholder="tenant_operator" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色名称">
              <el-input v-model="form.name" placeholder="租户操作员" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="说明">
              <el-input v-model="form.description" type="textarea" placeholder="角色用途说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="grantVisible"
      class="role-grant-dialog"
      :title="grantDialogTitle"
      width="1080px"
      top="6vh"
      destroy-on-close
    >
      <p v-if="grantReadonly" class="grant-readonly-note">平台级角色当前仅支持查看权限分配结果</p>
      <MenuPermissionAssignment
        v-model="selectedPermissionCodes"
        :menus="menus"
        :permissions="permissions"
        :page-required-permission-map="roleGrantRequiredPermissionMap"
        :readonly="grantReadonly"
      />
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :disabled="grantReadonly" @click="submitGrant">保存授权</el-button>
      </template>
    </el-dialog>
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import {
  createIamRole,
  getIamRolePermissions,
  listIamMenus,
  listIamPermissions,
  listIamRoles,
  setIamRoleEnabled,
  setIamRolePermissions,
  updateIamRole,
} from '@/api/iamAdmin';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import MenuPermissionAssignment from '@/framework/components/MenuPermissionAssignment.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import { businessPageRequiredPermissionMap } from '@/config/businessPageRequiredPermissions';
import { useAuthStore } from '@/stores/auth';
import { useAuthorizationStore } from '@/stores/authorization';
import type { IamMenu, IamPermission, IamRole, IamRolePayload } from '@/types/iamAdmin';

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
const form = reactive<IamRolePayload>({
  tenantId: tenantId.value,
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

const allEnabledPermissionCodes = computed(() => permissions.value
  .filter((permission) => permission.enabled !== false)
  .map((permission) => permission.code)
  .sort((left, right) => left.localeCompare(right)));

const grantDialogTitle = computed(() => {
  const prefix = grantReadonly.value ? '角色授权（只读）' : '角色授权';
  return grantingRole.value ? `${prefix} - ${grantingRole.value.name}` : prefix;
});

onMounted(async () => {
  await Promise.all([loadRoles(), loadMenus(), loadPermissions()]);
});

async function loadRoles() {
  loading.value = true;
  try {
    roles.value = await listIamRoles(Number(tenantId.value || 0));
  } finally {
    loading.value = false;
  }
}

async function loadPermissions() {
  permissions.value = await listIamPermissions();
}

async function loadMenus() {
  menus.value = await listIamMenus();
}

function resetForm() {
  Object.assign(form, {
    tenantId: Number(tenantId.value || 0),
    code: '',
    name: '',
    description: '',
  });
}

function openCreate() {
  editingRole.value = null;
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row: IamRole) {
  editingRole.value = row;
  Object.assign(form, {
    tenantId: row.tenantId,
    code: row.code,
    name: row.name,
    description: row.description || '',
    enabled: row.enabled,
  });
  dialogVisible.value = true;
}

async function submitRole() {
  if (editingRole.value) {
    await updateIamRole(editingRole.value.id, form);
  } else {
    await createIamRole(form);
  }
  dialogVisible.value = false;
  ElMessage.success('角色已保存');
  await loadRoles();
}

async function toggleRole(row: IamRole) {
  await setIamRoleEnabled(row.id, !row.enabled);
  ElMessage.success(row.enabled ? '角色已停用' : '角色已启用');
  await loadRoles();
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
  if (grantReadonly.value) {
    selectedPermissionCodes.value = allEnabledPermissionCodes.value;
    grantVisible.value = true;
    return;
  }
  const grant = await getIamRolePermissions(row.tenantId, row.id);
  selectedPermissionCodes.value = grant.permissionCodes;
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
  await authorizationStore.loadPermissionSnapshot();
  grantVisible.value = false;
  ElMessage.success('角色授权已保存');
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

:global(.role-grant-dialog) {
  max-width: calc(100vw - 48px);
}

:global(.role-grant-dialog .el-dialog__body) {
  padding-top: 8px;
}
</style>
