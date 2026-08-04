<template>
  <ListPageShell
    title="菜单管理"
    description="左侧按照导航栏分组查看菜单，右侧维护当前分组下的实际菜单项。"
  >
    <template #query>
      <QueryToolbar>
        <el-input v-model="keyword" class="query-input" placeholder="搜索编码 / 标题 / 路径 / 菜单可见权限" clearable />
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadMenus" />
          <PermissionButton type="primary" permission="iam-menu:create" @click="openCreate('page')">新增页面</PermissionButton>
          <PermissionButton permission="iam-menu:create" @click="openCreate('group')">新增分组</PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <div class="menu-management-layout">
      <NavigationMenuTree
        v-model="selectedNodeKey"
        title="导航菜单树"
        all-node-title="全部菜单"
        :menus="menus"
        :keyword="keyword"
        context-menu-enabled
        :context-menu-actions-resolver="resolveMenuContextActions"
        :show-edit-button="true"
        @node-select="handleMenuNodeSelect"
        @node-edit="handleMenuNodeEdit"
        @context-menu-command="handleMenuContextCommand"
      />

      <section class="menu-table-panel">
        <header class="menu-table-head">
          <div class="menu-table-heading">
            <strong>{{ selectedNodeTitle }}</strong>
            <span>{{ selectedNodeDescription }}</span>
          </div>
          <el-tag type="primary" effect="plain">{{ filteredMenuRows.length }} 项菜单</el-tag>
        </header>

        <el-table v-loading="loading" :data="filteredMenuRows" row-key="id" border height="100%">
          <el-table-column label="菜单" min-width="240">
            <template #default="{ row }">
              <div class="menu-primary-cell">
                <strong>{{ resolveMenuDisplayTitle(row) }}</strong>
                <span>{{ row.code }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="层级" width="96">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ resolveMenuLevelLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="104">
            <template #default="{ row }">
              <el-tag size="small" :type="resolveMenuTagType(row)" effect="plain">{{ resolveMenuTypeLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="导航配置" min-width="280">
            <template #default="{ row }">
              <div class="menu-meta-cell">
                <span>父菜单：{{ resolveParentLabel(row) }}</span>
                <span>路由：{{ row.path || '未配置' }}</span>
                <span>菜单可见权限：{{ row.permissionCode || '未配置' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="i18nKey" label="国际化键" min-width="160">
            <template #default="{ row }">
              <span>{{ row.i18nKey || '未配置' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sortNo" label="排序" width="90" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <PermissionButton link type="primary" permission="iam-menu:update" no-permission-mode="disable" @click="openEdit(row)">编辑</PermissionButton>
              <PermissionButton link :type="row.enabled ? 'warning' : 'success'" permission="iam-menu:update" no-permission-mode="disable" @click="toggleMenu(row)">
                {{ row.enabled ? '停用' : '启用' }}
              </PermissionButton>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <DynamicFormDialog
      v-model="dialogVisible"
      :title="editingMenu ? '编辑菜单' : '新增菜单'"
      :description="menuTypeDescription"
      :fields="menuFormFields"
      :sections="menuFormSections"
      :model="form"
      size="lg"
      label-position="top"
      :confirm-permission="editingMenu ? 'iam-menu:update' : 'iam-menu:create'"
      @submit="submitMenu"
    />
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus/es/components/message/index';
import { RefreshCw } from 'lucide-vue-next';
import { createIamMenu, listIamMenus, setIamMenuEnabled, updateIamMenu } from '@/api/iamAdmin';
import DynamicFormDialog from '@/framework/components/DynamicFormDialog.vue';
import type { DynamicFormField, DynamicFormSection } from '@/framework/components/DynamicFormDialog.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import NavigationMenuTree, {
  type NavigationMenuTreeContextMenuAction,
  type NavigationMenuTreeNode,
} from '@/framework/components/NavigationMenuTree.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import type { IamMenu, IamMenuPayload } from '@/types/iamAdmin';

defineOptions({ name: 'IamMenuManagementView' });

type ParentOption = {
  value: number;
  label: string;
  disabled: boolean;
  children: ParentOption[];
};

type MenuFormType = 'group' | 'page' | 'entry';

const ALL_MENU_NODE = '__all__';
const { t } = useI18n();

const loading = ref(false);
const dialogVisible = ref(false);
const keyword = ref('');
const menus = ref<IamMenu[]>([]);
const editingMenu = ref<IamMenu | null>(null);
const selectedNodeKey = ref(ALL_MENU_NODE);
const selectedMenuNode = ref<NavigationMenuTreeNode | null>(null);
type MenuFormModel = IamMenuPayload & {
  menuType: MenuFormType;
};

const form = reactive<MenuFormModel>({
  code: '',
  parentId: null,
  title: '',
  i18nKey: '',
  path: '',
  icon: '',
  permissionCode: '',
  sortNo: 0,
  menuType: 'page',
});

const menuById = computed(() => new Map(menus.value.map((menu) => [menu.id, menu])));
const menuByCode = computed(() => new Map(menus.value.map((menu) => [menu.code, menu])));

const childrenByParentId = computed(() => {
  const result = new Map<number | null, IamMenu[]>();
  for (const menu of menus.value) {
    const parentId = menu.parentId ?? null;
    const siblings = result.get(parentId) || [];
    siblings.push(menu);
    result.set(parentId, siblings);
  }
  for (const siblings of result.values()) {
    siblings.sort((left, right) => left.sortNo - right.sortNo);
  }
  return result;
});

const orderedMenus = computed(() => {
  const result: IamMenu[] = [];
  const visited = new Set<number>();

  function appendChildren(parentId: number | null) {
    for (const item of childrenByParentId.value.get(parentId) || []) {
      if (visited.has(item.id)) {
        continue;
      }
      visited.add(item.id);
      result.push(item);
      appendChildren(item.id);
    }
  }

  appendChildren(null);
  for (const item of [...menus.value].sort((left, right) => left.sortNo - right.sortNo)) {
    if (!visited.has(item.id)) {
      visited.add(item.id);
      result.push(item);
      appendChildren(item.id);
    }
  }
  return result;
});

const menuFormFields = computed<DynamicFormField[]>(() => [
  {
    key: 'menuType',
    label: '菜单类型',
    component: 'radio-group',
    required: true,
    radioStyle: 'button',
    options: [
      { label: '导航分组', value: 'group' },
      { label: '页面菜单', value: 'page' },
      { label: '分组入口', value: 'entry' },
    ],
    span: 24,
  },
  {
    key: 'code',
    label: '菜单编码',
    placeholder: 'iam-role-management',
    required: true,
    disabled: Boolean(editingMenu.value),
    span: 12,
  },
  {
    key: 'parentId',
    label: '父菜单',
    component: 'tree-select',
    treeOptions: parentMenuTreeOptions.value,
    placeholder: '顶级菜单',
    clearable: true,
    checkStrictly: true,
    filterable: true,
    renderAfterExpand: false,
    filterNodeMethod: filterParentMenuNode,
    span: 12,
  },
  { key: 'title', label: '标题', placeholder: '角色管理', required: true, span: 12 },
  { key: 'i18nKey', label: '国际化键', placeholder: 'menu.iamRoles', span: 12 },
  {
    key: 'path',
    label: '路由路径',
    placeholder: form.menuType === 'group' ? '导航分组不需要路由路径' : '/system/iam/roles',
    disabled: form.menuType === 'group',
    span: 12,
  },
  { key: 'icon', label: '图标键', placeholder: 'ShieldCheck', span: 12 },
  {
    key: 'permissionCode',
    label: '菜单可见权限',
    placeholder: form.menuType === 'group' ? '导航分组通常不配置权限码' : '页面进入所需权限码，例如 iam-menu:view',
    disabled: form.menuType === 'group',
    span: 12,
  },
  {
    key: 'sortNo',
    label: '排序',
    component: 'number',
    placeholder: '0',
    inputMode: 'numeric',
    span: 12,
  },
]);

const menuFormSections = computed<DynamicFormSection[]>(() => [
  {
    title: '菜单配置',
    fields: menuFormFields.value,
  },
]);

const searchedMenus = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return orderedMenus.value;
  }
  return orderedMenus.value.filter((menu) => [
    menu.code,
    menu.title,
    menu.path || '',
    menu.permissionCode || '',
    menu.i18nKey || '',
    resolveParentLabel(menu),
  ].some((item) => item.toLowerCase().includes(value)));
});

const selectedNodeTitle = computed(() => selectedMenuNode.value?.title || '全部菜单');

const selectedNodeDescription = computed(() => {
  if (!selectedMenuNode.value || selectedMenuNode.value.key === ALL_MENU_NODE) {
    return '当前展示所有导航分组中的实际菜单项。';
  }
  return selectedMenuNode.value.menuCodes.length > 1
    ? '当前展示该导航分组下所有实际菜单项。'
    : '当前展示该菜单节点本身，便于核对页面入口配置。';
});

const filteredMenuRows = computed(() => {
  if (!selectedMenuNode.value || selectedMenuNode.value.key === ALL_MENU_NODE) {
    return searchedMenus.value;
  }
  const selectedCodes = new Set(selectedMenuNode.value.menuCodes);
  return searchedMenus.value.filter((menu) => selectedCodes.has(menu.code));
});

const parentMenuTreeOptions = computed<ParentOption[]>(() => buildParentMenuTreeOptions(null));

const menuTypeDescription = computed(() => {
  if (form.menuType === 'group') {
    return '用于左侧导航折叠分组，可作为其他菜单的父级，不配置路由路径和菜单可见权限。';
  }
  if (form.menuType === 'entry') {
    return '既能作为父级展开子菜单，也能点击进入页面；只在确实需要分组首页时使用。';
  }
  return '用于打开具体页面，必须配置路由路径，可按需配置页面进入所需权限码。';
});

onMounted(loadMenus);

watch(() => form.menuType, normalizeFormByMenuType);

async function loadMenus() {
  loading.value = true;
  try {
    menus.value = await listIamMenus();
  } finally {
    loading.value = false;
  }
}

function handleMenuNodeSelect(node: NavigationMenuTreeNode) {
  selectedMenuNode.value = node;
}

function handleMenuNodeEdit(node: NavigationMenuTreeNode) {
  const sourceMenu = node.sourceCode ? menuByCode.value.get(node.sourceCode) : null;
  if (!sourceMenu) {
    ElMessage.warning('该导航分组没有绑定可编辑菜单');
    return;
  }
  openEdit(sourceMenu);
}

function resolveMenuContextActions(node: NavigationMenuTreeNode): NavigationMenuTreeContextMenuAction[] {
  const sourceMenu = node.sourceCode ? menuByCode.value.get(node.sourceCode) : null;
  if (!sourceMenu) {
    return [];
  }
  return [
    {
      key: 'edit',
      label: '编辑菜单',
      onClick: ({ node }) => handleMenuNodeEdit(node),
    },
  ];
}

function handleMenuContextCommand(payload: { actionKey: string; node: NavigationMenuTreeNode }) {
  if (payload.actionKey === 'edit') {
    handleMenuNodeEdit(payload.node);
  }
}

function buildParentMenuTreeOptions(parentId: number | null): ParentOption[] {
  return (childrenByParentId.value.get(parentId) || [])
    .filter((menu) => menu.id !== editingMenu.value?.id)
    .map((menu) => ({
      value: menu.id,
      label: `${resolveMenuDisplayTitle(menu)}（${menu.code}）`,
      disabled: !isParentMenuSelectable(menu),
      children: buildParentMenuTreeOptions(menu.id),
    }));
}

function isParentMenuSelectable(menu: IamMenu) {
  return (!hasPagePath(menu) || hasChildrenMenu(menu)) && !isEditingMenuDescendant(menu.id);
}

function hasPagePath(menu: IamMenu) {
  return Boolean(menu.path?.trim());
}

function hasChildrenMenu(menu: IamMenu) {
  return Boolean(childrenByParentId.value.get(menu.id)?.length);
}

function isEditingMenuDescendant(menuId: number) {
  if (!editingMenu.value) {
    return false;
  }
  let current = menuById.value.get(menuId);
  const visited = new Set<number>();
  while (current?.parentId) {
    if (visited.has(current.parentId)) {
      return true;
    }
    visited.add(current.parentId);
    if (current.parentId === editingMenu.value.id) {
      return true;
    }
    current = menuById.value.get(current.parentId);
  }
  return false;
}

function filterParentMenuNode(value: string, data: ParentOption) {
  if (!value) {
    return true;
  }
  return data.label.toLowerCase().includes(value.toLowerCase());
}

function resetForm(menuType: MenuFormType = 'page') {
  Object.assign(form, {
    code: '',
    parentId: null,
    title: '',
    i18nKey: '',
    path: '',
    icon: '',
    permissionCode: '',
    sortNo: 0,
    menuType,
  });
}

function openCreate(type: MenuFormType = 'page') {
  editingMenu.value = null;
  resetForm(type);
  dialogVisible.value = true;
}

function openEdit(row: IamMenu) {
  editingMenu.value = row;
  form.menuType = inferMenuType(row);
  Object.assign(form, {
    code: row.code,
    parentId: row.parentId,
    title: row.title,
    i18nKey: row.i18nKey || '',
    path: row.path || '',
    icon: row.icon || '',
    permissionCode: row.permissionCode || '',
    sortNo: row.sortNo,
  });
  dialogVisible.value = true;
}

async function submitMenu(value: Record<string, unknown>) {
  Object.assign(form, value);
  normalizeFormByMenuType();
  form.sortNo = normalizeSortNo(form.sortNo);
  if ((form.menuType === 'page' || form.menuType === 'entry') && !form.path?.trim()) {
    ElMessage.warning('页面菜单必须填写路由路径');
    return;
  }
  const parentMenu = form.parentId ? menuById.value.get(form.parentId) : null;
  if (parentMenu && !isParentMenuSelectable(parentMenu)) {
    ElMessage.warning('父菜单只能选择可折叠的分组节点');
    return;
  }
  if (editingMenu.value) {
    await updateIamMenu(editingMenu.value.id, form);
  } else {
    await createIamMenu(form);
  }
  dialogVisible.value = false;
  ElMessage.success('菜单已保存');
  await loadMenus();
}

function normalizeFormByMenuType() {
  if (form.menuType === 'group') {
    form.path = '';
    form.permissionCode = '';
  }
}

function inferMenuType(menu: IamMenu): MenuFormType {
  const hasPath = hasPagePath(menu);
  const hasChildren = hasChildrenMenu(menu);
  if (hasPath && hasChildren) {
    return 'entry';
  }
  return hasPath ? 'page' : 'group';
}

async function toggleMenu(row: IamMenu) {
  await setIamMenuEnabled(row.id, !row.enabled);
  ElMessage.success(row.enabled ? '菜单已停用' : '菜单已启用');
  await loadMenus();
}

function resolveParentLabel(menu: IamMenu) {
  if (!menu.parentId) {
    return '顶级菜单';
  }
  const parent = menuById.value.get(menu.parentId);
  return parent ? `${resolveMenuDisplayTitle(parent)}（${parent.code}）` : '父菜单已缺失';
}

function resolveMenuDisplayTitle(menu: IamMenu) {
  if (!menu.i18nKey) {
    return menu.title;
  }
  const translated = t(menu.i18nKey);
  return translated === menu.i18nKey ? menu.title : translated;
}

function resolveMenuDepth(menu: IamMenu) {
  let depth = 1;
  let currentParentId = menu.parentId;
  const visited = new Set<number>();
  while (currentParentId) {
    if (visited.has(currentParentId)) {
      break;
    }
    visited.add(currentParentId);
    depth += 1;
    currentParentId = menuById.value.get(currentParentId)?.parentId ?? null;
  }
  return depth;
}

function resolveMenuLevelLabel(menu: IamMenu) {
  const depth = resolveMenuDepth(menu);
  return depth === 1 ? '一级' : `${depth}级`;
}

function resolveMenuTypeLabel(menu: IamMenu) {
  const hasChildren = (childrenByParentId.value.get(menu.id) || []).length > 0;
  if (menu.path && hasChildren) {
    return '分组入口';
  }
  if (menu.path) {
    return '页面菜单';
  }
  if (hasChildren) {
    return '导航分组';
  }
  return '叶子目录';
}

function resolveMenuTagType(menu: IamMenu) {
  const type = resolveMenuTypeLabel(menu);
  if (type === '页面菜单') {
    return 'primary';
  }
  if (type === '分组入口') {
    return 'warning';
  }
  if (type === '导航分组') {
    return 'success';
  }
  return 'info';
}

function normalizeSortNo(value: string | number | null | undefined) {
  const parsed = Number.parseInt(String(value ?? '0'), 10);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return 0;
  }
  return parsed;
}
</script>

<style scoped>
.menu-management-layout {
  width: 100%;
  min-height: 0;
  height: 100%;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  align-items: stretch;
  gap: 16px;
  overflow: hidden;
}

.menu-management-layout :deep(.navigation-menu-tree) {
  width: 300px;
  min-height: 0;
}

.menu-table-panel {
  min-width: 0;
  min-height: 0;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  background: var(--xuan-panel);
  overflow: hidden;
}

.menu-table-head {
  min-height: 48px;
  border-bottom: 1px solid var(--xuan-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
}

.menu-table-panel :deep(.el-table) {
  flex: 1 1 0;
  min-height: 0;
}

.menu-table-heading {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-table-heading strong {
  color: var(--xuan-text);
}

.menu-table-heading span {
  color: var(--xuan-muted);
  font-size: 12px;
}

.menu-primary-cell,
.menu-meta-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-primary-cell strong {
  color: var(--xuan-text);
}

.menu-primary-cell span,
.menu-meta-cell span {
  color: var(--xuan-muted);
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}

.menu-type-description {
  margin: 8px 0 0;
  color: var(--xuan-muted);
  font-size: 12px;
  line-height: 1.5;
}

.parent-menu-tree-select {
  width: 100%;
}

:global(.parent-menu-tree-popper .el-tree-node__content) {
  min-height: 34px;
}

:global(.parent-menu-tree-popper .el-tree-node.is-disabled > .el-tree-node__content) {
  color: #9aa5b1;
  cursor: not-allowed;
}

:global(.parent-menu-tree-popper .el-tree-node:not(.is-disabled) > .el-tree-node__content:hover) {
  color: var(--xuan-primary);
  background: var(--xuan-active);
}

@media (max-width: 980px) {
  .menu-management-layout {
    height: auto;
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .menu-management-layout :deep(.navigation-menu-tree) {
    width: 100%;
    max-height: 360px;
  }
}
</style>
