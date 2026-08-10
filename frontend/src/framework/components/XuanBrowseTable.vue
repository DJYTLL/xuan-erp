<template>
  <DataTableShell>
    <template v-if="hasToolbarContent" #toolbar>
      <div class="browse-table-toolbar">
        <div class="browse-table-toolbar-left">
          <span v-if="selectedCount" class="table-selected-count">已选 {{ selectedCount }} 项</span>
          <slot name="toolbar-left" />
        </div>
        <div class="browse-table-toolbar-actions">
          <template v-for="action in toolbarActions" :key="action.key">
            <PermissionButton
              v-if="action.permission"
              :type="normalizePermissionButtonType(action.type)"
              :text="action.text"
              :plain="action.plain"
              :permission="action.permission"
              :no-permission-mode="action.noPermissionMode || 'hide'"
              :state-resource="action.stateResource"
              :state-code="action.stateCode"
              :state-action="action.stateAction"
              :state-no-permission-reason="action.stateNoPermissionReason || '当前状态不可执行该动作'"
              :disabled="Boolean(action.disabled)"
              :disabled-reason="action.disabledReason || ''"
              @click="emitToolbarAction(action)"
            >
              {{ action.label }}
            </PermissionButton>
            <el-button
              v-else
              :type="normalizeElementButtonType(action.type)"
              :text="action.text"
              :plain="action.plain"
              :disabled="Boolean(action.disabled)"
              @click="emitToolbarAction(action)"
            >
              {{ action.label }}
            </el-button>
          </template>

          <el-select v-if="showDensityControl" v-model="densityModel" class="table-density-select" size="small" placeholder="密度">
            <el-option label="默认" value="default" />
            <el-option label="紧凑" value="small" />
            <el-option label="宽松" value="large" />
          </el-select>

          <el-popover
            v-if="showColumnSetting"
            trigger="click"
            placement="bottom-end"
            width="460"
            popper-class="browse-column-popover"
          >
            <template #reference>
              <el-button text :icon="SlidersHorizontal">列设置</el-button>
            </template>
            <div class="browse-column-panel">
              <header class="browse-column-panel-head">
                <strong>个人列设置</strong>
                <el-button link type="primary" @click="restoreDefaultPreference">恢复默认</el-button>
              </header>
              <div class="browse-column-list">
                <div v-for="column in columnPreferenceRows" :key="column.key" class="browse-column-row">
                  <div class="browse-column-row-main">
                    <el-checkbox
                      :model-value="column.preferenceVisible"
                      :disabled="!column.canToggleVisibility"
                      @change="toggleColumn(column.key, Boolean($event))"
                    >
                      {{ column.title }}
                    </el-checkbox>
                    <div class="browse-column-row-meta">
                      <span class="browse-column-access" :class="`browse-column-access--${column.accessMode.toLowerCase()}`">
                        {{ resolveAccessLabel(column.accessMode) }}
                      </span>
                      <span v-if="column.permissionHint">{{ column.permissionHint }}</span>
                    </div>
                  </div>
                  <div class="browse-column-row-actions">
                    <el-button link :disabled="!column.canAdjustLayout || column.order === 1" @click="move(column.key, -1)">上移</el-button>
                    <el-button
                      link
                      :disabled="!column.canAdjustLayout || column.order === columnPreferenceRows.length"
                      @click="move(column.key, 1)"
                    >
                      下移
                    </el-button>
                    <el-select
                      :model-value="column.fixed || ''"
                      size="small"
                      class="browse-fixed-select"
                      placeholder="请选择"
                      :disabled="!column.canAdjustLayout"
                      @change="setFixed(column.key, String($event))"
                    >
                      <el-option label="不固定" value="" />
                      <el-option label="左固定" value="left" />
                      <el-option label="右固定" value="right" />
                    </el-select>
                  </div>
                </div>
              </div>
            </div>
          </el-popover>

          <slot name="toolbar-actions" />
        </div>
      </div>
    </template>

    <el-table
      :data="data"
      :size="densityModel"
      :height="height"
      border
      @selection-change="handleSelectionChange"
      @header-dragend="handleHeaderDragend"
    >
      <template #empty>
        <slot name="empty">
          <AppState type="empty" :title="emptyState.title || '暂无数据'" :description="emptyState.description || '当前筛选条件下没有数据。'" />
        </slot>
      </template>

      <el-table-column v-if="selectableEnabled" type="selection" width="46" fixed="left" />

      <el-table-column
        v-for="column in displayedColumns"
        :key="column.key"
        :column-key="column.key"
        :prop="column.prop || column.key"
        :label="column.title"
        :width="column.width"
        :min-width="column.minWidth"
        :fixed="column.fixed || false"
        :align="column.align"
        show-overflow-tooltip
        resizable
      >
        <template #default="scope">
          <slot :name="`cell-${column.key}`" v-bind="{ ...scope, column }">
            <el-tag
              v-if="shouldRenderTag(column)"
              size="small"
              :type="resolveTagType(scope.row, column)"
            >
              {{ resolveCellText(scope.row, column) }}
            </el-tag>
            <span v-else>{{ resolveCellText(scope.row, column) }}</span>
          </slot>
        </template>
      </el-table-column>

      <el-table-column
        v-if="hasActionsColumn"
        label="操作"
        fixed="right"
        :width="actionsColumnWidth"
      >
        <template #default="scope">
          <slot name="actions" v-bind="scope">
            <template v-for="action in resolveVisibleRowActions(scope.row)" :key="action.key">
              <PermissionButton
                v-if="action.permission"
                :type="normalizePermissionButtonType(action.type)"
                :link="action.link !== false"
                :text="action.text"
                :plain="action.plain"
                :permission="action.permission"
                :no-permission-mode="action.noPermissionMode || 'hide'"
                :state-resource="action.stateResource"
                :state-code="resolveRowActionStateCode(action, scope.row)"
                :state-action="action.stateAction"
                :state-no-permission-reason="action.stateNoPermissionReason || '当前状态不可执行该动作'"
                :disabled="resolveRowActionDisabled(action, scope.row)"
                :disabled-reason="resolveRowActionDisabledReason(action, scope.row)"
                @click="emitRowAction(action, scope.row)"
              >
                {{ action.label }}
              </PermissionButton>
              <el-button
                v-else
                :type="normalizeElementButtonType(action.type)"
                :link="action.link !== false"
                :text="action.text"
                :plain="action.plain"
                :disabled="resolveRowActionDisabled(action, scope.row)"
                @click="emitRowAction(action, scope.row)"
              >
                {{ action.label }}
              </el-button>
            </template>
          </slot>
        </template>
      </el-table-column>
    </el-table>

    <template v-if="showPagination" #pagination>
      <span>共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="currentPageModel"
        v-model:page-size="pageSizeModel"
        layout="sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="normalizedPageSizes"
      />
    </template>
  </DataTableShell>
