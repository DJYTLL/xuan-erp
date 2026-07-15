<template>
  <section class="menu-permission-assignment">
    <aside class="permission-tree-panel">
      <el-input
        v-model="searchKeyword"
        class="permission-assignment-search"
        placeholder="搜索菜单、权限名称或编码"
        clearable
      />

      <div class="permission-tree-list">
        <button
          v-for="node in visibleNodes"
          :key="node.key"
          class="permission-tree-row"
          :class="{ active: node.key === activeNode?.key }"
          :style="{ paddingLeft: `${12 + node.level * 18}px` }"
          type="button"
          @click="selectNode(node)"
        >
          <el-checkbox
            :model-value="isNodeChecked(node)"
            :indeterminate="isNodeIndeterminate(node)"
            :disabled="props.readonly"
            @click.stop
            @change="toggleNodePermissions(node)"
          />
          <button
            v-if="node.children.length"
            class="permission-expand-button"
            type="button"
            @click.stop="toggleExpanded(node.key)"
          >
            {{ isExpanded(node.key) ? '⌄' : '›' }}
          </button>
          <span v-else class="permission-leaf-dot">•</span>
          <span class="permission-node-title">{{ node.title }}</span>
          <span class="permission-node-count">{{ selectedCount(node.permissionCodes) }}/{{ node.permissionCodes.length }}</span>
        </button>
      </div>
    </aside>

    <main class="permission-detail-panel">
      <header class="permission-detail-head">
        <div class="permission-detail-title">
          <strong>{{ activeNode?.title || '权限分配' }}</strong>
          <span>{{ activeSelectedCount }}/{{ activePermissionItems.length }}</span>
        </div>
        <div class="permission-detail-actions">
          <el-tag type="success" effect="plain">全部: {{ selectedPermissionCount }}/{{ allPermissionCodes.length }}</el-tag>
          <el-tag v-if="props.readonly" type="info" effect="plain">只读</el-tag>
          <el-button v-if="!props.readonly" size="small" type="primary" plain @click="selectAllPermissions">全部权限全选</el-button>
          <el-button v-if="!props.readonly" size="small" type="danger" plain @click="clearAllPermissions">全部清空</el-button>
        </div>
      </header>

      <div v-if="activePermissionItems.length" class="permission-detail-list">
        <el-checkbox-group v-model="selectedCodes">
          <el-checkbox
            v-for="permission in activePermissionItems"
            :key="permission.code"
            :label="permission.code"
            :disabled="props.readonly"
            class="permission-detail-item"
          >
            <span class="permission-name">{{ permission.name }}</span>
            <el-tag
              v-if="permission.relatedToActiveMenu"
              class="permission-related-tag"
              size="small"
              type="warning"
              effect="plain"
            >
              页面依赖
            </el-tag>
            <code>{{ permission.code }}</code>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <AppState
        v-else
        type="empty"
        title="暂无权限项"
        description="当前菜单下还没有绑定权限。"
      />
    </main>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import AppState from '@/framework/components/AppState.vue';
import {
  createNavigationPermissionTree,
  type FrameworkMenuSourceNode,
  type NavigationPermissionNode,
} from '@/framework/navigation/menu';

type PermissionMenuSource = FrameworkMenuSourceNode & {
  id: number;
  parentId?: number | null;
  enabled?: boolean;
};

type PermissionAssignmentPermission = {
  code: string;
  name: string;
  menuCode?: string | null;
  enabled?: boolean;
};

type PermissionTreeNode = {
  key: string;
  code: string;
  title: string;
  level: number;
  menuCodes: string[];
  children: PermissionTreeNode[];
  directPermissions: PermissionAssignmentPermission[];
  relatedPermissions: PermissionAssignmentPermission[];
  permissionCodes: string[];
};

type PermissionAssignmentItem = PermissionAssignmentPermission & {
  relatedToActiveMenu: boolean;
};

const props = withDefaults(defineProps<{
  menus: PermissionMenuSource[];
  permissions: PermissionAssignmentPermission[];
  pageRequiredPermissionMap?: Record<string, string[]>;
  modelValue: string[];
  readonly?: boolean;
}>(), {
  pageRequiredPermissionMap: () => ({}),
  readonly: false,
});

