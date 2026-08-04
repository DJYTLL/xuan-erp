<template>
  <ListPageShell title="角色列权限">
    <template #query>
      <QueryToolbar>
        <el-select
          v-if="showTenantSelector"
          v-model="selectedTenantId"
          class="tenant-filter"
          placeholder="选择租户"
          filterable
          :loading="loadingTenants"
          @change="handleTenantChange"
        >
          <el-option
            v-for="tenant in tenantOptions"
            :key="tenant.id"
            :label="`${tenant.name}（${tenant.code}）`"
            :value="tenant.id"
          />
        </el-select>
        <el-tag v-else class="current-tenant-tag" type="info" effect="plain">
          {{ activeTenantLabel }}
        </el-tag>
        <el-select v-model="selectedRoleId" class="role-filter" placeholder="选择角色" filterable clearable>
          <el-option
            v-for="role in filteredRoles"
            :key="role.id"
            :label="formatRoleLabel(role)"
            :value="role.id"
          />
        </el-select>
        <el-input v-model="roleKeyword" class="query-input" placeholder="搜索角色编码 / 名称" clearable />
        <el-input v-model="menuKeyword" class="query-input" placeholder="搜索菜单 / 页面" clearable />
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadPage" />
          <PermissionButton
            type="primary"
            permission="iam-role-column-permission:update"
            no-permission-mode="disable"
            :disabled="!canSaveRoleRules"
            :loading="submitting"
            @click="saveRoleColumnRules"
          >
            保存规则
          </PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <section class="role-column-dependency-strip" :class="{ warning: missingDependencyHints.length || roleColumnPermissionErrorMessage }">
      <div>
        <strong>接口依赖</strong>
        <span>角色查询、列权限资源、角色列权限规则都需要具备查看权限。</span>
      </div>
      <div class="dependency-items">
        <span
          v-for="dependency in roleColumnPermissionDependencies"
          :key="dependency.permission"
          class="dependency-chip"
          :class="{ missing: !authStore.hasPermission(dependency.permission) }"
        >
          {{ dependency.permission }} · {{ authStore.hasPermission(dependency.permission) ? '已具备' : dependency.message }}
        </span>
      </div>
      <span v-if="roleColumnPermissionErrorMessage" class="dependency-error">{{ roleColumnPermissionErrorMessage }}</span>
    </section>

    <div class="role-column-workspace" :style="workspaceStyle">
      <aside class="role-column-tree-shell" :class="{ collapsed: isMenuTreeCollapsed }">
        <button
          type="button"
          class="role-column-tree-collapse"
          :aria-label="isMenuTreeCollapsed ? '展开菜单树' : '收起菜单树'"
          @click="toggleMenuTreeCollapse"
        >
          <PanelLeftOpen v-if="isMenuTreeCollapsed" :size="16" />
          <PanelLeftClose v-else :size="16" />
        </button>

        <div v-if="isMenuTreeCollapsed" class="role-column-tree-collapsed">
          <span>菜单树</span>
        </div>

        <NavigationMenuTree
          v-else
          v-model="selectedMenuCode"
          title="导航菜单树"
          all-node-title="全部页面"
          :menus="menus"
          :keyword="menuKeyword"
          :count-resolver="countColumnsByMenuCodes"
          @node-select="handleMenuNodeSelect"
        />
      </aside>

      <section ref="workbenchRef" class="role-column-main">
        <section class="role-column-editor-panel" :style="editorPanelStyle">
          <header class="role-column-editor-head">
            <div class="role-column-editor-title">
              <strong>{{ activePageTitle }}</strong>
              <span>字段配置器</span>
              <div class="role-column-meta-row">
                <span v-if="activeRole" class="mini-badge">{{ formatRoleLabel(activeRole) }}</span>
                <span v-if="activePagePath" class="mini-badge">路径 {{ activePagePath }}</span>
                <span class="mini-badge">租户模板池 {{ tenantTemplateAssignments.length }} 个</span>
                <span class="mini-badge">可授权字段 {{ selectedAssignableColumns.length }} / {{ selectedPageColumns.length }}</span>
              </div>
            </div>
            <div class="role-column-editor-actions">
              <button
                type="button"
                class="column-panel-toggle"
                :aria-label="isEditorPanelCollapsed ? '展开字段配置器' : '收起字段配置器'"
                @click="toggleEditorPanelCollapse"
              >
                <ChevronDown v-if="isEditorPanelCollapsed" :size="16" />
                <ChevronUp v-else :size="16" />
              </button>
              <button type="button" class="editor-action primary" :disabled="!selectedAssignableColumns.length || readonly" @click="applyMaxAccessToCurrentPage">
                按租户上限显示
              </button>
              <button type="button" class="editor-action" :disabled="!selectedAssignableColumns.length || readonly" @click="hideCurrentPageColumns">
                全部隐藏
              </button>
              <button type="button" class="editor-action" :disabled="!selectedAssignableColumns.length || readonly" @click="resetPageRules">
                恢复已保存
              </button>
            </div>
          </header>

          <div v-if="isEditorPanelCollapsed" class="panel-collapsed-tip">
            字段配置器已折叠，点击右侧箭头展开。
          </div>

          <div v-else-if="!activeRole" class="column-empty-state">
            <strong>请选择角色</strong>
            <span>先选择租户和角色，再维护该角色自己的列权限规则。</span>
          </div>

          <div v-else-if="!selectedPageConfig" class="column-empty-state">
            <strong>{{ activePageTitle }}</strong>
            <span>当前菜单还没有接入列权限字段，后续页面接入 schema 后这里会自动出现字段。</span>
          </div>

          <div v-else-if="!selectedAssignableColumns.length" class="column-empty-state">
            <strong>{{ activePageTitle }}</strong>
            <span>租户还没有获得当前页面字段模板，请先在租户管理中给租户分配列权限模板。</span>
          </div>

          <div v-else class="field-grid">
            <article
              v-for="column in selectedAssignableColumns"
              :key="column.id"
              class="field-card"
              :class="{ off: resolveColumnAccess(column.id) === 'HIDDEN' }"
            >
              <div class="field-info">
                <strong>{{ column.columnName }}</strong>
                <span>{{ column.columnKey }} · {{ resolveColumnLocation(column) }} · 上限 {{ resolveAccessLabel(resolveMaxAccess(column.id)) }}</span>
              </div>
              <el-segmented
                :model-value="resolveColumnAccess(column.id)"
                :options="resolveAccessOptions(column.id)"
                size="small"
                :disabled="readonly"
                @update:model-value="toggleColumnVisibility(column, $event)"
              />
            </article>
          </div>
        </section>

        <div
          class="workbench-divider"
          :class="{ dragging: workbenchResizeState.active, disabled: isEditorPanelCollapsed || isPreviewPanelCollapsed }"
          @mousedown.prevent="startWorkbenchResize"
        >
          <span class="workbench-divider-line" />
        </div>

        <section class="role-column-preview-panel" :style="previewPanelStyle">
          <header class="role-column-preview-head">
            <div>
              <strong>{{ activePageTitle }}预览</strong>
              <span>按当前角色列权限规则实时预览页面字段展示。</span>
            </div>
            <div class="preview-head-actions">
              <span class="preview-source-badge">真实数据</span>
              <span class="preview-source-badge">共享 schema</span>
              <button
                type="button"
                class="column-panel-toggle"
                :aria-label="isPreviewPanelCollapsed ? '展开页面预览' : '收起页面预览'"
                @click="togglePreviewPanelCollapse"
              >
                <ChevronUp v-if="isPreviewPanelCollapsed" :size="16" />
                <ChevronDown v-else :size="16" />
              </button>
            </div>
          </header>

          <div v-if="isPreviewPanelCollapsed" class="panel-collapsed-tip">
            预览区已折叠，点击右上角箭头展开。
          </div>

          <div v-else-if="previewTableSchema && selectedAssignableColumns.length" class="preview-shell">
            <section class="preview-page">
              <XuanBrowseTable
                v-loading="previewLoading"
                v-model:current-page="previewCurrentPage"
                v-model:page-size="previewPageSize"
                v-model:density="previewDensity"
                :schema="previewTableSchema"
                :tenant-id="String(targetTenantId || 0)"
                :user-id="authStore.currentUser?.username || 'anonymous'"
                :data="previewRows"
                :column-permission-snapshot="previewColumnPermissionSnapshot"
                :total="previewTotal"
                height="100%"
              />
            </section>
          </div>

          <div v-else class="column-empty-state preview-empty-state">
            <strong>暂无可预览页面</strong>
            <span>{{ previewHintText }}</span>
          </div>
        </section>
      </section>
    </div>
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { ChevronDown, ChevronUp, PanelLeftClose, PanelLeftOpen, RefreshCw } from 'lucide-vue-next';
import { resolveRoleColumnPermissionForbiddenMessage } from '@/api/http-error';
import {
  getIamColumnPermissionTemplateItems,
  getIamRoleColumnPermissions,
  listIamUsers,
  listIamResourceColumns,
  listIamRoles,
  listIamTenantColumnPermissionTemplates,
  setIamRoleColumnPermissions,
} from '@/api/iamAdmin';
import { listColumnPermissionTenants } from '@/api/tenants';
import { columnPermissionPageConfigs, findColumnPermissionPageConfig } from '@/config/columnPermissionPages';
import { createIamUserBrowseTableSchema } from '@/config/iamUserBrowseTableSchema';
import { createTenantBrowseTableSchema } from '@/config/tenantBrowseTableSchema';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import NavigationMenuTree, { type NavigationMenuTreeNode } from '@/framework/components/NavigationMenuTree.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import XuanBrowseTable from '@/framework/components/XuanBrowseTable.vue';
import type { BrowseTableDensity } from '@/framework/components/browseTablePreferences';
import {
  createBrowseTablePermissionKey,
  type BrowseTableColumnPermissionSnapshot,
  type XuanBrowseTableSchema,
} from '@/framework/components/browseTableSchema';
import { useAuthStore } from '@/stores/auth';
import { useAuthorizationStore } from '@/stores/authorization';
import type { CurrentMenuNode } from '@/types/auth';
import type {
  IamColumnPermissionTemplateItem,
  IamMenu,
  IamResourceColumn,
  IamRole,
  IamUser,
} from '@/types/iamAdmin';
import type { Tenant } from '@/types/tenant';