</template>

<script setup lang="ts" generic="TRow extends object">
import { computed, onMounted, ref, useSlots, watch } from 'vue';
import { SlidersHorizontal } from 'lucide-vue-next';
import AppState from '@/framework/components/AppState.vue';
import DataTableShell from '@/framework/components/DataTableShell.vue';
import PermissionButton from '@/framework/components/PermissionButton.vue';
import {
  createDefaultBrowseTablePreference,
  getBrowseTablePreferenceKey,
  mergeBrowseTablePreference,
  moveColumn,
  resizeColumn,
  toggleColumnVisibility,
  type BrowseTableColumnInput,
  type BrowseTableColumnSetting,
  type BrowseTableDensity,
  type BrowseTablePreference,
} from '@/framework/components/browseTablePreferences';
import {
  isBrowseTableColumnUserToggleAllowed,
  resolveBrowseTableCellText,
  resolveBrowseTableTagType,
  resolveColumnAccessMode,
  type BrowseTableButtonType,
  type BrowseTableColumnAccessMode,
  type BrowseTableColumnPermissionSnapshot,
  type XuanBrowseTableColumnSchema,
  type XuanBrowseTableRowActionSchema,
  type XuanBrowseTableSchema,
  type XuanBrowseTableToolbarActionSchema,
} from '@/framework/components/browseTableSchema';
import { useFrameworkPreferenceAdapter } from '@/framework/preferences/preferenceAdapter';

export type XuanBrowseTableColumn<TRow extends object = Record<string, unknown>> = XuanBrowseTableColumnSchema<TRow>;

defineOptions({ name: 'XuanBrowseTable' });

const slots = useSlots();

const props = withDefaults(defineProps<{
  data: TRow[];
  schema?: XuanBrowseTableSchema<TRow>;
  columnPermissionSnapshot?: BrowseTableColumnPermissionSnapshot | null;
  strictColumnPermissionSnapshot?: boolean;
  columns?: Array<XuanBrowseTableColumn<TRow>>;
  total: number;
  pageCode?: string;
  tableCode?: string;
  tenantId?: string;
  userId?: string;
  height?: string | number;
  selectable?: boolean;
  selectedCount?: number;
  actionsWidth?: number;
  pageSizes?: number[];
  currentPage: number;
  pageSize: number;
  density: BrowseTableDensity;
}>(), {
  schema: undefined,
  columnPermissionSnapshot: null,
  strictColumnPermissionSnapshot: false,
  columns: () => [],
  pageCode: '',
  tableCode: '',
  tenantId: '0',
  userId: 'anonymous',
  height: 520,
  selectable: true,
  selectedCount: 0,
  actionsWidth: 180,
  pageSizes: () => [10, 20, 50],
});

