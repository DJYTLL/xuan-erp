<template>
  <ListPageShell title="初始化模板">
    <template #query>
      <QueryToolbar>
        <el-input v-model="keyword" class="query-input" placeholder="搜索模板编码 / 名称" clearable />
        <el-select v-model="enabledFilter" class="status-filter" placeholder="状态" clearable>
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadTemplates" />
          <PermissionButton type="primary" permission="iam:create" @click="openCreate">新增模板</PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <el-table v-loading="loading" :data="filteredTemplates" row-key="id" border height="100%">
      <el-table-column prop="code" label="模板编码" min-width="160" />
      <el-table-column prop="name" label="模板名称" min-width="150" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.defaultTemplate" type="primary" effect="plain">默认</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="权限数" width="100">
        <template #default="{ row }">
          <span>{{ row.permissionCodes.length }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openGrant(row)">权限</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingTemplate ? '编辑模板' : '新增模板'" width="640px">
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板编码">
              <el-input v-model.trim="form.code" :disabled="Boolean(editingTemplate)" placeholder="standard" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称">
              <el-input v-model.trim="form.name" placeholder="标准模板" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用">
              <el-switch v-model="form.enabled" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认模板">
              <el-switch v-model="form.defaultTemplate" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="说明">
              <el-input v-model.trim="form.description" type="textarea" :rows="3" placeholder="模板用途说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTemplate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="grantVisible"
      class="init-template-grant-dialog"
      :title="grantDialogTitle"
      width="1080px"
      top="6vh"
      destroy-on-close
    >
      <MenuPermissionAssignment
        v-model="selectedPermissionCodes"
        :menus="menus"
        :permissions="permissions"
        :page-required-permission-map="businessPageRequiredPermissionMap"
      />
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <PermissionButton type="primary" permission="iam:update" @click="submitGrant">保存权限</PermissionButton>
      </template>
    </el-dialog>
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import {
  createIamTenantInitTemplate,
  listIamMenus,
  listIamPermissions,
  listIamTenantInitTemplates,
  setIamTenantInitTemplatePermissions,
  updateIamTenantInitTemplate,
} from '@/api/iamAdmin';
import { businessPageRequiredPermissionMap } from '@/config/businessPageRequiredPermissions';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import MenuPermissionAssignment from '@/framework/components/MenuPermissionAssignment.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import { useAuthStore } from '@/stores/auth';
import type {
  IamMenu,
  IamPermission,
  IamTenantInitTemplate,
  IamTenantInitTemplatePayload,
} from '@/types/iamAdmin';

defineOptions({ name: 'IamTenantInitTemplateManagementView' });

const authStore = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const grantVisible = ref(false);
const keyword = ref('');
const enabledFilter = ref<boolean | ''>('');
const templates = ref<IamTenantInitTemplate[]>([]);
const menus = ref<IamMenu[]>([]);
const permissions = ref<IamPermission[]>([]);
const editingTemplate = ref<IamTenantInitTemplate | null>(null);
const grantingTemplate = ref<IamTenantInitTemplate | null>(null);
const selectedPermissionCodes = ref<string[]>([]);
const form = reactive<IamTenantInitTemplatePayload>({
  code: '',
  name: '',
  description: '',
  enabled: true,
  defaultTemplate: false,
});

const filteredTemplates = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  return templates.value.filter((template) => {
    if (enabledFilter.value !== '' && template.enabled !== enabledFilter.value) {
      return false;
    }
    if (!value) {
      return true;
    }
    return [template.code, template.name, template.description || '']
      .some((item) => item.toLowerCase().includes(value));
  });
});

const grantDialogTitle = computed(() => (
  grantingTemplate.value ? `模板权限 - ${grantingTemplate.value.name}` : '模板权限'
));

onMounted(async () => {
  await Promise.all([loadTemplates(), loadMenus(), loadPermissions()]);
});

async function loadTemplates() {
  loading.value = true;
  try {
    templates.value = await listIamTenantInitTemplates();
  } finally {
    loading.value = false;
  }
}

async function loadMenus() {
  menus.value = await listIamMenus();
}

async function loadPermissions() {
  permissions.value = await listIamPermissions();
}

function resetForm() {
  Object.assign(form, {
    code: '',
    name: '',
    description: '',
    enabled: true,
    defaultTemplate: false,
  });
}

function openCreate() {
  editingTemplate.value = null;
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row: IamTenantInitTemplate) {
  editingTemplate.value = row;
  Object.assign(form, {
    code: row.code,
    name: row.name,
    description: row.description || '',
    enabled: row.enabled,
    defaultTemplate: row.defaultTemplate,
  });
  dialogVisible.value = true;
}

async function submitTemplate() {
  if (!form.code?.trim() || !form.name?.trim()) {
    ElMessage.warning('请填写模板编码和模板名称');
    return;
  }
  submitting.value = true;
  try {
    if (editingTemplate.value) {
      await updateIamTenantInitTemplate(editingTemplate.value.id, form);
    } else {
      await createIamTenantInitTemplate({ ...form, permissionCodes: [] });
    }
    dialogVisible.value = false;
    ElMessage.success('初始化模板已保存');
    await loadTemplates();
  } finally {
    submitting.value = false;
  }
}

function openGrant(row: IamTenantInitTemplate) {
  grantingTemplate.value = row;
  selectedPermissionCodes.value = [...row.permissionCodes];
  grantVisible.value = true;
}

async function submitGrant() {
  if (!grantingTemplate.value) {
    return;
  }
  const saved = await setIamTenantInitTemplatePermissions(
    grantingTemplate.value.id,
    selectedPermissionCodes.value,
    authStore.username,
  );
  templates.value = templates.value.map((template) => (template.id === saved.id ? saved : template));
  grantVisible.value = false;
  ElMessage.success('模板权限已保存');
}
</script>

<style scoped>
.status-filter {
  width: 132px;
}

:global(.init-template-grant-dialog) {
  max-width: calc(100vw - 48px);
}

:global(.init-template-grant-dialog .el-dialog__body) {
  padding-top: 8px;
}
</style>