defineOptions({ name: 'IamRoleColumnPermissionManagementView' });

type AccessMode = 'VISIBLE' | 'MASKED' | 'HIDDEN';
type SchemaPermissionColumn = {
  resourceKey: string;
  columnKey: string;
  title: string;
  location: 'list' | 'detail';
  order: number;
};
type PreviewRow = Tenant | IamUser;

const ALL_MENU_CODE = '__all__';

const authStore = useAuthStore();
const authorizationStore = useAuthorizationStore();
const loading = ref(false);
const loadingTenants = ref(false);
const submitting = ref(false);
const selectedTenantId = ref<number | null>(authStore.tenantId > 0 ? authStore.tenantId : null);
const tenants = ref<Tenant[]>([]);
const selectedRoleId = ref<number | null>(null);
const roleKeyword = ref('');
const menuKeyword = ref('');
const selectedMenuCode = ref(ALL_MENU_CODE);
const selectedMenuNode = ref<NavigationMenuTreeNode | null>(null);
const isMenuTreeCollapsed = ref(false);
const workbenchRef = ref<HTMLElement | null>(null);
const editorPanelHeight = ref(240);
const isEditorPanelCollapsed = ref(false);
const isPreviewPanelCollapsed = ref(false);
const lastExpandedEditorHeight = ref(240);
const roles = ref<IamRole[]>([]);
const menus = ref<IamMenu[]>([]);
const resourceColumns = ref<IamResourceColumn[]>([]);
const tenantTemplateAssignments = ref<Array<{ templateId: number; templateName: string }>>([]);
const tenantAssignableRules = ref<IamColumnPermissionTemplateItem[]>([]);
const previewCurrentPage = ref(1);
const previewPageSize = ref(10);
const previewDensity = ref<BrowseTableDensity>('default');
const previewLoading = ref(false);
const previewRows = ref<PreviewRow[]>([]);
const previewTotal = ref(0);
const roleColumnPermissionErrorMessage = ref('');
const workbenchResizeState = reactive({
  active: false,
  startY: 0,
  startHeight: 240,
});
const initialRuleForm = ref<Record<number, AccessMode>>({});
const ruleForm = reactive<Record<number, AccessMode>>({});
const roleColumnPermissionDependencies = [
  { permission: 'iam-role:view', message: '缺少角色查询权限' },
  { permission: 'iam-column-permission:view', message: '缺少列权限资源查看权限' },
  { permission: 'iam-role-column-permission:view', message: '缺少角色列权限查看权限' },
];