const emit = defineEmits<{
  'update:currentPage': [value: number];
  'update:pageSize': [value: number];
  'update:density': [value: BrowseTableDensity];
  'selection-change': [rows: TRow[]];
  'preference-change': [preference: BrowseTablePreference];
  'toolbar-action': [payload: { actionKey: string; action: XuanBrowseTableToolbarActionSchema }];
  'row-action': [payload: { actionKey: string; action: XuanBrowseTableRowActionSchema<TRow>; row: TRow }];
}>();

type RuntimeColumn = XuanBrowseTableColumnSchema<TRow> & BrowseTableColumnSetting & {
  accessMode: BrowseTableColumnAccessMode;
  preferenceVisible: boolean;
  canToggleVisibility: boolean;
  canAdjustLayout: boolean;
  effectiveVisible: boolean;
  permissionHint: string;
};

const normalizedSchema = computed<XuanBrowseTableSchema<TRow>>(() => {
  if (props.schema) {
    return {
      ...props.schema,
      selectable: props.schema.selectable ?? props.selectable,
      actionsWidth: props.schema.actionsWidth ?? props.actionsWidth,
      defaultDensity: props.schema.defaultDensity ?? props.density,
      defaultPageSize: props.schema.defaultPageSize ?? props.pageSize,
      toolbar: {
        showDensity: props.schema.toolbar?.showDensity ?? true,
        showColumnSetting: props.schema.toolbar?.showColumnSetting ?? true,
        actions: props.schema.toolbar?.actions ?? [],
      },
      rowActions: props.schema.rowActions ?? [],
      pagination: {
        show: props.schema.pagination?.show ?? true,
        pageSizes: props.schema.pagination?.pageSizes ?? props.pageSizes,
      },
      emptyState: {
        title: props.schema.emptyState?.title || '暂无数据',
        description: props.schema.emptyState?.description || '当前筛选条件下没有数据。',
      },
    };
  }

  return {
    pageCode: props.pageCode || 'browse-table',
    tableCode: props.tableCode || 'default',
    selectable: props.selectable,
    actionsWidth: props.actionsWidth,
    defaultDensity: props.density,
    defaultPageSize: props.pageSize,
    toolbar: {
      showDensity: true,
      showColumnSetting: true,
      actions: [],
    },
    columns: props.columns.map((column) => ({
      ...column,
      visibleByDefault: column.visibleByDefault ?? true,
      displayType: column.displayType || 'text',
    })),
    rowActions: [],
    pagination: {
      show: true,
      pageSizes: props.pageSizes,
    },
    emptyState: {
      title: '暂无数据',
      description: '当前筛选条件下没有数据。',
    },
  };
});

const schemaColumnInputs = computed<BrowseTableColumnInput[]>(() => normalizedSchema.value.columns.map((column) => ({
  key: column.key,
  title: column.title,
  width: column.width,
  fixed: column.fixed,
  visible: column.visibleByDefault ?? true,
})));

const defaultPreference = computed(() => createDefaultBrowseTablePreference({
  tenantId: props.tenantId,
  userId: props.userId,
  pageCode: normalizedSchema.value.pageCode,
  tableCode: normalizedSchema.value.tableCode,
  columns: schemaColumnInputs.value,
  pageSize: normalizedSchema.value.defaultPageSize || props.pageSize,
  density: normalizedSchema.value.defaultDensity || props.density,
}));

const preference = ref<BrowseTablePreference>(defaultPreference.value);
let preferenceLoadVersion = 0;
const preferenceAdapter = useFrameworkPreferenceAdapter();

const storageKey = computed(() => getBrowseTablePreferenceKey({
  tenantId: props.tenantId,
  userId: props.userId,
  pageCode: normalizedSchema.value.pageCode,
  tableCode: normalizedSchema.value.tableCode,
}));

const currentPageModel = computed({
  get: () => props.currentPage,
  set: (value: number) => emit('update:currentPage', value),
});

const pageSizeModel = computed({
  get: () => props.pageSize,
  set: (value: number) => {
    emit('update:pageSize', value);
    savePreference({ ...preference.value, pageSize: value });
  },
});

