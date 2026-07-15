<template>
  <ListPageShell title="用户授权">
    <template #query>
      <QueryToolbar>
        <el-input v-model.number="tenantId" class="query-input" placeholder="租户 ID" />
        <el-input v-model="keyword" class="query-input" placeholder="搜索用户名 / 显示名 / 手机号" clearable />
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadUsers" />
        </template>
      </QueryToolbar>
    </template>

    <el-table v-loading="loading" :data="filteredUsers" row-key="id" border>
      <el-table-column prop="username" label="用户名" min-width="150" />
      <el-table-column prop="displayName" label="显示名" min-width="140" />
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="authVersion" label="权限版本" width="100" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.enabled && row.accountNonLocked ? 'success' : 'info'">
            {{ row.enabled && row.accountNonLocked ? '正常' : '受限' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <PermissionButton link type="primary" permission="iam:update" @click="openRoleGrant(row)">
            分配角色
          </PermissionButton>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="grantVisible" :title="grantDialogTitle" width="620px">
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
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :disabled="grantReadonly" @click="submitRoleGrant">保存角色</el-button>
      </template>
    </el-dialog>
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import {
  getIamUserRoles,
  listIamRoles,
  listIamUsers,
  setIamUserRoles,
} from '@/api/iamAdmin';
import AppState from '@/framework/components/AppState.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import { useAuthStore } from '@/stores/auth';
import { useAuthorizationStore } from '@/stores/authorization';
import type { IamRole, IamUser } from '@/types/iamAdmin';

defineOptions({ name: 'IamUserManagementView' });

const authStore = useAuthStore();
const authorizationStore = useAuthorizationStore();
const loading = ref(false);
const grantVisible = ref(false);
const grantReadonly = ref(false);
const keyword = ref('');
const roleKeyword = ref('');
const tenantId = ref(authStore.tenantId || 0);
const users = ref<IamUser[]>([]);
const roles = ref<IamRole[]>([]);
const grantingUser = ref<IamUser | null>(null);
const selectedRoleIds = ref<number[]>([]);
const isTenantReady = computed(() => Number.isFinite(Number(tenantId.value)) && tenantId.value >= 0);

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

onMounted(async () => {
  if (!isTenantReady.value) {
    return;
  }
  await Promise.all([loadUsers(), loadRoles()]);
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
    await authorizationStore.loadPermissionSnapshot();
  }
}

function ensureTenantReady() {
  if (isTenantReady.value) {
    return true;
  }
  ElMessage.warning('请输入有效租户 ID');
  return false;
}
</script>

<style scoped>
.user-role-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 260px;
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
  max-height: 360px;
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