const workspaceStyle = computed(() => ({
  '--role-column-tree-width': isMenuTreeCollapsed.value ? '52px' : '300px',
}));

const editorPanelStyle = computed(() => {
  if (isEditorPanelCollapsed.value) {
    return {
      height: '88px',
      flex: '0 0 auto',
    };
  }
  if (isPreviewPanelCollapsed.value) {
    return {
      minHeight: '220px',
      flex: '1 1 0',
    };
  }
  return {
    height: `${editorPanelHeight.value}px`,
    flex: '0 0 auto',
  };
});

const previewPanelStyle = computed(() => {
  if (isPreviewPanelCollapsed.value) {
    return {
      height: '88px',
      flex: '0 0 auto',
    };
  }
  if (isEditorPanelCollapsed.value) {
    return {
      minHeight: '240px',
      flex: '1 1 0',
    };
  }
  return {
    minHeight: '240px',
    flex: '1 1 0',
  };
});

const showTenantSelector = computed(() => Number(authStore.tenantId || 0) <= 0);
const targetTenantId = computed(() => Number(selectedTenantId.value || authStore.tenantId || 0));
const tenantOptions = computed(() => tenants.value);
const activeTenant = computed(() => tenants.value.find((tenant) => tenant.id === targetTenantId.value) || null);
const activeTenantLabel = computed(() => {
  if (activeTenant.value) {
    return `${activeTenant.value.name}（${activeTenant.value.code}）`;
  }
  const currentTenantId = Number(authStore.tenantId || 0);
  return currentTenantId > 0 ? `当前租户 #${currentTenantId}` : '请选择租户';
});

const filteredRoles = computed(() => {
  const value = roleKeyword.value.trim().toLowerCase();
  if (!value) {
    return roles.value;
  }
  return roles.value.filter((role) => [role.code, role.name, formatRoleDisplayName(role)]
    .some((item) => item.toLowerCase().includes(value)));
});

const activeRole = computed(() => roles.value.find((role) => role.id === selectedRoleId.value) || null);
const menuByCode = computed(() => new Map(menus.value.map((menu) => [menu.code, menu])));
const activeLeafMenuCode = computed(() => (
  selectedMenuNode.value?.menuCodes.length === 1 ? selectedMenuNode.value.menuCodes[0] : null
));
const selectedPageConfig = computed(() => findColumnPermissionPageConfig(activeLeafMenuCode.value));
const activePageMenu = computed(() => (
  activeLeafMenuCode.value ? menuByCode.value.get(activeLeafMenuCode.value) || null : null
));
const activePageTitle = computed(() => activePageMenu.value?.title || selectedMenuNode.value?.title || '角色列权限');
const activePagePath = computed(() => activePageMenu.value?.path || '');
const readonly = computed(() => !authStore.hasPermission('iam-role-column-permission:update') || targetTenantId.value <= 0);
const canSaveRoleRules = computed(() => Boolean(activeRole.value) && !readonly.value && isRuleDirty.value);
const missingDependencyHints = computed(() => roleColumnPermissionDependencies
  .filter((dependency) => !authStore.hasPermission(dependency.permission))
  .map((dependency) => dependency.message));