const densityModel = computed({
  get: () => props.density,
  set: (value: BrowseTableDensity) => {
    emit('update:density', value);
    savePreference({ ...preference.value, density: value });
  },
});

const toolbarActions = computed(() => normalizedSchema.value.toolbar?.actions ?? []);
const showDensityControl = computed(() => normalizedSchema.value.toolbar?.showDensity !== false);
const showColumnSetting = computed(() => normalizedSchema.value.toolbar?.showColumnSetting !== false);
const hasToolbarContent = computed(() => (
  showDensityControl.value
  || showColumnSetting.value
  || toolbarActions.value.length > 0
  || Boolean(slots['toolbar-left'])
  || Boolean(slots['toolbar-actions'])
));
const showPagination = computed(() => normalizedSchema.value.pagination?.show !== false);
const selectableEnabled = computed(() => normalizedSchema.value.selectable !== false);
const actionsColumnWidth = computed(() => normalizedSchema.value.actionsWidth ?? props.actionsWidth);
const normalizedPageSizes = computed(() => normalizedSchema.value.pagination?.pageSizes ?? props.pageSizes);
const emptyState = computed(() => normalizedSchema.value.emptyState || {
  title: '暂无数据',
  description: '当前筛选条件下没有数据。',
});
const schemaColumnMap = computed(() => new Map(normalizedSchema.value.columns.map((column) => [column.key, column])));
const hasSchemaRowActions = computed(() => (normalizedSchema.value.rowActions?.length ?? 0) > 0);
const hasActionsColumn = computed(() => hasSchemaRowActions.value || Boolean(slots.actions));

const columnPreferenceRows = computed<RuntimeColumn[]>(() => preference.value.columns
  .map((setting) => {
    const source = schemaColumnMap.value.get(setting.key);
    if (!source) {
      return null;
    }
    const accessMode = resolveColumnAccessMode(source, props.columnPermissionSnapshot, props.strictColumnPermissionSnapshot);
    const canToggleVisibility = isBrowseTableColumnUserToggleAllowed(source, accessMode);
    const effectiveVisible = accessMode !== 'HIDDEN' && setting.visible;
    return {
      ...source,
      ...setting,
      accessMode,
      preferenceVisible: setting.visible,
      canToggleVisibility,
      canAdjustLayout: accessMode !== 'HIDDEN',
      effectiveVisible,
      permissionHint: resolvePermissionHint(accessMode),
    };
  })
  .filter((column): column is RuntimeColumn => Boolean(column)));

const displayedColumns = computed<RuntimeColumn[]>(() => columnPreferenceRows.value.filter((column) => column.effectiveVisible));

watch(
  () => [normalizedSchema.value.columns, normalizedSchema.value.pageCode, normalizedSchema.value.tableCode, storageKey.value],
  () => {
    void loadPreference();
  },
);

onMounted(() => {
  void loadPreference();
});

async function loadPreference() {
  const loadVersion = ++preferenceLoadVersion;
  const saved = await readPreference();
  if (loadVersion !== preferenceLoadVersion) {
    return;
  }
  const merged = mergeBrowseTablePreference(defaultPreference.value, saved);
  preference.value = merged;
  emit('update:pageSize', merged.pageSize);
  emit('update:density', merged.density);
  emit('preference-change', merged);
}

function savePreference(nextPreference: BrowseTablePreference) {
  preference.value = nextPreference;
  localStorage.setItem(storageKey.value, JSON.stringify(nextPreference));
  preferenceAdapter.savePreference(backendPreferenceKey(), nextPreference).catch(() => {
    // 后端偏好同步失败时继续保留 localStorage，避免表格操作中断。
  });
  emit('preference-change', nextPreference);
}

async function readPreference() {
  const rawValue = localStorage.getItem(storageKey.value);
  let localPreference: BrowseTablePreference | null = null;
  if (!rawValue) {
    localPreference = null;
  } else {
    try {
      localPreference = JSON.parse(rawValue) as BrowseTablePreference;
    } catch {
      localPreference = null;
    }
  }

  try {
    const remotePreference = await preferenceAdapter.getPreference<BrowseTablePreference>(backendPreferenceKey());
    if (remotePreference) {
      localStorage.setItem(storageKey.value, JSON.stringify(remotePreference));
      return remotePreference;
    }
  } catch {
    return localPreference;
  }
  return localPreference;
}

function restoreDefaultPreference() {
  localStorage.removeItem(storageKey.value);
  savePreference(defaultPreference.value);
  emit('update:pageSize', defaultPreference.value.pageSize);
  emit('update:density', defaultPreference.value.density);
}

