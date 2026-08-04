<template>
  <ListPageShell :title="pageTitle">
    <template v-if="viewMode === 'list'" #query>
      <QueryToolbar>
        <el-input v-model="templateKeyword" class="query-input" placeholder="搜索模板编码 / 名称" clearable />
        <el-select v-model="enabledFilter" class="status-filter" placeholder="状态" clearable>
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
        <template #actions>
          <el-button :icon="RefreshCw" circle @click="loadPage" />
          <PermissionButton type="primary" permission="iam-column-permission:create" no-permission-mode="disable" @click="openCreate">
            新增模板
          </PermissionButton>
        </template>
      </QueryToolbar>
    </template>

    <template v-if="viewMode === 'detail'" #actions>
      <el-button :icon="ArrowLeft" @click="backToTemplateList">返回模板列表</el-button>
    </template>

    <section v-if="viewMode === 'list'" class="column-template-list">
      <header class="column-template-list-head">
        <strong>列权限模板列表</strong>
        <span>先管理有哪些列权限模板，再进入模板维护具体页面字段规则。</span>
      </header>
      <el-table v-loading="loading" :data="filteredTemplates" row-key="id" border height="100%">
        <el-table-column prop="code" label="模板编码" min-width="180" />
        <el-table-column prop="name" label="模板名称" min-width="180" />
        <el-table-column label="租户范围" width="120">
          <template #default="{ row }">
            <el-tag :type="row.tenantId === 0 ? 'primary' : 'info'" effect="plain">{{ formatTemplateTenant(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="300" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <PermissionButton link type="primary" permission="iam-column-permission:update" no-permission-mode="disable" @click="openTemplateDetail(row)">字段</PermissionButton>
            <PermissionButton link type="primary" permission="iam-column-permission:update" no-permission-mode="disable" @click="openEdit(row)">编辑</PermissionButton>
            <PermissionButton link type="primary" permission="iam-column-permission:update" no-permission-mode="disable" @click="toggleTemplate(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </PermissionButton>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div v-else class="column-workspace" :style="workspaceStyle">
      <aside class="column-tree-shell" :class="{ collapsed: isMenuTreeCollapsed }">
        <button
          type="button"
          class="column-tree-collapse"
          :aria-label="isMenuTreeCollapsed ? '展开菜单树' : '收起菜单树'"
          @click="toggleMenuTreeCollapse"
        >
          <PanelLeftOpen v-if="isMenuTreeCollapsed" :size="16" />
          <PanelLeftClose v-else :size="16" />
        </button>

        <div v-if="isMenuTreeCollapsed" class="column-tree-collapsed">
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

      <section ref="workbenchRef" class="column-workbench">
        <section class="column-editor-panel" :style="editorPanelStyle">
          <template v-if="activeTemplate">
            <header class="column-editor-head">
              <div class="column-editor-head-main">
                <strong>{{ activePageTitle }}</strong>
                <div class="column-editor-head-desc">字段配置器。这里专门处理当前页面有哪些字段要显示。</div>
                <div class="column-editor-meta-row">
                  <span v-if="activePagePath" class="mini-badge">路径 {{ activePagePath }}</span>
                  <span v-if="activeLeafMenuCode" class="mini-badge">菜单编码 {{ activeLeafMenuCode }}</span>
                  <span class="mini-badge">{{ activeTemplate.name }}（{{ activeTemplate.code }}）</span>
                  <span class="mini-badge" :class="{ primary: activeTemplate.enabled }">
                    {{ activeTemplate.enabled ? '已接入字段权限' : '模板已停用' }}
                  </span>
                  <span class="mini-badge">当前显示 {{ visibleColumnCount }} / {{ totalColumnCount }}</span>
                </div>
              </div>
              <div class="column-editor-actions">
                <button
                  type="button"
                  class="column-panel-toggle"
                  :aria-label="isEditorPanelCollapsed ? '展开字段配置器' : '收起字段配置器'"
                  @click="toggleEditorPanelCollapse"
                >
                  <ChevronDown v-if="isEditorPanelCollapsed" :size="16" />
                  <ChevronUp v-else :size="16" />
                </button>
                <button type="button" class="editor-action primary" :disabled="!selectedPageColumns.length" @click="applyVisibleToCurrentPage">
                  全部显示
                </button>
                <button type="button" class="editor-action" :disabled="!selectedPageColumns.length" @click="hideSensitiveColumns">
                  隐藏敏感字段
                </button>
                <button type="button" class="editor-action" :disabled="!selectedPageColumns.length" @click="resetPageRules">
                  恢复默认
                </button>
                <PermissionButton
                  type="primary"
                  permission="iam-column-permission:update"
                  no-permission-mode="disable"
                  :disabled="!selectedPageConfig || !isRuleDirty"
                  :loading="submitting"
                  @click="saveTemplateRules"
                >
                  保存规则
                </PermissionButton>
              </div>
            </header>

            <div v-if="isEditorPanelCollapsed" class="panel-collapsed-tip">
              字段配置器已折叠，点击右侧箭头展开。
            </div>

            <div v-else-if="!selectedPageConfig" class="column-empty-state">
              <strong>{{ activePageTitle }}</strong>
              <span>{{ selectionHintText }}</span>
            </div>

            <div v-else-if="!selectedPageColumns.length" class="column-empty-state">
              <strong>{{ activePageTitle }}</strong>
              <span>当前页面还没有接入字段资源，后续补齐 resourceKey 后这里会自动出现可配置字段。</span>
            </div>

            <template v-else>
              <div class="field-grid">
                <article
                  v-for="column in selectedPageColumns"
                  :key="column.id"
                  class="field-card"
                  :class="{ off: !isColumnVisible(column.id) }"
                >
                  <div class="field-info">
                    <strong>{{ column.columnName }}</strong>
                    <span>{{ column.columnKey }} · {{ resolveColumnLocation(column) }} · {{ resolveCurrentAccessLabel(column) }}</span>
                  </div>
                  <div class="field-side">
                    <span class="pill" :class="resolveSensitivityClass(column)">{{ resolveSensitivityLabel(column) }}</span>
                    <el-switch
                      :model-value="isColumnVisible(column.id)"
                      @update:model-value="toggleColumnVisibility(column, $event)"
                    />
                  </div>
                </article>
              </div>
              <div class="editor-foot">
                敏感字段重新打开时会优先恢复原始可见模式；支持脱敏的字段在没有历史规则时默认恢复到脱敏显示。
              </div>
            </template>
          </template>

          <div v-else class="column-empty-state">
            <strong>请选择列权限模板</strong>
            <span>先在上方选择模板，或者直接新建模板，然后再进入页面字段配置。</span>
          </div>
        </section>

        <div
          class="workbench-divider"
          :class="{ dragging: workbenchResizeState.active, disabled: isEditorPanelCollapsed || isPreviewPanelCollapsed }"
          @mousedown.prevent="startWorkbenchResize"
        >
          <span class="workbench-divider-line" />
        </div>

        <section class="column-preview-panel" :style="previewPanelStyle">
          <header class="column-preview-head">
            <div class="preview-summary-head">
              <strong>{{ activePageTitle }}预览</strong>
              <div class="preview-summary-subline">
                <span>按当前列权限模板实时展示页面效果。</span>
                <div class="preview-summary-meta">
                  <span class="preview-summary-chip">{{ activeTemplate?.name || '未选择模板' }}</span>
                  <span v-if="hiddenColumnNames.length" class="preview-summary-chip warn">已隐藏：{{ hiddenColumnNames.join(' / ') }}</span>
                  <span v-else class="preview-summary-chip success">当前显示全部已接入字段</span>
                </div>
              </div>
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

          <div v-else-if="activeTemplate && previewTableSchema" class="preview-shell">
            <section class="preview-page">
              <XuanBrowseTable
                v-loading="previewLoading"
                v-model:current-page="previewCurrentPage"
                v-model:page-size="previewPageSize"
                v-model:density="previewDensity"
                :schema="previewTableSchema"
                :tenant-id="String(authStore.tenantId ?? 0)"
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

    <DynamicFormDialog
      v-model="templateDialogVisible"
      :title="editingTemplate ? '编辑列权限模板' : '新增列权限模板'"
      :fields="templateFormFields"
      :sections="templateFormSections"
      :model="form"
      size="md"
      label-position="top"
      :confirm-permission="editingTemplate ? 'iam-column-permission:update' : 'iam-column-permission:create'"
      :loading="submitting"
      @submit="submitTemplate"
    />
  </ListPageShell>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index';
import { ArrowLeft, ChevronDown, ChevronUp, PanelLeftClose, PanelLeftOpen, RefreshCw } from 'lucide-vue-next';
import {
  createIamColumnPermissionTemplate,
  getIamColumnPermissionTemplateItems,
  listIamUsers,
  listIamColumnPermissionTemplates,
  listIamMenuOptions,
  listIamResourceColumns,
  setIamColumnPermissionTemplateEnabled,
  setIamColumnPermissionTemplateItems,
  updateIamColumnPermissionTemplate,
} from '@/api/iamAdmin';
import { listColumnPermissionTenants } from '@/api/tenants';
import { columnPermissionPageConfigs, findColumnPermissionPageConfig } from '@/config/columnPermissionPages';
import { createIamUserBrowseTableSchema } from '@/config/iamUserBrowseTableSchema';
import { createTenantBrowseTableSchema } from '@/config/tenantBrowseTableSchema';
import DynamicFormDialog from '@/framework/components/DynamicFormDialog.vue';
import type { DynamicFormField, DynamicFormSection } from '@/framework/components/DynamicFormDialog.vue';
import ListPageShell from '@/framework/components/ListPageShell.vue';
import NavigationMenuTree, { type NavigationMenuTreeNode } from '@/framework/components/NavigationMenuTree.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import QueryToolbar from '@/framework/components/QueryToolbar.vue';
import XuanBrowseTable from '@/framework/components/XuanBrowseTable.vue';
import type { BrowseTableDensity } from '@/framework/components/browseTablePreferences';
import type { BrowseTableColumnPermissionSnapshot, XuanBrowseTableSchema } from '@/framework/components/browseTableSchema';
import { useAuthorizationStore } from '@/stores/authorization';
import { useAuthStore } from '@/stores/auth';
import type {
  IamColumnPermissionTemplate,
  IamColumnPermissionTemplatePayload,
  IamMenu,
  IamResourceColumn,
} from '@/types/iamAdmin';
import type { Tenant } from '@/types/tenant';

defineOptions({ name: 'IamColumnPermissionManagementView' });

type AccessMode = 'VISIBLE' | 'MASKED' | 'HIDDEN';
type ViewMode = 'list' | 'detail';
type PreviewRow = Record<string, unknown>;
type SchemaPermissionColumn = {
  resourceKey: string;
  columnKey: string;
  title: string;
  location: 'list' | 'detail';
  order: number;
};

const ALL_MENU_CODE = '__all__';

const authStore = useAuthStore();
const authorizationStore = useAuthorizationStore();
const workbenchRef = ref<HTMLElement | null>(null);
const loading = ref(false);
const submitting = ref(false);
const templateDialogVisible = ref(false);
const tenantId = ref(authStore.tenantId || 0);
const viewMode = ref<ViewMode>('list');
const templateKeyword = ref('');
const enabledFilter = ref<boolean | ''>('');
const menuKeyword = ref('');
const isMenuTreeCollapsed = ref(false);
const selectedTemplateId = ref<number | null>(null);
const selectedMenuCode = ref(ALL_MENU_CODE);
const selectedMenuNode = ref<NavigationMenuTreeNode | null>(null);
const menus = ref<IamMenu[]>([]);
const templates = ref<IamColumnPermissionTemplate[]>([]);
const resourceColumns = ref<IamResourceColumn[]>([]);
const editingTemplate = ref<IamColumnPermissionTemplate | null>(null);
const editorPanelHeight = ref(240);
const isEditorPanelCollapsed = ref(false);
const isPreviewPanelCollapsed = ref(false);
const lastExpandedEditorHeight = ref(240);
const previewCurrentPage = ref(1);
const previewPageSize = ref(10);
const previewDensity = ref<BrowseTableDensity>('default');
const previewLoading = ref(false);
const previewRows = ref<PreviewRow[]>([]);
const previewTotal = ref(0);
const workbenchResizeState = reactive({
  active: false,
  startY: 0,
  startHeight: 240,
});
const initialRuleForm = ref<Record<number, AccessMode>>({});
const ruleForm = reactive<Record<number, AccessMode>>({});
const form = reactive<IamColumnPermissionTemplatePayload>({
  tenantId: Number(tenantId.value || 0),
  code: '',
  name: '',
  description: '',
  enabled: true,
});

const menuByCode = computed(() => new Map(menus.value.map((menu) => [menu.code, menu])));
const pageTitle = computed(() => (
  viewMode.value === 'detail' && activeTemplate.value
    ? `列权限 - ${activeTemplate.value.name}`
    : '列权限'
));
const workspaceStyle = computed(() => ({
  '--column-tree-width': isMenuTreeCollapsed.value ? '52px' : '300px',
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

const activeTemplate = computed(() => (
  templates.value.find((item) => item.id === selectedTemplateId.value) || null
));

const filteredTemplates = computed(() => {
  const keyword = templateKeyword.value.trim().toLowerCase();
  return templates.value.filter((template) => {
    if (enabledFilter.value !== '' && template.enabled !== enabledFilter.value) {
      return false;
    }
    if (!keyword) {
      return true;
    }
    return [template.code, template.name, template.description || '']
      .some((item) => item.toLowerCase().includes(keyword));
  });
});

const activeLeafMenuCode = computed(() => (
  selectedMenuNode.value?.menuCodes.length === 1 ? selectedMenuNode.value.menuCodes[0] : null
));

const selectedPageConfig = computed(() => findColumnPermissionPageConfig(activeLeafMenuCode.value));

const activePageMenu = computed(() => (
  activeLeafMenuCode.value ? menuByCode.value.get(activeLeafMenuCode.value) || null : null
));

const activePageTitle = computed(() => (
  activePageMenu.value?.title || selectedMenuNode.value?.title || '列权限'
));

const activePagePath = computed(() => activePageMenu.value?.path || '');

function createPermissionSourceTableSchema(pageConfig: NonNullable<typeof selectedPageConfig.value>) {
  if (pageConfig.previewSchemaKey === 'tenant-management') {
    return createPreviewTableSchema();
  }
  if (pageConfig.previewSchemaKey === 'iam-user-management') {
    return createIamUserBrowseTableSchema({
      pageCode: 'iam-column-source-iam-user-management',
      tableCode: 'iam-user-list-source',
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
    });
  }
  return null;
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

function createPreviewTableSchema() {
  const schema = createTenantBrowseTableSchema({
    pageCode: 'iam-column-preview-tenant-management',
    tableCode: 'tenant-list-preview',
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

const previewTableSchema = computed(() => {
  const pageConfig = selectedPageConfig.value;
  if (!pageConfig) {
    return null;
  }
  return createPermissionSourceTableSchema(pageConfig) as XuanBrowseTableSchema<PreviewRow> | null;
});

const selectedPageSchemaColumns = computed<SchemaPermissionColumn[]>(() => {
  const pageConfig = selectedPageConfig.value;
  if (!pageConfig) {
    return [];
  }
  return resolvePageSchemaColumns(pageConfig);
});

const selectedPageColumns = computed(() => {
  const pageConfig = selectedPageConfig.value;
  if (!pageConfig) {
    return [];
  }
  const schemaColumnMap = new Map(
    selectedPageSchemaColumns.value.map((column) => [`${column.resourceKey}::${column.columnKey}`, column]),
  );
  return resourceColumns.value
    .filter((column) => schemaColumnMap.has(`${column.resourceKey}::${column.columnKey}`))
    .sort((left, right) => {
      const leftOrder = schemaColumnMap.get(`${left.resourceKey}::${left.columnKey}`)?.order ?? Number.MAX_SAFE_INTEGER;
      const rightOrder = schemaColumnMap.get(`${right.resourceKey}::${right.columnKey}`)?.order ?? Number.MAX_SAFE_INTEGER;
      if (leftOrder !== rightOrder) {
        return leftOrder - rightOrder;
      }
      return left.sortNo - right.sortNo;
    });
});

const totalColumnCount = computed(() => selectedPageColumns.value.length);

const visibleColumnCount = computed(() => (
  selectedPageColumns.value.filter((column) => resolveColumnAccess(column.id) !== 'HIDDEN').length
));

const hiddenColumnNames = computed(() => selectedPageColumns.value
  .filter((column) => resolveColumnAccess(column.id) === 'HIDDEN')
  .map((column) => column.columnName));

const previewColumnPermissionSnapshot = computed<BrowseTableColumnPermissionSnapshot>(() => Object.fromEntries(
  selectedPageColumns.value.map((column) => [`${column.resourceKey}::${column.columnKey}`, resolveColumnAccess(column.id)]),
));

const selectionHintText = computed(() => {
  if (!selectedMenuNode.value || selectedMenuCode.value === ALL_MENU_CODE) {
    return '请先从左侧选择一个具体页面，再配置这个页面的字段显示规则。';
  }
  if (selectedMenuNode.value.menuCodes.length > 1) {
    return '当前选中的是导航分组，请继续点到具体页面后再配置字段。';
  }
  return '当前页面暂未接入字段权限，后续补齐 resourceKey 映射后这里会自动可配。';
});

const previewHintText = computed(() => {
  if (!activeTemplate.value) {
    return '先选择一个列权限模板，再进入页面预览。';
  }
  if (!selectedPageConfig.value) {
    return selectionHintText.value;
  }
  if (!previewTableSchema.value) {
    return '当前页面还没有接入通用表格预览，后续抽出共享 schema 后这里会自动显示真实页面。';
  }
  return '当前页面没有可展示的字段，预览区暂时为空。';
});

const isRuleDirty = computed(() => resourceColumns.value.some((column) => (
  resolveColumnAccess(column.id) !== (initialRuleForm.value[column.id] || 'VISIBLE')
)));

const templateFormFields = computed<DynamicFormField[]>(() => [
  {
    key: 'code',
    label: '模板编码',
    placeholder: 'tenant_admin_default',
    required: true,
    disabled: Boolean(editingTemplate.value),
    span: 12,
  },
  {
    key: 'name',
    label: '模板名称',
    placeholder: '租户管理列权限模板',
    required: true,
    span: 12,
  },
  {
    key: 'enabled',
    label: '启用',
    component: 'switch',
    span: 12,
  },
  {
    key: 'description',
    label: '说明',
    component: 'textarea',
    placeholder: '模板用途说明',
    span: 24,
  },
]);

const templateFormSections = computed<DynamicFormSection[]>(() => [{
  title: '模板信息',
  fields: templateFormFields.value,
}]);

onMounted(loadPage);
onBeforeUnmount(stopWorkbenchResize);

watch(
  () => {
    const schemaKey = selectedPageConfig.value?.previewSchemaKey || '';
    return schemaKey ? `${schemaKey}:${previewCurrentPage.value}:${previewPageSize.value}` : '';
  },
  (requestKey) => {
    if (!requestKey) {
      previewRows.value = [];
      previewTotal.value = 0;
      return;
    }
    void loadPreviewRows();
  },
  { immediate: true },
);

async function loadPage(preferredTemplateId?: number | null, preferredMenuCode?: string | null) {
  loading.value = true;
  try {
    const [nextMenus, nextTemplates, nextColumns] = await Promise.all([
      listIamMenuOptions(),
      listIamColumnPermissionTemplates({
        tenantId: Number.isFinite(Number(tenantId.value)) ? Number(tenantId.value) : undefined,
      }),
      listIamResourceColumns(),
    ]);
    menus.value = nextMenus;
    templates.value = nextTemplates;
    resourceColumns.value = nextColumns.filter((column) => column.enabled);
    if (viewMode.value === 'detail' || preferredTemplateId) {
      viewMode.value = 'detail';
      syncSelectedTemplate(nextTemplates, preferredTemplateId);
      syncSelectedMenu(nextMenus, preferredMenuCode);
      await loadTemplateItems(selectedTemplateId.value);
    } else {
      selectedTemplateId.value = null;
      resetRules();
      captureInitialRules();
    }
  } finally {
    loading.value = false;
  }
}

function toggleMenuTreeCollapse() {
  isMenuTreeCollapsed.value = !isMenuTreeCollapsed.value;
}

function syncSelectedTemplate(nextTemplates: IamColumnPermissionTemplate[], preferredTemplateId?: number | null) {
  const candidateId = preferredTemplateId ?? selectedTemplateId.value;
  if (candidateId && nextTemplates.some((item) => item.id === candidateId)) {
    selectedTemplateId.value = candidateId;
    return;
  }
  selectedTemplateId.value = nextTemplates[0]?.id ?? null;
}

function syncSelectedMenu(nextMenus: IamMenu[], preferredMenuCode?: string | null) {
  const menuCodes = new Set(nextMenus.map((menu) => menu.code));
  const candidateCode = preferredMenuCode ?? selectedMenuCode.value;
  if (candidateCode && menuCodes.has(candidateCode)) {
    selectedMenuCode.value = candidateCode;
    return;
  }
  const firstConfigurableCode = nextMenus
    .map((menu) => menu.code)
    .find((code) => Boolean(findColumnPermissionPageConfig(code)));
  selectedMenuCode.value = firstConfigurableCode || ALL_MENU_CODE;
}

async function openTemplateDetail(row: IamColumnPermissionTemplate) {
  tenantId.value = row.tenantId;
  viewMode.value = 'detail';
  await loadPage(row.id, activeLeafMenuCode.value);
}

function backToTemplateList() {
  stopWorkbenchResize();
  viewMode.value = 'list';
  selectedTemplateId.value = null;
  selectedMenuCode.value = ALL_MENU_CODE;
  selectedMenuNode.value = null;
  resetRules();
  captureInitialRules();
}

async function loadTemplateItems(templateId: number | null) {
  resetRules();
  if (templateId) {
    const items = await getIamColumnPermissionTemplateItems(templateId);
    for (const item of items) {
      ruleForm[item.resourceColumnId] = item.accessMode;
    }
  }
  captureInitialRules();
}

function captureInitialRules() {
  initialRuleForm.value = Object.fromEntries(
    resourceColumns.value.map((column) => [column.id, resolveColumnAccess(column.id)]),
  );
}

function resetRules() {
  for (const key of Object.keys(ruleForm)) {
    delete ruleForm[Number(key)];
  }
  for (const column of resourceColumns.value) {
    ruleForm[column.id] = 'VISIBLE';
  }
}

function handleMenuNodeSelect(node: NavigationMenuTreeNode) {
  selectedMenuNode.value = node;
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
  window.addEventListener('mousemove', handleWorkbenchResize);
  window.addEventListener('mouseup', stopWorkbenchResize);
}

function handleWorkbenchResize(event: MouseEvent) {
  if (!workbenchResizeState.active || !workbenchRef.value) {
    return;
  }
  const containerHeight = workbenchRef.value.clientHeight;
  const dividerHeight = 16;
  const minEditorHeight = 160;
  const minPreviewHeight = 260;
  const nextHeight = workbenchResizeState.startHeight + (event.clientY - workbenchResizeState.startY);
  const maxEditorHeight = Math.max(minEditorHeight, containerHeight - minPreviewHeight - dividerHeight);
  editorPanelHeight.value = Math.min(Math.max(nextHeight, minEditorHeight), maxEditorHeight);
}

function stopWorkbenchResize() {
  if (!workbenchResizeState.active) {
    window.removeEventListener('mousemove', handleWorkbenchResize);
    window.removeEventListener('mouseup', stopWorkbenchResize);
    return;
  }
  workbenchResizeState.active = false;
  lastExpandedEditorHeight.value = editorPanelHeight.value;
  window.removeEventListener('mousemove', handleWorkbenchResize);
  window.removeEventListener('mouseup', stopWorkbenchResize);
}

function openCreate() {
  editingTemplate.value = null;
  Object.assign(form, {
    tenantId: 0,
    code: '',
    name: '',
    description: '',
    enabled: true,
  });
  templateDialogVisible.value = true;
}

function openEdit(row: IamColumnPermissionTemplate) {
  editingTemplate.value = row;
  Object.assign(form, {
    tenantId: row.tenantId,
    code: row.code,
    name: row.name,
    description: row.description || '',
    enabled: row.enabled,
  });
  templateDialogVisible.value = true;
}

async function submitTemplate(value: Record<string, unknown>) {
  Object.assign(form, value);
  const payload = normalizeTemplatePayload(form);
  const shouldReturnToDetail = viewMode.value === 'detail' && selectedTemplateId.value === editingTemplate.value?.id;
  submitting.value = true;
  try {
    const savedTemplate = editingTemplate.value
      ? await updateIamColumnPermissionTemplate(editingTemplate.value.id, payload)
      : await createIamColumnPermissionTemplate(payload);
    templateDialogVisible.value = false;
    ElMessage.success('列权限模板已保存');
    await loadPage(shouldReturnToDetail ? savedTemplate.id : null, activeLeafMenuCode.value);
    await refreshCurrentAuthorizationAfterTemplateChange();
  } finally {
    submitting.value = false;
  }
}

async function toggleTemplate(row: IamColumnPermissionTemplate) {
  await setIamColumnPermissionTemplateEnabled(row.id, !row.enabled, authStore.username);
  ElMessage.success(row.enabled ? '模板已停用' : '模板已启用');
  await loadPage(viewMode.value === 'detail' ? row.id : null, activeLeafMenuCode.value);
  await refreshCurrentAuthorizationAfterTemplateChange();
}

function countColumnsByMenuCodes(menuCodes: string[]) {
  const menuCodeSet = new Set(menuCodes);
  return columnPermissionPageConfigs.reduce((sum, pageConfig) => {
    if (!menuCodeSet.has(pageConfig.menuCode)) {
      return sum;
    }
    if (pageConfig.previewSchemaKey === 'tenant-management') {
      return sum + resolvePageSchemaColumns(pageConfig).length;
    }
    return sum + resourceColumns.value.filter((column) => pageConfig.resourceKeys.includes(column.resourceKey)).length;
  }, 0);
}

function resolveColumnAccess(columnId: number): AccessMode {
  return ruleForm[columnId] || 'VISIBLE';
}

function isColumnVisible(columnId: number) {
  return resolveColumnAccess(columnId) !== 'HIDDEN';
}

function resolvePreferredVisibleMode(column: IamResourceColumn): AccessMode {
  const initial = initialRuleForm.value[column.id];
  if (initial && initial !== 'HIDDEN') {
    return initial;
  }
  return column.maskType ? 'MASKED' : 'VISIBLE';
}

function toggleColumnVisibility(column: IamResourceColumn, checked: boolean | string | number) {
  ruleForm[column.id] = checked ? resolvePreferredVisibleMode(column) : 'HIDDEN';
}

function applyVisibleToCurrentPage() {
  for (const column of selectedPageColumns.value) {
    ruleForm[column.id] = resolvePreferredVisibleMode(column);
  }
}

function hideSensitiveColumns() {
  for (const column of selectedPageColumns.value) {
    if (column.maskType) {
      ruleForm[column.id] = 'HIDDEN';
    }
  }
}

function resetPageRules() {
  for (const column of selectedPageColumns.value) {
    ruleForm[column.id] = initialRuleForm.value[column.id] || 'VISIBLE';
  }
}

async function saveTemplateRules() {
  if (!activeTemplate.value) {
    return;
  }
  submitting.value = true;
  try {
    await setIamColumnPermissionTemplateItems(
      activeTemplate.value.id,
      resourceColumns.value.map((column) => ({
        resourceColumnId: column.id,
        accessMode: resolveColumnAccess(column.id),
      })),
      authStore.username,
    );
    captureInitialRules();
    ElMessage.success('字段规则已保存');
    await refreshCurrentAuthorizationAfterTemplateChange();
  } finally {
    submitting.value = false;
  }
}

async function refreshCurrentAuthorizationAfterTemplateChange() {
  if (Number(authStore.tenantId || 0) > 0) {
    await authorizationStore.refreshCurrentAuthorizationContext();
  }
}

function resolveColumnLocation(column: IamResourceColumn) {
  return selectedPageSchemaColumns.value.find((item) => (
    item.resourceKey === column.resourceKey && item.columnKey === column.columnKey
  ))?.location === 'detail' ? '详情字段' : '列表列';
}

function resolveCurrentAccessLabel(column: IamResourceColumn) {
  const accessMode = resolveColumnAccess(column.id);
  if (accessMode === 'MASKED') {
    return '脱敏显示';
  }
  if (accessMode === 'HIDDEN') {
    return '已隐藏';
  }
  return '明文显示';
}

function resolveSensitivityLabel(column: IamResourceColumn) {
  if (!column.maskType) {
    return '普通';
  }
  if (column.maskType === 'PHONE' || column.maskType === 'EMAIL') {
    return '隐私';
  }
  return '业务敏感';
}

function resolveSensitivityClass(column: IamResourceColumn) {
  return column.maskType ? 'warn' : 'normal';
}

function formatTemplateTenant(template: IamColumnPermissionTemplate) {
  return template.tenantId === 0 ? '平台模板' : `租户 ${template.tenantId}`;
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
    previewRows.value = page.records as unknown as PreviewRow[];
    previewTotal.value = page.total;
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
    const users = await listIamUsers(resolvePreviewTenantId());
    const start = (previewCurrentPage.value - 1) * previewPageSize.value;
    previewRows.value = users.slice(start, start + previewPageSize.value) as unknown as PreviewRow[];
    previewTotal.value = users.length;
  } catch {
    previewRows.value = [];
    previewTotal.value = 0;
  } finally {
    previewLoading.value = false;
  }
}

function resolvePreviewTenantId() {
  const templateTenantId = activeTemplate.value?.tenantId ?? 0;
  if (templateTenantId > 0) {
    return templateTenantId;
  }
  return Number(authStore.tenantId ?? tenantId.value ?? 0);
}

function normalizeTemplatePayload(value: IamColumnPermissionTemplatePayload): IamColumnPermissionTemplatePayload {
  return {
    tenantId: resolveTemplateTenantIdForSubmit(value),
    code: textValue(value.code),
    name: textValue(value.name),
    description: normalizeOptionalText(value.description),
    enabled: Boolean(value.enabled),
    operator: authStore.username,
  };
}

function resolveTemplateTenantIdForSubmit(value: IamColumnPermissionTemplatePayload) {
  if (editingTemplate.value) {
    return editingTemplate.value.tenantId;
  }
  return Number(value.tenantId || 0);
}

function normalizeOptionalText(value: unknown) {
  const trimmed = textValue(value);
  return trimmed || null;
}

function textValue(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}
</script>

<style scoped>
.status-filter {
  width: 132px;
}

.column-template-list {
  min-width: 0;
  min-height: 0;
  height: 100%;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  background: var(--xuan-panel);
}

.column-template-list-head {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--xuan-border);
}

.column-template-list-head strong {
  color: var(--xuan-text);
  font-size: 16px;
  line-height: 1.35;
}

.column-template-list-head span {
  color: var(--xuan-muted);
  font-size: 12px;
  line-height: 1.5;
}

.column-workspace {
  width: 100%;
  min-height: 0;
  height: 100%;
  display: grid;
  grid-template-columns: var(--column-tree-width, 300px) minmax(0, 1fr);
  gap: 16px;
  overflow: hidden;
}

.column-tree-shell {
  min-width: 0;
  min-height: 0;
  position: relative;
}

.column-tree-shell :deep(.navigation-menu-tree) {
  width: 100%;
  min-height: 0;
}

.column-tree-collapse {
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

.column-tree-shell.collapsed .column-tree-collapse {
  position: static;
  margin: 10px auto 6px;
}

.column-tree-collapsed {
  height: 100%;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--xuan-panel);
  color: var(--xuan-muted);
  font-size: 12px;
  writing-mode: vertical-rl;
  text-orientation: mixed;
}

.column-workbench {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.column-editor-panel,
.column-preview-panel {
  min-width: 0;
  min-height: 0;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  background: var(--xuan-panel);
  overflow: hidden;
}

.column-editor-panel {
  margin-bottom: 0;
}

.column-editor-head,
.column-preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--xuan-border);
}

.column-editor-head-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.column-editor-head-main strong {
  color: var(--xuan-text);
  font-size: 18px;
  line-height: 1.3;
}

.column-editor-head-desc,
.column-preview-head span,
.preview-note,
.editor-foot,
.column-empty-state span,
.preview-empty-state span {
  color: var(--xuan-muted);
  font-size: 12px;
  line-height: 1.6;
}

.column-editor-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mini-badge {
  padding: 4px 9px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 11px;
  line-height: 1;
  white-space: nowrap;
}

.mini-badge.primary {
  background: #eef6ff;
  color: #1d4ed8;
}

.column-editor-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
  padding-top: 6px;
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

.column-panel-toggle {
  width: 32px;
  min-width: 32px;
  height: 32px;
  border: 1px solid var(--xuan-border);
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: #64748b;
  cursor: pointer;
}

.preview-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.preview-summary-head {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.preview-summary-head strong {
  color: var(--xuan-text);
  font-size: 18px;
  line-height: 1.3;
}

.preview-summary-subline {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preview-summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.preview-summary-chip,
.preview-source-badge {
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 11px;
  line-height: 1.2;
  white-space: nowrap;
}

.preview-summary-chip {
  color: #64748b;
  background: #f1f5f9;
}

.preview-summary-chip.warn {
  color: #92400e;
  background: #fef3c7;
}

.preview-summary-chip.success {
  color: #166534;
  background: #dcfce7;
}

.preview-source-badge {
  color: #1d4ed8;
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

.field-grid {
  flex: 1 1 0;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 12px;
  overflow: auto;
}

.field-card {
  min-height: 72px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
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
  gap: 2px;
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

.field-side {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.pill {
  min-width: 46px;
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 11px;
  line-height: 1;
  text-align: center;
}

.pill.normal {
  color: #64748b;
  background: #e2e8f0;
}

.pill.warn {
  color: #92400e;
  background: #fef3c7;
}

.editor-foot {
  padding: 0 16px 14px;
}

.panel-collapsed-tip {
  min-height: 0;
  padding: 12px 16px;
  color: var(--xuan-muted);
  font-size: 12px;
  line-height: 1.5;
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

.column-empty-state strong,
.column-preview-head strong,
.preview-detail-title {
  color: var(--xuan-text);
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

.preview-page :deep(.el-table__inner-wrapper),
.preview-page :deep(.el-table__body-wrapper),
.preview-page :deep(.el-scrollbar) {
  min-height: 0;
}

.preview-page :deep(.el-scrollbar__wrap) {
  overflow: auto;
}

@media (max-width: 1380px) {
  .field-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .column-workspace {
    height: auto;
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .column-tree-shell :deep(.navigation-menu-tree) {
    max-height: 360px;
  }
}

@media (max-width: 760px) {
  .status-filter {
    width: 100%;
  }

  .column-editor-head,
  .column-preview-head {
    flex-direction: column;
    align-items: stretch;
  }

  .preview-head-actions {
    justify-content: flex-start;
  }

  .preview-summary-subline {
    flex-direction: column;
    align-items: flex-start;
  }

  .preview-summary-meta {
    justify-content: flex-start;
  }

  .column-editor-actions {
    justify-content: flex-start;
    padding-top: 0;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .preview-page :deep(.el-table) {
    min-width: 860px;
  }
}
</style>