const tenantAssignableRuleByColumnId = computed(() => {
  const next = new Map<number, IamColumnPermissionTemplateItem>();
  for (const rule of tenantAssignableRules.value) {
    const current = next.get(rule.resourceColumnId);
    if (!current || accessRank(rule.accessMode) > accessRank(current.accessMode)) {
      next.set(rule.resourceColumnId, rule);
    }
  }
  return next;
});

const selectedPageSchemaColumns = computed<SchemaPermissionColumn[]>(() => (
  selectedPageConfig.value ? resolvePageSchemaColumns(selectedPageConfig.value) : []
));

const selectedPageColumns = computed(() => {
  if (!selectedPageConfig.value) {
    return [];
  }
  const schemaColumnMap = new Map(
    selectedPageSchemaColumns.value.map((column) => [`${column.resourceKey}:${column.columnKey}`, column]),
  );
  return resourceColumns.value
    .filter((column) => schemaColumnMap.has(`${column.resourceKey}:${column.columnKey}`))
    .sort((left, right) => {
      const leftOrder = schemaColumnMap.get(`${left.resourceKey}:${left.columnKey}`)?.order ?? left.sortNo;
      const rightOrder = schemaColumnMap.get(`${right.resourceKey}:${right.columnKey}`)?.order ?? right.sortNo;
      return leftOrder - rightOrder;
    });
});

const selectedAssignableColumns = computed(() => (
  selectedPageColumns.value.filter((column) => isTenantColumnAssignable(column.id))
));

const selectedAssignableRules = computed(() => selectedAssignableColumns.value.flatMap((column) => {
  const rule = tenantAssignableRuleByColumnId.value.get(column.id);
  return rule ? [rule] : [];
}));

const isRuleDirty = computed(() => {
  const columnIds = selectedAssignableRules.value.map((rule) => rule.resourceColumnId);
  return columnIds.some((columnId) => resolveColumnAccess(columnId) !== (initialRuleForm.value[columnId] || resolveMaxAccess(columnId)));
});

const previewTableSchema = computed(() => {
  const pageConfig = selectedPageConfig.value;
  if (!pageConfig) {
    return null;
  }
  const schema = createPermissionSourceTableSchema(pageConfig) as XuanBrowseTableSchema<PreviewRow> | null;
  return schema ? filterPreviewTableSchemaByAssignableColumns(schema) : null;
});

const previewColumnPermissionSnapshot = computed<BrowseTableColumnPermissionSnapshot>(() => Object.fromEntries(
  selectedAssignableColumns.value.flatMap((column) => {
    const permissionKey = createBrowseTablePermissionKey(column.resourceKey, column.columnKey);
    return permissionKey ? [[permissionKey, resolveColumnAccess(column.id)]] : [];
  }),
));

const previewHintText = computed(() => {
  if (!activeRole.value) {
    return '先选择角色，再查看真实页面预览。';
  }
  if (!selectedPageConfig.value) {
    return '当前菜单没有接入列权限 schema。';
  }
  if (!previewTableSchema.value) {
    return '当前页面还没有接入通用表格预览，后续抽出共享 schema 后这里会自动显示真实页面。';
  }
  return '租户模板池中暂无当前页面可授权字段。';
});

onMounted(async () => {
  await Promise.all([loadTenantOptions(), loadRoleColumnVisibleMenus()]);
  await loadPage();
});

onBeforeUnmount(stopWorkbenchResize);

watch(selectedRoleId, async () => {
  await loadRoleRules();
});

watch(selectedPageConfig, async () => {
  await loadPreviewRows();
});

async function loadPage() {
  roleColumnPermissionErrorMessage.value = '';
  await loadRoleColumnVisibleMenus();
  const nextTenantId = targetTenantId.value;
  if (nextTenantId <= 0) {
    roles.value = [];
    selectedRoleId.value = null;
    tenantAssignableRules.value = [];
    resetRules();
    ElMessage.warning(showTenantSelector.value ? '请选择租户' : '当前账号缺少有效租户上下文');
    return;
  }
  loading.value = true;
  try {
    const [nextRoles, nextColumns, assignments] = await Promise.all([
      listIamRoles(nextTenantId),
      listIamResourceColumns(),
      listIamTenantColumnPermissionTemplates(nextTenantId),
    ]);
    roles.value = nextRoles;
    resourceColumns.value = nextColumns;
    tenantTemplateAssignments.value = assignments.map((assignment) => ({
      templateId: assignment.templateId,
      templateName: assignment.templateName,
    }));
    const itemGroups = await Promise.all(assignments
      .filter((assignment) => assignment.templateEnabled)
      .map((assignment) => getIamColumnPermissionTemplateItems(assignment.templateId)));
    tenantAssignableRules.value = itemGroups.flat();
    syncSelectedRole(nextRoles);
    syncSelectedMenu(menus.value);
    await loadRoleRules();
    await loadPreviewRows();
  } catch (error) {
    roles.value = [];
    resourceColumns.value = [];
    tenantTemplateAssignments.value = [];
    tenantAssignableRules.value = [];
    selectedRoleId.value = null;
    resetRules();
    handleRoleColumnPermissionApiError(error, '角色列权限页面数据加载失败，请检查接口权限或稍后重试');
  } finally {
    loading.value = false;
  }
}