function backendPreferenceKey() {
  return `table.${normalizedSchema.value.pageCode}.${normalizedSchema.value.tableCode}`;
}

function toggleColumn(columnKey: string, visible: boolean) {
  const column = columnPreferenceRows.value.find((item) => item.key === columnKey);
  if (!column?.canToggleVisibility) {
    return;
  }
  savePreference(toggleColumnVisibility(preference.value, columnKey, visible));
}

function move(columnKey: string, direction: -1 | 1) {
  const column = columnPreferenceRows.value.find((item) => item.key === columnKey);
  if (!column?.canAdjustLayout) {
    return;
  }
  savePreference(moveColumn(preference.value, columnKey, direction));
}

function setFixed(columnKey: string, fixed: string) {
  const column = columnPreferenceRows.value.find((item) => item.key === columnKey);
  if (!column?.canAdjustLayout) {
    return;
  }
  savePreference({
    ...preference.value,
    columns: preference.value.columns.map((item) => {
      if (item.key !== columnKey) {
        return item;
      }
      return {
        ...item,
        fixed: fixed === 'left' || fixed === 'right' ? fixed : null,
      };
    }),
  });
}

function handleSelectionChange(rows: TRow[]) {
  emit('selection-change', rows);
}

function handleHeaderDragend(newWidth: number, _oldWidth: number, column: { columnKey?: string; property?: string }) {
  const columnKey = column.columnKey || column.property;
  if (!columnKey) {
    return;
  }
  const runtimeColumn = columnPreferenceRows.value.find((item) => item.key === columnKey);
  if (!runtimeColumn?.canAdjustLayout) {
    return;
  }
  savePreference(resizeColumn(preference.value, columnKey, newWidth));
}

function resolveAccessLabel(accessMode: BrowseTableColumnAccessMode) {
  if (accessMode === 'MASKED') {
    return '脱敏';
  }
  if (accessMode === 'HIDDEN') {
    return '隐藏';
  }
  return '明文';
}

function resolvePermissionHint(accessMode: BrowseTableColumnAccessMode) {
  if (accessMode === 'MASKED') {
    return '当前列受列权限约束，按脱敏方式展示';
  }
  if (accessMode === 'HIDDEN') {
    return '当前列已被列权限隐藏';
  }
  return '';
}

function normalizePermissionButtonType(type?: BrowseTableButtonType) {
  return type === 'default' || !type ? '' : type;
}

function normalizeElementButtonType(type?: BrowseTableButtonType) {
  return type === 'default' ? undefined : type;
}

function emitToolbarAction(action: XuanBrowseTableToolbarActionSchema) {
  if (action.disabled) {
    return;
  }
  emit('toolbar-action', { actionKey: action.key, action });
}

function emitRowAction(action: XuanBrowseTableRowActionSchema<TRow>, row: TRow) {
  if (resolveRowActionDisabled(action, row)) {
    return;
  }
  emit('row-action', { actionKey: action.key, action, row });
}

function resolveVisibleRowActions(row: TRow) {
  return (normalizedSchema.value.rowActions ?? []).filter((action) => {
    if (typeof action.visible === 'function') {
      return action.visible(row);
    }
    if (typeof action.visible === 'boolean') {
      return action.visible;
    }
    return true;
  });
}

function resolveRowActionDisabled(action: XuanBrowseTableRowActionSchema<TRow>, row: TRow) {
  if (typeof action.disabled === 'function') {
    return action.disabled(row);
  }
  return Boolean(action.disabled);
}

function resolveRowActionDisabledReason(action: XuanBrowseTableRowActionSchema<TRow>, row: TRow) {
  if (typeof action.disabledReason === 'function') {
    return action.disabledReason(row);
  }
  return action.disabledReason || '';
}

function resolveRowActionStateCode(action: XuanBrowseTableRowActionSchema<TRow>, row: TRow) {
  if (typeof action.stateCode === 'function') {
    return action.stateCode(row);
  }
  return action.stateCode;
}

function shouldRenderTag(column: RuntimeColumn) {
  return column.displayType === 'tag' && column.accessMode === 'VISIBLE';
}

function resolveCellText(row: TRow, column: RuntimeColumn) {
  return resolveBrowseTableCellText(row, column, column.accessMode);
}

function resolveTagType(row: TRow, column: RuntimeColumn) {
  const value = resolveBrowseTableCellText(row, column, 'VISIBLE');
  return resolveBrowseTableTagType(row, column, value);
}
</script>