const emit = defineEmits<{
  'update:modelValue': [value: string[]];
}>();

const { t } = useI18n();
const searchKeyword = ref('');
const expandedKeys = ref<Set<string>>(new Set());
const activeNodeKey = ref('');

const selectedCodes = computed({
  get: () => props.modelValue,
  set: (value: string[]) => emit('update:modelValue', dedupe(value)),
});

const selectedCodeSet = computed(() => new Set(selectedCodes.value));

const enabledPermissions = computed(() => props.permissions
  .filter((permission) => permission.enabled !== false)
  .sort((left, right) => left.code.localeCompare(right.code)));

const allPermissionCodes = computed(() => enabledPermissions.value.map((permission) => permission.code));

const menuTree = computed(() => buildPermissionTree(
  props.menus,
  enabledPermissions.value,
  props.pageRequiredPermissionMap,
  t,
));

const visibleNodes = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  return flattenTree(menuTree.value, keyword);
});

const activeNode = computed(() => {
  const nodes = flattenAllNodes(menuTree.value);
  return nodes.find((node) => node.key === activeNodeKey.value) || nodes.find((node) => node.permissionCodes.length) || null;
});

const activePermissionItems = computed<PermissionAssignmentItem[]>(() => {
  if (!activeNode.value) {
    return [];
  }
  const shouldShowDirectPanel = activeNode.value.directPermissions.length > 0
    || activeNode.value.relatedPermissions.length > 0;
  const relatedCodes = new Set(activeNode.value.relatedPermissions.map((permission) => permission.code));
  const activeCodes = shouldShowDirectPanel
    ? [
      ...activeNode.value.directPermissions.map((permission) => permission.code),
      ...activeNode.value.relatedPermissions.map((permission) => permission.code),
    ]
    : activeNode.value.permissionCodes;
  return permissionsByCodes(activeCodes).map((permission) => ({
    ...permission,
    relatedToActiveMenu: relatedCodes.has(permission.code),
  }));
});

const selectedPermissionCount = computed(() => selectedCount(allPermissionCodes.value));
const activeSelectedCount = computed(() => selectedCount(activePermissionItems.value.map((permission) => permission.code)));

watch(
  menuTree,
  (nodes) => {
    const allNodes = flattenAllNodes(nodes);
    expandedKeys.value = new Set(allNodes.filter((node) => node.children.length).map((node) => node.key));
    if (!allNodes.some((node) => node.key === activeNodeKey.value)) {
      activeNodeKey.value = allNodes.find((node) => node.permissionCodes.length)?.key || allNodes[0]?.key || '';
    }
  },
  { immediate: true },
);

function buildPermissionTree(
  menus: PermissionMenuSource[],
  permissions: PermissionAssignmentPermission[],
  pageRequiredPermissionMap: Record<string, string[]>,
  translator: (key: string) => string,
) {
  const directPermissionMap = groupPermissionsByMenuCode(permissions);
  const permissionByCode = new Map(permissions.map((permission) => [permission.code, permission]));
  const navigationRoots = createNavigationPermissionTree(toCurrentMenuNodes(menus), translator);
  const roots = navigationRoots.map((node) => buildNavigationPermissionNode(
    node,
    directPermissionMap,
    permissionByCode,
    pageRequiredPermissionMap,
  ));

  const orphanPermissions = directPermissionMap.get('__unclassified__') || [];
  if (orphanPermissions.length) {
    roots.push({
      key: 'menu:__unclassified__',
      code: '__unclassified__',
      title: '未分类权限',
      level: 0,
      menuCodes: ['__unclassified__'],
      children: [],
      directPermissions: orphanPermissions,
      relatedPermissions: [],
      permissionCodes: [],
    });
  }

  roots.forEach((node) => assignNodeState(node, 0));
  return roots.filter((node) => node.permissionCodes.length || node.children.length);
}