async function loadRoleColumnVisibleMenus() {
  if (!authorizationStore.isLoaded) {
    await authorizationStore.refreshCurrentAuthorizationContext();
  }
  menus.value = flattenRoleColumnVisibleMenus(authorizationStore.menus);
}

async function loadTenantOptions() {
  if (!showTenantSelector.value) {
    tenants.value = [];
    return;
  }
  loadingTenants.value = true;
  try {
    const page = await listColumnPermissionTenants(1, 200);
    tenants.value = page.records;
    if (!selectedTenantId.value) {
      selectedTenantId.value = page.records[0]?.id ?? null;
    }
  } catch (error) {
    tenants.value = [];
    selectedTenantId.value = null;
    handleRoleColumnPermissionApiError(error, '租户列表加载失败，请稍后重试');
  } finally {
    loadingTenants.value = false;
  }
}

async function handleTenantChange() {
  selectedRoleId.value = null;
  tenantAssignableRules.value = [];
  resetRules();
  await loadPage();
}

function syncSelectedRole(nextRoles: IamRole[]) {
  if (selectedRoleId.value && nextRoles.some((role) => role.id === selectedRoleId.value)) {
    return;
  }
  selectedRoleId.value = nextRoles[0]?.id ?? null;
}

function syncSelectedMenu(nextMenus: IamMenu[]) {
  const menuCodes = new Set(nextMenus.map((menu) => menu.code));
  if (selectedMenuCode.value && menuCodes.has(selectedMenuCode.value)) {
    return;
  }
  selectedMenuCode.value = nextMenus
    .map((menu) => menu.code)
    .find((code) => Boolean(findColumnPermissionPageConfig(code))) || ALL_MENU_CODE;
}

async function loadRoleRules() {
  resetRules();
  if (!activeRole.value) {
    captureInitialRules();
    return;
  }
  try {
    const rules = await getIamRoleColumnPermissions(activeRole.value.tenantId, activeRole.value.id);
    for (const rule of rules) {
      ruleForm[rule.resourceColumnId] = rule.accessMode;
    }
    roleColumnPermissionErrorMessage.value = '';
  } catch (error) {
    handleRoleColumnPermissionApiError(error, '角色列权限规则加载失败，请检查接口权限或稍后重试');
  }
  captureInitialRules();
}

function resetRules() {
  for (const key of Object.keys(ruleForm)) {
    delete ruleForm[Number(key)];
  }
  for (const rule of selectedAssignableRules.value) {
    ruleForm[rule.resourceColumnId] = rule.accessMode;
  }
}

function captureInitialRules() {
  initialRuleForm.value = Object.fromEntries(
    selectedAssignableRules.value.map((rule) => [rule.resourceColumnId, resolveColumnAccess(rule.resourceColumnId)]),
  );
}

function handleMenuNodeSelect(node: NavigationMenuTreeNode) {
  selectedMenuNode.value = node;
}

function toggleMenuTreeCollapse() {
  isMenuTreeCollapsed.value = !isMenuTreeCollapsed.value;
}

function toggleEditorPanelCollapse() {
  if (isEditorPanelCollapsed.value) {
    isEditorPanelCollapsed.value = false;
    editorPanelHeight.value = lastExpandedEditorHeight.value;
    return;
  }
  if (!isPreviewPanelCollapsed.value) {
    lastExpandedEditorHeight.value = editorPanelHeight.value;
  }
  isPreviewPanelCollapsed.value = false;
  isEditorPanelCollapsed.value = true;
}

function togglePreviewPanelCollapse() {
  if (isPreviewPanelCollapsed.value) {
    isPreviewPanelCollapsed.value = false;
    return;
  }
  if (!isEditorPanelCollapsed.value) {
    lastExpandedEditorHeight.value = editorPanelHeight.value;
  }
  isEditorPanelCollapsed.value = false;
  isPreviewPanelCollapsed.value = true;
}

function startWorkbenchResize(event: MouseEvent) {
  if (isEditorPanelCollapsed.value || isPreviewPanelCollapsed.value) {
    return;
  }
  workbenchResizeState.active = true;
  workbenchResizeState.startY = event.clientY;
  workbenchResizeState.startHeight = editorPanelHeight.value;
  window.addEventListener('mousemove', resizeWorkbench);
  window.addEventListener('mouseup', stopWorkbenchResize);
}

function resizeWorkbench(event: MouseEvent) {
  if (!workbenchResizeState.active || !workbenchRef.value) {
    return;
  }
  const containerHeight = workbenchRef.value.clientHeight;
  const minEditorHeight = 160;
  const minPreviewHeight = 260;
  const dividerHeight = 16;
  const maxEditorHeight = Math.max(minEditorHeight, containerHeight - minPreviewHeight - dividerHeight);
  const nextHeight = workbenchResizeState.startHeight + (event.clientY - workbenchResizeState.startY);
  editorPanelHeight.value = Math.min(Math.max(nextHeight, minEditorHeight), maxEditorHeight);
}

function stopWorkbenchResize() {
  if (!workbenchResizeState.active) {
    window.removeEventListener('mousemove', resizeWorkbench);
    window.removeEventListener('mouseup', stopWorkbenchResize);
    return;
  }
  workbenchResizeState.active = false;
  lastExpandedEditorHeight.value = editorPanelHeight.value;
  window.removeEventListener('mousemove', resizeWorkbench);
  window.removeEventListener('mouseup', stopWorkbenchResize);
}

