<template>
  <ListPageShell title="权限管理">
    <template #query>
      <QueryToolbar>
        <el-input v-model="keyword" class="query-input" placeholder="搜索权限码 / 名称 / 服务 / 菜单" clearable />
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadPermissionPage" />
          <PermissionButton type="primary" permission="iam-permission:create" @click="openCreate">新增权限</PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <div class="permission-menu-layout">
      <NavigationMenuTree
        v-model="selectedMenuCode"
        title="菜单权限树"
        all-node-title="全部权限"
        :menus="menus"
        :keyword="keyword"
        :total-count="filteredPermissions.length"
        :extra-nodes="extraPermissionMenuNodes"
        :count-resolver="countPermissionsByMenuCodes"
        @node-select="handleMenuNodeSelect"
      />

      <section class="permission-table-panel">
        <header class="permission-table-head">
          <div>
            <strong>{{ selectedMenuTitle }}</strong>
            <span>{{ selectedMenuDescription }}</span>
          </div>
          <el-tag type="primary" effect="plain">{{ filteredPermissionRows.length }} 项权限</el-tag>
        </header>

        <el-table v-loading="loading" :data="filteredPermissionRows" row-key="id" border height="100%">
          <el-table-column prop="code" label="权限码" min-width="190" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="serviceName" label="来源服务" min-width="150" />
          <el-table-column label="关系" width="110">
            <template #default="{ row }">
              <el-tag :type="isRelatedPermissionRow(row) ? 'warning' : 'info'" effect="plain">
                {{ isRelatedPermissionRow(row) ? '页面依赖' : '直接权限' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="所属菜单" min-width="180">
            <template #default="{ row }">
              <span>{{ resolvePermissionMenuLabel(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="220" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <PermissionButton link type="primary" permission="iam-permission:update" no-permission-mode="disable" @click="openEdit(row)">编辑</PermissionButton>
              <PermissionButton link :type="row.enabled ? 'warning' : 'success'" permission="iam-permission:update" no-permission-mode="disable" @click="togglePermission(row)">
                {{ row.enabled ? '停用' : '启用' }}
              </PermissionButton>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <DynamicFormDialog
      v-model="dialogVisible"
      :title="editingPermission ? '编辑权限' : '新增权限'"
      :fields="permissionFormFields"
      :sections="permissionFormSections"
      :model="form"
      size="md"
      label-position="top"
      :confirm-permission="editingPermission ? 'iam-permission:update' : 'iam-permission:create'"
      @submit="submitPermission"
    />
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import {
  createIamPermission,
  listIamMenuOptions,
  listIamPermissions,
  setIamPermissionEnabled,
  updateIamPermission,
} from '@/api/iamAdmin';
import DynamicFormDialog from '@/framework/components/DynamicFormDialog.vue';
import type { DynamicFormField, DynamicFormSection } from '@/framework/components/DynamicFormDialog.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import NavigationMenuTree, { type NavigationMenuTreeNode } from '@/framework/components/NavigationMenuTree.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import {
  businessPageRequiredPermissionMap,
  collectBusinessPageRequiredPermissionCodes,
} from '@/config/businessPageRequiredPermissions';
import type { IamMenu, IamPermission, IamPermissionPayload } from '@/types/iamAdmin';

defineOptions({ name: 'IamPermissionManagementView' });

type MenuSelectOption = {
  code: string;
  label: string;
};

const ALL_MENU_CODE = '__all__';
const UNASSIGNED_MENU_CODE = '__unassigned__';
const UNASSIGNED_MENU_NODE: NavigationMenuTreeNode = {
  key: UNASSIGNED_MENU_CODE,
  title: '未绑定菜单',
  level: 1,
  menuCodes: [],
  count: 0,
  children: [],
};

const loading = ref(false);
const dialogVisible = ref(false);
const keyword = ref('');
const selectedMenuCode = ref(ALL_MENU_CODE);
const selectedMenuNode = ref<NavigationMenuTreeNode | null>(null);
const menus = ref<IamMenu[]>([]);
const permissions = ref<IamPermission[]>([]);
const editingPermission = ref<IamPermission | null>(null);
const form = reactive<IamPermissionPayload>({
  code: '',
  name: '',
  serviceName: 'xuan-iam',
  menuCode: '',
  description: '',
});

const menuByCode = computed(() => new Map(menus.value.map((menu) => [menu.code, menu])));
const permissionByCode = computed(() => new Map(permissions.value.map((permission) => [permission.code, permission])));

const childrenByParentId = computed(() => {
  const result = new Map<number | null, IamMenu[]>();
  for (const menu of menus.value) {
    const parentId = menu.parentId ?? null;
    const children = result.get(parentId) || [];
    children.push(menu);
    result.set(parentId, children);
  }
  for (const children of result.values()) {
    children.sort((left, right) => left.sortNo - right.sortNo);
  }
  return result;
});

const filteredPermissions = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return permissions.value;
  }
  return permissions.value.filter((permission) => {
    const menuTitle = permission.menuCode ? menuByCode.value.get(permission.menuCode)?.title || '' : '未绑定菜单';
    return [permission.code, permission.name, permission.serviceName, permission.menuCode || '', menuTitle]
      .some((item) => item.toLowerCase().includes(value));
  });
});

const unassignedPermissionCount = computed(() => filteredPermissions.value.filter((permission) => !permission.menuCode).length);

const extraPermissionMenuNodes = computed<NavigationMenuTreeNode[]>(() => [
  {
    ...UNASSIGNED_MENU_NODE,
    count: unassignedPermissionCount.value,
  },
]);

const selectedMenuTitle = computed(() => selectedMenuNode.value?.title || '全部权限');

const selectedMenuDescription = computed(() => {
  if (!selectedMenuNode.value || selectedMenuCode.value === ALL_MENU_CODE) {
    return '当前展示所有菜单下的权限。';
  }
  if (selectedMenuCode.value === UNASSIGNED_MENU_CODE) {
    return '这些权限还没有绑定菜单编码，需要补齐归属。';
  }
  const relatedCount = selectedMenuPermissionScope.value.relatedPermissionCodes.size;
  if (relatedCount > 0) {
    return selectedMenuNode.value.menuCodes.length > 1
      ? `当前展示该导航分组直接权限，以及页面正常运作依赖的 ${relatedCount} 项关联权限。`
      : `当前展示该菜单直接权限，以及页面正常运作依赖的 ${relatedCount} 项关联权限。`;
  }
  return selectedMenuNode.value.menuCodes.length > 1
    ? '当前展示该导航分组下所有后端菜单的权限。'
    : '当前展示该菜单下的权限。';
});

const selectedMenuPermissionScope = computed(() => {
  if (!selectedMenuNode.value || selectedMenuCode.value === ALL_MENU_CODE || selectedMenuCode.value === UNASSIGNED_MENU_CODE) {
    return {
      directPermissionCodes: new Set<string>(),
      relatedPermissionCodes: new Set<string>(),
      effectivePermissionCodes: new Set<string>(),
    };
  }
  return resolveMenuPermissionScope(selectedMenuNode.value.menuCodes);
});

const filteredPermissionRows = computed(() => {
  if (!selectedMenuNode.value || selectedMenuCode.value === ALL_MENU_CODE) {
    return filteredPermissions.value;
  }
  if (selectedMenuCode.value === UNASSIGNED_MENU_CODE) {
    return filteredPermissions.value.filter((permission) => !permission.menuCode);
  }
  return filteredPermissions.value.filter((permission) => selectedMenuPermissionScope.value.effectivePermissionCodes.has(permission.code));
});

const menuSelectOptions = computed<MenuSelectOption[]>(() => flattenMenuOptions(null));

const permissionFormFields = computed<DynamicFormField[]>(() => [
  {
    key: 'code',
    label: '权限码',
    placeholder: 'iam:role:update',
    required: true,
    disabled: Boolean(editingPermission.value),
    span: 12,
  },
  {
    key: 'name',
    label: '名称',
    placeholder: '角色修改',
    required: true,
    span: 12,
  },
  {
    key: 'serviceName',
    label: '来源服务',
    placeholder: 'xuan-iam',
    required: true,
    span: 12,
  },
  {
    key: 'menuCode',
    label: '所属菜单',
    component: 'select',
    placeholder: '选择菜单',
    clearable: true,
    options: menuSelectOptions.value.map((item) => ({ label: item.label, value: item.code })),
    span: 12,
  },
  {
    key: 'description',
    label: '说明',
    component: 'textarea',
    placeholder: '权限用途说明',
    span: 24,
  },
]);

const permissionFormSections = computed<DynamicFormSection[]>(() => [
  {
    title: '权限信息',
    fields: permissionFormFields.value,
  },
]);

onMounted(loadPermissionPage);

async function loadPermissionPage() {
  loading.value = true;
  try {
    const [nextMenus, nextPermissions] = await Promise.all([listIamMenuOptions(), listIamPermissions()]);
    menus.value = nextMenus;
    permissions.value = nextPermissions;
  } finally {
    loading.value = false;
  }
}

async function loadPermissions() {
  permissions.value = await listIamPermissions();
}

function countPermissionsByMenuCodes(menuCodes: string[]) {
  const scope = resolveMenuPermissionScope(menuCodes);
  return filteredPermissions.value.filter((permission) => scope.effectivePermissionCodes.has(permission.code)).length;
}

function flattenMenuOptions(parentId: number | null, level = 0): MenuSelectOption[] {
  return (childrenByParentId.value.get(parentId) || []).flatMap((menu) => [
    {
      code: menu.code,
      label: `${'　'.repeat(level)}${menu.title}（${menu.code}）`,
    },
    ...flattenMenuOptions(menu.id, level + 1),
  ]);
}

function handleMenuNodeSelect(node: NavigationMenuTreeNode) {
  selectedMenuNode.value = node;
}

function resolveMenuPermissionScope(menuCodes: string[]) {
  const selectedCodes = new Set(menuCodes);
  const directPermissionCodes = new Set(
    permissions.value
      .filter((permission) => permission.menuCode && selectedCodes.has(permission.menuCode))
      .map((permission) => permission.code),
  );
  const relatedPermissionCodes = new Set(
    collectBusinessPageRequiredPermissionCodes(menuCodes, businessPageRequiredPermissionMap)
      .filter((code) => !directPermissionCodes.has(code) && permissionByCode.value.has(code)),
  );
  return {
    directPermissionCodes,
    relatedPermissionCodes,
    effectivePermissionCodes: new Set([...directPermissionCodes, ...relatedPermissionCodes]),
  };
}

function isRelatedPermissionRow(permission: IamPermission) {
  return selectedMenuPermissionScope.value.relatedPermissionCodes.has(permission.code);
}

function resolvePermissionMenuLabel(permission: IamPermission) {
  if (!permission.menuCode) {
    return '未绑定菜单';
  }
  const menu = menuByCode.value.get(permission.menuCode);
  return menu ? `${menu.title}（${permission.menuCode}）` : permission.menuCode;
}

function resetForm() {
  Object.assign(form, {
    code: '',
    name: '',
    serviceName: 'xuan-iam',
    menuCode: '',
    description: '',
  });
}

function openCreate() {
  editingPermission.value = null;
  resetForm();
  if (selectedMenuNode.value?.menuCodes.length === 1) {
    form.menuCode = selectedMenuNode.value.menuCodes[0];
  }
  dialogVisible.value = true;
}

function openEdit(row: IamPermission) {
  editingPermission.value = row;
  Object.assign(form, {
    code: row.code,
    name: row.name,
    serviceName: row.serviceName,
    menuCode: row.menuCode || '',
    description: row.description || '',
    enabled: row.enabled,
  });
  dialogVisible.value = true;
}

async function submitPermission(value: Record<string, unknown>) {
  Object.assign(form, value);
  if (editingPermission.value) {
    await updateIamPermission(editingPermission.value.id, form);
  } else {
    await createIamPermission(form);
  }
  dialogVisible.value = false;
  ElMessage.success('权限已保存');
  await loadPermissions();
}

async function togglePermission(row: IamPermission) {
  await setIamPermissionEnabled(row.id, !row.enabled);
  ElMessage.success(row.enabled ? '权限已停用' : '权限已启用');
  await loadPermissions();
}
</script>

<style scoped>
.permission-menu-layout {
  width: 100%;
  min-height: 0;
  height: 100%;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  align-items: stretch;
  gap: 16px;
  overflow: hidden;
}

.permission-menu-layout :deep(.navigation-menu-tree) {
  width: 300px;
  min-height: 0;
}

.permission-table-panel {
  min-width: 0;
  min-height: 0;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  background: var(--xuan-panel);
  overflow: hidden;
}

.permission-table-head {
  min-height: 48px;
  border-bottom: 1px solid var(--xuan-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
}

.permission-table-head div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.permission-table-head strong {
  color: var(--xuan-text);
}

.permission-table-head span {
  color: var(--xuan-muted);
  font-size: 12px;
}

.permission-table-panel :deep(.el-table) {
  flex: 1 1 0;
  min-height: 0;
}

@media (max-width: 900px) {
  .permission-menu-layout {
    height: auto;
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .permission-menu-layout :deep(.navigation-menu-tree) {
    width: 100%;
    max-height: 360px;
  }
}
</style>