function buildNavigationPermissionNode(
  node: NavigationPermissionNode,
  directPermissionMap: Map<string, PermissionAssignmentPermission[]>,
  permissionByCode: Map<string, PermissionAssignmentPermission>,
  pageRequiredPermissionMap: Record<string, string[]>,
): PermissionTreeNode {
  const sourceCode = node.sourceCode || null;
  const directPermissions = sourceCode ? directPermissionMap.get(sourceCode) || [] : [];
  return {
    key: `nav:${node.key}`,
    code: node.key,
    title: node.title,
    level: 0,
    menuCodes: node.menuCodes,
    children: node.children.map((child) => buildNavigationPermissionNode(
      child,
      directPermissionMap,
      permissionByCode,
      pageRequiredPermissionMap,
    )),
    directPermissions,
    relatedPermissions: sourceCode
      ? resolvePageRequiredPermissions(sourceCode, directPermissions, permissionByCode, pageRequiredPermissionMap)
      : [],
    permissionCodes: [],
  };
}

function groupPermissionsByMenuCode(permissions: PermissionAssignmentPermission[]) {
  const grouped = new Map<string, PermissionAssignmentPermission[]>();
  for (const permission of permissions) {
    const menuCode = permission.menuCode || '__unclassified__';
    grouped.set(menuCode, [...(grouped.get(menuCode) || []), permission]);
  }
  return grouped;
}

function resolvePageRequiredPermissions(
  menuCode: string,
  directPermissions: PermissionAssignmentPermission[],
  permissionByCode: Map<string, PermissionAssignmentPermission>,
  pageRequiredPermissionMap: Record<string, string[]>,
) {
  const directCodes = new Set(directPermissions.map((permission) => permission.code));
  return dedupe(pageRequiredPermissionMap[menuCode] || [])
    .filter((code) => !directCodes.has(code))
    .map((code) => permissionByCode.get(code))
    .filter((permission): permission is PermissionAssignmentPermission => Boolean(permission));
}

function assignNodeState(node: PermissionTreeNode, level: number): string[] {
  node.level = level;
  const childCodes = node.children.flatMap((child) => assignNodeState(child, level + 1));
  node.permissionCodes = dedupe([
    ...node.directPermissions.map((permission) => permission.code),
    ...node.relatedPermissions.map((permission) => permission.code),
    ...childCodes,
  ]);
  return node.permissionCodes;
}

function flattenTree(nodes: PermissionTreeNode[], keyword: string): PermissionTreeNode[] {
  const result: PermissionTreeNode[] = [];
  for (const node of nodes) {
    if (keyword && !matchesNodeOrDescendant(node, keyword)) {
      continue;
    }
    result.push(node);
    if ((keyword || isExpanded(node.key)) && node.children.length) {
      result.push(...flattenTree(node.children, keyword));
    }
  }
  return result;
}

function flattenAllNodes(nodes: PermissionTreeNode[]): PermissionTreeNode[] {
  return nodes.flatMap((node) => [node, ...flattenAllNodes(node.children)]);
}

function matchesNodeOrDescendant(node: PermissionTreeNode, keyword: string): boolean {
  return matchesNode(node, keyword) || node.children.some((child) => matchesNodeOrDescendant(child, keyword));
}

function matchesNode(node: PermissionTreeNode, keyword: string) {
  return [
    node.title,
    node.code,
    ...node.directPermissions.flatMap((permission) => [permission.name, permission.code]),
    ...node.relatedPermissions.flatMap((permission) => [permission.name, permission.code]),
  ]
    .some((value) => value.toLowerCase().includes(keyword));
}

function permissionsByCodes(codes: string[]) {
  const activeCodes = new Set(codes);
  return enabledPermissions.value.filter((permission) => activeCodes.has(permission.code));
}

function selectNode(node: PermissionTreeNode) {
  activeNodeKey.value = node.key;
}

function isExpanded(key: string) {
  return expandedKeys.value.has(key);
}

function toggleExpanded(key: string) {
  const next = new Set(expandedKeys.value);
  if (next.has(key)) {
    next.delete(key);
  } else {
    next.add(key);
  }
  expandedKeys.value = next;
}