function countColumnsByMenuCodes(menuCodes: string[]) {
  const menuCodeSet = new Set(menuCodes);
  return columnPermissionPageConfigs.reduce((sum, pageConfig) => {
    if (!menuCodeSet.has(pageConfig.menuCode)) {
      return sum;
    }
    const schemaColumnKeys = new Set(
      resolvePageSchemaColumns(pageConfig).map((column) => `${column.resourceKey}:${column.columnKey}`),
    );
    return sum + resourceColumns.value.filter((column) => (
      schemaColumnKeys.has(`${column.resourceKey}:${column.columnKey}`)
      && isTenantColumnAssignable(column.id)
    )).length;
  }, 0);
}

function flattenRoleColumnVisibleMenus(sourceMenus: CurrentMenuNode[]) {
  const nextId = { value: 1 };
  return flattenRoleColumnVisibleMenuNodes(sourceMenus, null, nextId);
}

function flattenRoleColumnVisibleMenuNodes(
  sourceMenus: CurrentMenuNode[],
  parentId: number | null,
  nextId: { value: number },
): IamMenu[] {
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
      ...flattenRoleColumnVisibleMenuNodes(menu.children || [], id, nextId),
    ];
  });
}

function resolvePageSchemaColumns(pageConfig: NonNullable<typeof selectedPageConfig.value>): SchemaPermissionColumn[] {
  const schema = createPermissionSourceTableSchema(pageConfig);
  if (!schema) {
    return [];
  }
  const detailKeys = new Set(pageConfig.detailColumnKeys || []);
  return schema.columns.flatMap((column, index) => {
    const resourceKey = column.permission?.resourceKey?.trim();
    const columnKey = column.permission?.columnKey?.trim();
    if (!resourceKey || !columnKey) {
      return [];
    }
    return [{
      resourceKey,
      columnKey,
      title: column.title,
      location: detailKeys.has(columnKey) ? 'detail' : 'list',
      order: index,
    }];
  });
}

function createPermissionSourceTableSchema(pageConfig: NonNullable<typeof selectedPageConfig.value>) {
  if (pageConfig.previewSchemaKey === 'tenant-management') {
    return createTenantPreviewTableSchema();
  }
  if (pageConfig.previewSchemaKey === 'iam-user-management') {
    return createIamUserBrowseTableSchema({
      pageCode: 'iam-role-column-preview-iam-user-management',
      tableCode: 'iam-user-list-role-column-preview',
      defaultPageSize: previewPageSize.value,
      actionsWidth: 0,
      toolbar: {
        showDensity: false,
        showColumnSetting: false,
        actions: [],
      },
      pagination: {
        show: false,
      },
      rowActions: [],
      emptyState: {
        title: '暂无用户',
        description: '当前用户管理列表暂无可预览数据。',
      },
    });
  }
  return null;
}

function createTenantPreviewTableSchema() {
  const schema = createTenantBrowseTableSchema({
    pageCode: 'iam-role-column-preview-tenant-management',
    tableCode: 'tenant-list-role-column-preview',
    defaultPageSize: previewPageSize.value,
    actionsWidth: 0,
    toolbar: {
      showDensity: false,
      showColumnSetting: false,
      actions: [],
    },
    pagination: {
      show: false,
    },
    rowActions: [],
    emptyState: {
      title: '暂无租户',
      description: '当前租户列表暂无可预览数据。',
    },
  });
  return {
    ...schema,
    columns: schema.columns.map((column) => ({
      ...column,
      width: column.width || column.minWidth || 140,
    })),
  };
}

function filterPreviewTableSchemaByAssignableColumns(schema: XuanBrowseTableSchema<PreviewRow>) {
  const assignablePermissionKeys = new Set(
    selectedAssignableColumns.value.flatMap((column) => {
      const permissionKey = createBrowseTablePermissionKey(column.resourceKey, column.columnKey);
      return permissionKey ? [permissionKey] : [];
    }),
  );
  return {
    ...schema,
    columns: schema.columns.filter((column) => {
      const permissionKey = createBrowseTablePermissionKey(column.permission?.resourceKey, column.permission?.columnKey);
      return !permissionKey || assignablePermissionKeys.has(permissionKey);
    }),
  };
}

function resolveColumnAccess(columnId: number): AccessMode {
  return ruleForm[columnId] || resolveMaxAccess(columnId);
}

function isTenantColumnAssignable(columnId: number) {
  const rule = tenantAssignableRuleByColumnId.value.get(columnId);
  return Boolean(rule && rule.accessMode !== 'HIDDEN');
}

function resolveMaxAccess(columnId: number): AccessMode {
  return tenantAssignableRuleByColumnId.value.get(columnId)?.accessMode || 'HIDDEN';
}

function resolveAccessOptions(columnId: number) {
  const maxRank = accessRank(resolveMaxAccess(columnId));
  return [
    { label: '明文', value: 'VISIBLE', disabled: maxRank < 3 },
    { label: '脱敏', value: 'MASKED', disabled: maxRank < 2 },
    { label: '隐藏', value: 'HIDDEN' },
  ];
}

