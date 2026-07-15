<template>
  <DataTableShell>
    <template #toolbar>
      <div class="browse-table-toolbar">
        <div class="browse-table-toolbar-left">
          <span v-if="selectedCount" class="table-selected-count">已选 {{ selectedCount }} 项</span>
          <slot name="toolbar-left" />
        </div>
        <div class="browse-table-toolbar-actions">
          <el-select v-model="densityModel" class="table-density-select" size="small" placeholder="密度">
            <el-option label="默认" value="default" />
            <el-option label="紧凑" value="small" />
            <el-option label="宽松" value="large" />
          </el-select>
          <el-popover
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
                <div v-for="column in preference.columns" :key="column.key" class="browse-column-row">
                  <el-checkbox
                    :model-value="column.visible"
                    @change="toggleColumn(column.key, Boolean($event))"
                  >
                    {{ column.title }}
                  </el-checkbox>
                  <div class="browse-column-row-actions">
                    <el-button link :disabled="column.order === 1" @click="move(column.key, -1)">上移</el-button>
                    <el-button
                      link
                      :disabled="column.order === preference.columns.length"
                      @click="move(column.key, 1)"
                    >
                      下移
                    </el-button>
                    <el-select
                      :model-value="column.fixed || ''"
                      size="small"
                      class="browse-fixed-select"
                      placeholder="请选择"
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
          <AppState type="empty" title="暂无数据" description="当前筛选条件下没有数据。" />
        </slot>
      </template>

      <el-table-column v-if="selectable" type="selection" width="46" fixed="left" />

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
            {{ formatCellValue(scope.row, column) }}
          </slot>
        </template>
      </el-table-column>

      <el-table-column
        v-if="$slots.actions"
        label="操作"
        fixed="right"
        :width="actionsWidth"
      >
        <template #default="scope">
          <slot name="actions" v-bind="scope" />
        </template>
      </el-table-column>
    </el-table>

    <template #pagination>
      <span>共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="currentPageModel"
        v-model:page-size="pageSizeModel"
        layout="sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="pageSizes"
      />
    </template>
  </DataTableShell>
</template>

<script setup lang="ts" generic="TRow extends object">
import { computed, onMounted, ref, watch } from 'vue';
import { SlidersHorizontal } from 'lucide-vue-next';
import AppState from '@/framework/components/AppState.vue';
import DataTableShell from '@/framework/components/DataTableShell.vue';
import {
  createDefaultBrowseTablePreference,
  getBrowseTablePreferenceKey,
  mergeBrowseTablePreference,
  moveColumn,
  resizeColumn,
  toggleColumnVisibility,
  type BrowseTableColumnSetting,
  type BrowseTableDensity,
  type BrowseTablePreference,
} from '@/framework/components/browseTablePreferences';
import { useFrameworkPreferenceAdapter } from '@/framework/preferences/preferenceAdapter';

export type XuanBrowseTableColumn<TRow extends object = Record<string, unknown>> = {
  key: string;
  title: string;
  prop?: keyof TRow & string;
  width?: number;
  minWidth?: number;
  visible?: boolean;
  fixed?: 'left' | 'right' | null;
  align?: 'left' | 'center' | 'right';
  formatter?: (row: TRow) => string | number;
};

const props = withDefaults(defineProps<{
  data: TRow[];
  columns: Array<XuanBrowseTableColumn<TRow>>;
  total: number;
  pageCode: string;
  tableCode: string;
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
}>();

type RuntimeColumn = XuanBrowseTableColumn<TRow> & BrowseTableColumnSetting;

const defaultPreference = computed(() => createDefaultBrowseTablePreference({
  tenantId: props.tenantId,
  userId: props.userId,
  pageCode: props.pageCode,
  tableCode: props.tableCode,
  columns: props.columns,
  pageSize: props.pageSize,
  density: props.density,
}));

const preference = ref<BrowseTablePreference>(defaultPreference.value);
let preferenceLoadVersion = 0;
const preferenceAdapter = useFrameworkPreferenceAdapter();

const storageKey = computed(() => getBrowseTablePreferenceKey({
  tenantId: props.tenantId,
  userId: props.userId,
  pageCode: props.pageCode,
  tableCode: props.tableCode,
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

const displayedColumns = computed<RuntimeColumn[]>(() => {
  const columnMap = new Map(props.columns.map((column) => [column.key, column]));
  return preference.value.columns
    .filter((column) => column.visible)
    .map((setting) => {
      const source = columnMap.get(setting.key);
      return {
        ...source,
        ...setting,
        prop: source?.prop,
        minWidth: source?.minWidth,
        align: source?.align,
        formatter: source?.formatter,
      } as RuntimeColumn;
    });
});

watch(
  () => [props.columns, storageKey.value],
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
  return `table.${props.pageCode}.${props.tableCode}`;
}

function toggleColumn(columnKey: string, visible: boolean) {
  savePreference(toggleColumnVisibility(preference.value, columnKey, visible));
}

function move(columnKey: string, direction: -1 | 1) {
  savePreference(moveColumn(preference.value, columnKey, direction));
}

function setFixed(columnKey: string, fixed: string) {
  savePreference({
    ...preference.value,
    columns: preference.value.columns.map((column) => {
      if (column.key !== columnKey) {
        return column;
      }
      return {
        ...column,
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
  savePreference(resizeColumn(preference.value, columnKey, newWidth));
}

function formatCellValue(row: TRow, column: RuntimeColumn) {
  if (column.formatter) {
    return column.formatter(row);
  }
  const prop = column.prop || column.key;
  const value = row[prop as keyof TRow];
  return value === null || value === undefined || value === '' ? '-' : String(value);
}
</script>