function isNodeChecked(node: PermissionTreeNode) {
  return node.permissionCodes.length > 0 && node.permissionCodes.every((code) => selectedCodeSet.value.has(code));
}

function isNodeIndeterminate(node: PermissionTreeNode) {
  const count = selectedCount(node.permissionCodes);
  return count > 0 && count < node.permissionCodes.length;
}

function selectedCount(codes: string[]) {
  return codes.filter((code) => selectedCodeSet.value.has(code)).length;
}

function toggleNodePermissions(node: PermissionTreeNode) {
  if (props.readonly) {
    return;
  }
  const current = new Set(selectedCodes.value);
  const shouldSelect = !isNodeChecked(node);
  for (const code of node.permissionCodes) {
    if (shouldSelect) {
      current.add(code);
    } else {
      current.delete(code);
    }
  }
  selectedCodes.value = [...current];
}

function selectAllPermissions() {
  if (props.readonly) {
    return;
  }
  selectedCodes.value = allPermissionCodes.value;
}

function clearAllPermissions() {
  if (props.readonly) {
    return;
  }
  selectedCodes.value = [];
}

function dedupe(value: string[]) {
  return [...new Set(value)].sort((left, right) => left.localeCompare(right));
}

function toCurrentMenuNodes(sourceMenus: PermissionMenuSource[], parentId: number | null = null): FrameworkMenuSourceNode[] {
  return sourceMenus
    .filter((menu) => menu.enabled !== false)
    .filter((menu) => (menu.parentId ?? null) === parentId)
    .sort((left, right) => left.sortNo - right.sortNo)
    .map((menu) => ({
      code: menu.code,
      title: menu.title,
      i18nKey: menu.i18nKey,
      path: menu.path,
      icon: menu.icon,
      permissionCode: menu.permissionCode,
      sortNo: menu.sortNo,
      children: toCurrentMenuNodes(sourceMenus, menu.id),
    }));
}
</script>

<style scoped>
.menu-permission-assignment {
  min-height: 0;
  height: min(640px, calc(100vh - 220px));
  display: grid;
  grid-template-columns: minmax(300px, 42%) minmax(360px, 1fr);
  gap: 16px;
  overflow: hidden;
}

.permission-tree-panel,
.permission-detail-panel {
  min-width: 0;
  min-height: 0;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  background: var(--xuan-panel);
  overflow: hidden;
}

.permission-tree-panel {
  display: flex;
  flex-direction: column;
  padding: 12px;
}

.permission-assignment-search {
  margin-bottom: 10px;
}

.permission-tree-list {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  gap: 4px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.permission-tree-row {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 34px;
  border: 0;
  border-radius: 6px;
  color: var(--xuan-text);
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.permission-tree-row:hover,
.permission-tree-row.active {
  color: var(--xuan-primary);
  background: var(--xuan-active);
}

.permission-expand-button,
.permission-leaf-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 0;
  color: var(--xuan-muted);
  background: transparent;
}

.permission-node-title {
  min-width: 0;
  overflow: hidden;
  flex: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-node-count {
  margin-left: 8px;
  color: var(--xuan-primary);
  font-size: 12px;
  white-space: nowrap;
}

.permission-detail-panel {
  display: flex;
  flex-direction: column;
  padding: 14px;
}

.permission-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.permission-detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.permission-detail-title span {
  color: var(--xuan-primary);
  font-size: 13px;
}

.permission-detail-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.permission-detail-list {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.permission-detail-list :deep(.el-checkbox-group) {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 8px 14px;
}

.permission-detail-item {
  align-items: center;
  min-height: 34px;
  height: auto;
  margin-right: 0;
}

.permission-name {
  margin-right: 8px;
  color: var(--xuan-text);
  font-weight: 600;
}

code {
  border-radius: 4px;
  padding: 2px 5px;
  color: #b45309;
  background: #fff7ed;
  font-size: 12px;
}

.permission-related-tag {
  margin-right: 8px;
}

@media (max-width: 900px) {
  .menu-permission-assignment {
    height: auto;
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .permission-tree-panel,
  .permission-detail-panel {
    max-height: 360px;
  }
}
</style>