function toggleColumnVisibility(column: IamResourceColumn, value: string | number | boolean) {
  const accessMode = String(value) as AccessMode;
  if (accessRank(accessMode) > accessRank(resolveMaxAccess(column.id))) {
    return;
  }
  ruleForm[column.id] = accessMode;
}

function applyMaxAccessToCurrentPage() {
  for (const column of selectedAssignableColumns.value) {
    ruleForm[column.id] = resolveMaxAccess(column.id);
  }
}

function hideCurrentPageColumns() {
  for (const column of selectedAssignableColumns.value) {
    ruleForm[column.id] = 'HIDDEN';
  }
}

function resetPageRules() {
  for (const column of selectedAssignableColumns.value) {
    ruleForm[column.id] = initialRuleForm.value[column.id] || resolveMaxAccess(column.id);
  }
}

async function saveRoleColumnRules() {
  if (!activeRole.value || readonly.value) {
    return;
  }
  submitting.value = true;
  try {
    await setIamRoleColumnPermissions(activeRole.value.id, {
      tenantId: activeRole.value.tenantId,
      rules: selectedAssignableRules.value.map((rule) => ({
        resourceColumnId: rule.resourceColumnId,
        accessMode: resolveColumnAccess(rule.resourceColumnId),
      })),
      operator: authStore.username,
    });
    captureInitialRules();
    ElMessage.success('角色列权限已保存');
    await refreshCurrentAuthorizationIfNeeded();
  } finally {
    submitting.value = false;
  }
}

async function refreshCurrentAuthorizationIfNeeded() {
  if (
    activeRole.value
    && activeRole.value.tenantId === authStore.tenantId
    && authStore.currentUser?.roles.includes(activeRole.value.code)
  ) {
    await authorizationStore.refreshCurrentAuthorizationContext();
  }
}

function handleRoleColumnPermissionApiError(error: unknown, fallbackMessage: string) {
  roleColumnPermissionErrorMessage.value = resolveRoleColumnPermissionForbiddenMessage(error)
    || missingDependencyHints.value[0]
    || fallbackMessage;
}

function resolveColumnLocation(column: IamResourceColumn) {
  return selectedPageSchemaColumns.value.find((item) => (
    item.resourceKey === column.resourceKey && item.columnKey === column.columnKey
  ))?.location === 'detail' ? '详情字段' : '列表列';
}

function resolveAccessLabel(value: AccessMode) {
  if (value === 'VISIBLE') {
    return '明文';
  }
  if (value === 'MASKED') {
    return '脱敏';
  }
  return '隐藏';
}

function accessRank(accessMode: string) {
  if (accessMode === 'VISIBLE') {
    return 3;
  }
  if (accessMode === 'MASKED') {
    return 2;
  }
  return 1;
}

function formatRoleLabel(role: IamRole) {
  return `${formatRoleDisplayName(role)}（${role.code}）`;
}

function formatRoleDisplayName(role: IamRole) {
  const name = role.name.trim();
  if (name && !hasCorruptedText(name)) {
    return name;
  }
  if (role.code === 'codex_tenant_viewer') {
    return 'Codex 租户查看员';
  }
  if (role.code === 'tenant_readonly') {
    return '租户只读角色';
  }
  if (role.code === 'tenant_admin') {
    return '租户管理员';
  }
  if (role.code === 'tenant_owner') {
    return '租户拥有者';
  }
  return role.code;
}

function hasCorruptedText(value: string) {
  return /\?{2,}|�/.test(value);
}

async function loadPreviewRows() {
  if (selectedPageConfig.value?.previewSchemaKey === 'iam-user-management') {
    await loadIamUserPreviewRows();
    return;
  }
  if (selectedPageConfig.value?.previewSchemaKey !== 'tenant-management') {
    previewRows.value = [];
    previewTotal.value = 0;
    return;
  }
  previewLoading.value = true;
  try {
    const page = await listColumnPermissionTenants(previewCurrentPage.value, previewPageSize.value);
    previewRows.value = page.records;
    previewTotal.value = page.total;
    previewCurrentPage.value = page.pageNum;
    previewPageSize.value = page.pageSize;
  } catch {
    previewRows.value = [];
    previewTotal.value = 0;
  } finally {
    previewLoading.value = false;
  }
}

async function loadIamUserPreviewRows() {
  previewLoading.value = true;
  try {
    const users = await listIamUsers(targetTenantId.value);
    const start = (previewCurrentPage.value - 1) * previewPageSize.value;
    previewRows.value = users.slice(start, start + previewPageSize.value);
    previewTotal.value = users.length;
  } catch {
    previewRows.value = [];
    previewTotal.value = 0;
  } finally {
    previewLoading.value = false;
  }
}
</script>

<style scoped>
.role-filter {
  width: 240px;
}

.tenant-filter {
  width: 240px;
}

.current-tenant-tag {
  height: 32px;
  max-width: 240px;
  display: inline-flex;
  align-items: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-column-dependency-strip {
  margin-bottom: 12px;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(0, 1fr);
  gap: 12px;
  padding: 12px 14px;
  background: var(--xuan-panel);
}

.role-column-dependency-strip.warning {
  border-color: #fecaca;
  background: #fff7ed;
}

.role-column-dependency-strip strong {
  display: block;
  margin-bottom: 4px;
  color: var(--xuan-text);
  font-size: 13px;
}

.role-column-dependency-strip span {
  color: var(--xuan-muted);
  font-size: 12px;
  line-height: 1.5;
}

.dependency-items {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dependency-chip {
  border: 1px solid #dbeafe;
  border-radius: 999px;
  padding: 4px 10px;
  background: #eff6ff;
  color: #1d4ed8 !important;
  white-space: nowrap;
}

.dependency-chip.missing,
.dependency-error {
  border-color: #fecaca;
  background: #fee2e2;
  color: #b91c1c !important;
}

.dependency-error {
  grid-column: 1 / -1;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 8px 10px;
}

.role-column-workspace {
  width: 100%;
  min-height: 0;
  height: 100%;
  display: grid;
  grid-template-columns: var(--role-column-tree-width, 300px) minmax(0, 1fr);
  gap: 16px;
  overflow: hidden;
}

.role-column-tree-shell {
  min-width: 0;
  min-height: 0;
  position: relative;
}

.role-column-tree-collapse {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  width: 28px;
  height: 28px;
  border: 1px solid var(--xuan-border);
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--xuan-panel);
  color: var(--xuan-primary);
  cursor: pointer;
}

.role-column-tree-shell.collapsed .role-column-tree-collapse {
  position: static;
  margin: 10px auto 6px;
}

.role-column-tree-collapsed {
  height: 100%;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--xuan-panel);
  color: var(--xuan-muted);
  font-size: 12px;
  writing-mode: vertical-rl;
}

.role-column-main {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.role-column-editor-panel,
.role-column-preview-panel {
  min-width: 0;
  min-height: 0;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  background: var(--xuan-panel);
  overflow: hidden;
}

.role-column-editor-head,
.role-column-preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--xuan-border);
}

.role-column-editor-title,
.role-column-preview-head > div:first-child {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.role-column-editor-title strong,
.role-column-preview-head strong {
  color: var(--xuan-text);
  font-size: 18px;
  line-height: 1.3;
}

.role-column-editor-title span,
.role-column-preview-head span,
.column-empty-state span {
  color: var(--xuan-muted);
  font-size: 12px;
  line-height: 1.6;
}

.role-column-meta-row,
.role-column-editor-actions,
.preview-head-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.role-column-editor-actions,
.preview-head-actions {
  justify-content: flex-end;
}

.column-panel-toggle {
  width: 28px;
  height: 28px;
  border: 1px solid var(--xuan-border);
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: var(--xuan-primary);
  cursor: pointer;
}

.panel-collapsed-tip {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--xuan-muted);
  font-size: 12px;
}

.mini-badge,
.preview-source-badge {
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 11px;
  line-height: 1.2;
  white-space: nowrap;
}

.mini-badge {
  color: #64748b;
  background: #f1f5f9;
}

.preview-source-badge {
  color: #1d4ed8;
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

.editor-action {
  height: 32px;
  border: 1px solid var(--xuan-border);
  border-radius: 999px;
  padding: 0 12px;
  background: #fff;
  color: #334155;
  font-size: 12px;
  cursor: pointer;
}

.editor-action:disabled {
  color: #94a3b8;
  cursor: not-allowed;
  background: #f8fafc;
}

.editor-action.primary {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.field-grid {
  flex: 1 1 0;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(260px, 1fr));
  gap: 10px;
  padding: 12px;
  overflow: auto;
}

.field-card {
  min-height: 74px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background: #fff;
}

.field-card.off {
  background: #f8fafc;
}

.field-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.field-info strong {
  color: var(--xuan-text);
  font-size: 13px;
  line-height: 1.35;
}

.field-info span {
  color: var(--xuan-muted);
  font-size: 11px;
  line-height: 1.35;
  word-break: break-all;
}

.workbench-divider {
  height: 16px;
  flex: 0 0 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: row-resize;
  user-select: none;
}

.workbench-divider.disabled {
  cursor: default;
}

.workbench-divider-line {
  width: 100%;
  height: 2px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--xuan-primary) 18%, var(--xuan-border));
}

.workbench-divider.dragging .workbench-divider-line {
  background: var(--xuan-primary);
}

.preview-shell {
  flex: 1 1 0;
  min-height: 0;
  padding: 14px;
  background: #f8fafc;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-page {
  min-width: 0;
  min-height: 0;
  flex: 1 1 0;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-page :deep(.table-shell) {
  height: 100%;
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  border: 0;
  border-radius: 0;
}

.preview-page :deep(.table-shell-body) {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-page :deep(.el-table) {
  width: 100%;
  min-width: 980px;
  min-height: 0;
}

.preview-page :deep(.el-scrollbar__wrap) {
  overflow: auto;
}

.column-empty-state,
.preview-empty-state {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  text-align: center;
}

.column-empty-state strong {
  color: var(--xuan-text);
}

@media (max-width: 1380px) {
  .field-grid {
    grid-template-columns: repeat(2, minmax(240px, 1fr));
  }
}

@media (max-width: 760px) {
  .tenant-filter,
  .role-filter {
    width: 100%;
  }

  .role-column-workspace {
    height: auto;
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .role-column-main {
    overflow: visible;
  }

  .role-column-editor-head,
  .role-column-preview-head {
    flex-direction: column;
    align-items: stretch;
  }

  .role-column-editor-actions,
  .preview-head-actions {
    justify-content: flex-start;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
