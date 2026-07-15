<template>
  <aside ref="rootRef" class="navigation-menu-tree">
    <header class="navigation-menu-tree__head">
      <div class="navigation-menu-tree__head-main">
        <span>{{ title }}</span>
        <el-tag size="small" type="info">{{ rootCount }} {{ countUnit }}</el-tag>
      </div>
      <div class="navigation-menu-tree__head-actions">
        <button type="button" @click="expandAllNodes">全部展开</button>
        <button type="button" @click="collapseAllNodes">全部收起</button>
      </div>
    </header>

    <div class="navigation-menu-tree__list">
      <div
        v-for="node in visibleNodes"
        :key="node.key"
        class="navigation-menu-tree__row"
        :class="{ active: node.key === selectedKey, muted: node.count === 0 }"
        :style="{ paddingLeft: `${12 + node.level * indentSize}px` }"
        role="button"
        tabindex="0"
        @click="handleRowClick(node)"
        @dblclick="handleRowDoubleClick(node)"
        @keydown.enter.prevent="handleRowClick(node)"
        @keydown.space.prevent="handleRowClick(node)"
        @contextmenu="handleRowContextMenu($event, node)"
      >
        <span
          class="navigation-menu-tree__expand"
          :class="{ visible: node.children.length }"
          @click.stop="toggleExpanded(node.key)"
        >
          <ChevronRight
            v-if="node.children.length"
            class="navigation-menu-tree__expand-icon"
            :class="{ rotated: isExpanded(node.key) }"
            :size="14"
          />
        </span>
        <span class="navigation-menu-tree__title">{{ node.title }}</span>
        <button
          v-if="showInlineEditButton(node)"
          class="navigation-menu-tree__edit"
          :class="{ visible: contextActionKey === node.key }"
          type="button"
          title="编辑菜单"
          @click.stop="handleEditClick(node)"
          @contextmenu="handleRowContextMenu($event, node)"
        >
          <Pencil :size="13" />
        </button>
        <span class="navigation-menu-tree__count">{{ node.count }}</span>
      </div>
    </div>
  </aside>

  <Teleport to="body">
    <div
      v-if="contextMenu.visible"
      ref="contextMenuRef"
      class="navigation-menu-tree__context-menu"
      :style="{ left: `${contextMenu.x}px`, top: `${contextMenu.y}px` }"
      @click.stop
    >
      <button
        v-for="action in contextMenu.actions"
        :key="action.key"
        type="button"
        :disabled="Boolean(action.disabled)"
        @click="handleContextMenuCommand(action)"
      >
        {{ action.label }}
      </button>
    </div>
  </Teleport>
</template>

<script lang="ts">
export type NavigationMenuTreeNode = {
  key: string;
  title: string;
  level: number;
  sourceCode?: string;
  menuId?: number;
  menuCodes: string[];
  count: number;
  children: NavigationMenuTreeNode[];
};

export type NavigationMenuTreeContextMenuAction = {
  key: string;
  label: string;
  disabled?: boolean;
  visible?: boolean;
  onClick?: (payload: { action: NavigationMenuTreeContextMenuAction; node: NavigationMenuTreeNode }) => void;
};
</script>

<script setup lang="ts">
import { Teleport, computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ChevronRight, Pencil } from 'lucide-vue-next';
import {
  createNavigationPermissionTree,
  type FrameworkMenuSourceNode,
  type NavigationPermissionNode,
} from '@/framework/navigation/menu';

type NavigationMenuSource = FrameworkMenuSourceNode & {
  id: number;
  parentId?: number | null;
};

const props = withDefaults(defineProps<{
  menus: NavigationMenuSource[];
  modelValue: string;
  title?: string;
  keyword?: string;
  allNodeKey?: string;
  allNodeTitle?: string;
  countUnit?: string;
  totalCount?: number;
  indentSize?: number;
  extraNodes?: NavigationMenuTreeNode[];
  countResolver?: (menuCodes: string[], nodeKey: string) => number;
  contextMenuEnabled?: boolean;
  contextMenuActionsResolver?: (node: NavigationMenuTreeNode) => NavigationMenuTreeContextMenuAction[];
  showEditButton?: boolean;
}>(), {
  title: '导航菜单树',
  keyword: '',
  allNodeKey: '__all__',
  allNodeTitle: '全部菜单',
  countUnit: '项',
  totalCount: undefined,
  indentSize: 18,
  extraNodes: () => [],
  countResolver: undefined,
  contextMenuEnabled: false,
  contextMenuActionsResolver: undefined,
  showEditButton: false,
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'node-select': [node: NavigationMenuTreeNode];
  'node-edit': [node: NavigationMenuTreeNode];
  'context-menu-command': [payload: { actionKey: string; node: NavigationMenuTreeNode }];
}>();

const { t } = useI18n();
const rootRef = ref<HTMLElement | null>(null);
const contextMenuRef = ref<HTMLElement | null>(null);
const expandedKeys = ref<Set<string>>(new Set());
const contextActionKey = ref('');
const contextMenu = reactive<{
  visible: boolean;
  x: number;
  y: number;
  node: NavigationMenuTreeNode | null;
  actions: NavigationMenuTreeContextMenuAction[];
}>({
  visible: false,
  x: 0,
  y: 0,
  node: null,
  actions: [],
});

const selectedKey = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
});

const menuByCode = computed(() => new Map(props.menus.map((menu) => [menu.code, menu])));

const matchedMenuCodes = computed(() => {
  const value = props.keyword.trim().toLowerCase();
  if (!value) {
    return new Set(props.menus.map((menu) => menu.code));
  }
  return new Set(props.menus
    .filter((menu) => [
      menu.code,
      menu.title,
      menu.path || '',
      menu.permissionCode || '',
      menu.i18nKey || '',
    ].some((item) => item.toLowerCase().includes(value)))
    .map((menu) => menu.code));
});

const rootCount = computed(() => props.totalCount ?? countMenusByCodes(props.menus.map((menu) => menu.code), props.allNodeKey));

const treeNodes = computed<NavigationMenuTreeNode[]>(() => {
  const navigationNodes = createNavigationPermissionTree(toCurrentMenuNodes(props.menus), t)
    .map((node) => buildTreeNode(node, 1));
  return [
    {
      key: props.allNodeKey,
      title: props.allNodeTitle,
      level: 0,
      menuCodes: props.menus.map((menu) => menu.code),
      count: rootCount.value,
      children: [
        ...navigationNodes,
        ...props.extraNodes.map((node) => ({ ...node, level: 1 })),
      ],
    },
  ];
});

const visibleNodes = computed(() => flattenVisibleNodes(treeNodes.value, props.keyword.trim().toLowerCase()));

watch(
  treeNodes,
  (nodes) => {
    const allNodes = flattenAllNodes(nodes);
    expandedKeys.value = new Set(allNodes.filter((node) => node.children.length).map((node) => node.key));
    const selectedNode = allNodes.find((node) => node.key === selectedKey.value) || allNodes[0];
    if (selectedNode) {
      if (selectedNode.key !== selectedKey.value) {
        selectedKey.value = selectedNode.key;
      }
      emit('node-select', selectedNode);
    }
  },
  { immediate: true },
);

onMounted(() => {
  window.addEventListener('click', handleWindowClick, true);
  window.addEventListener('contextmenu', handleWindowContextMenu);
  window.addEventListener('resize', hideContextMenu);
  window.addEventListener('keydown', handleWindowKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener('click', handleWindowClick, true);
  window.removeEventListener('contextmenu', handleWindowContextMenu);
  window.removeEventListener('resize', hideContextMenu);
  window.removeEventListener('keydown', handleWindowKeydown);
});

function buildTreeNode(node: NavigationPermissionNode, level: number): NavigationMenuTreeNode {
  const sourceMenu = node.sourceCode ? menuByCode.value.get(node.sourceCode) : undefined;
  const children = node.children.map((child) => buildTreeNode(child, level + 1));
  return {
    key: node.key,
    title: node.title,
    level,
    sourceCode: node.sourceCode,
    menuId: sourceMenu?.id,
    menuCodes: node.menuCodes,
    count: countMenusByCodes(node.menuCodes, node.key),
    children,
  };
}

function countMenusByCodes(menuCodes: string[], nodeKey: string) {
  if (props.countResolver) {
    return props.countResolver(menuCodes, nodeKey);
  }
  const selectedCodes = new Set(menuCodes);
  return [...matchedMenuCodes.value].filter((code) => selectedCodes.has(code)).length;
}

function flattenVisibleNodes(nodes: NavigationMenuTreeNode[], value: string): NavigationMenuTreeNode[] {
  const result: NavigationMenuTreeNode[] = [];
  for (const node of nodes) {
    if (value && !matchesNodeOrDescendant(node, value)) {
      continue;
    }
    result.push(node);
    if ((value || isExpanded(node.key)) && node.children.length) {
      result.push(...flattenVisibleNodes(node.children, value));
    }
  }
  return result;
}

function flattenAllNodes(nodes: NavigationMenuTreeNode[]): NavigationMenuTreeNode[] {
  return nodes.flatMap((node) => [node, ...flattenAllNodes(node.children)]);
}

function matchesNodeOrDescendant(node: NavigationMenuTreeNode, value: string): boolean {
  return matchesNode(node, value) || node.count > 0 || node.children.some((child) => matchesNodeOrDescendant(child, value));
}

function matchesNode(node: NavigationMenuTreeNode, value: string) {
  const relatedMenus = node.menuCodes
    .map((code) => menuByCode.value.get(code))
    .filter((menu): menu is NavigationMenuSource => Boolean(menu));
  return [
    node.title,
    ...relatedMenus.flatMap((menu) => [menu.title, menu.code, menu.path || '', menu.permissionCode || '']),
  ].some((item) => item.toLowerCase().includes(value));
}

function selectNode(node: NavigationMenuTreeNode) {
  selectedKey.value = node.key;
  emit('node-select', node);
}

function handleRowClick(node: NavigationMenuTreeNode) {
  hideContextMenu();
  selectNode(node);
}

function handleRowDoubleClick(node: NavigationMenuTreeNode) {
  selectNode(node);
  if (node.children.length) {
    toggleExpanded(node.key);
  }
}

function handleRowContextMenu(event: MouseEvent, node: NavigationMenuTreeNode) {
  if (!props.contextMenuEnabled) {
    return;
  }
  event.preventDefault();
  event.stopPropagation();
  selectNode(node);
  openContextMenu(event, node);
}

function handleEditClick(node: NavigationMenuTreeNode) {
  hideContextMenu();
  emit('node-edit', node);
}

function handleContextMenuCommand(action: NavigationMenuTreeContextMenuAction) {
  if (!contextMenu.node) {
    return;
  }
  const node = contextMenu.node;
  const payload = { action, node };
  hideContextMenu();
  action.onClick?.(payload);
  emit('context-menu-command', { actionKey: action.key, node });
}

function canEditNode(node: NavigationMenuTreeNode) {
  return Boolean(node.sourceCode && menuByCode.value.has(node.sourceCode));
}

function showInlineEditButton(node: NavigationMenuTreeNode) {
  return props.showEditButton && canEditNode(node);
}

function resolveContextMenuActions(node: NavigationMenuTreeNode) {
  return (props.contextMenuActionsResolver?.(node) || [])
    .filter((action) => action.visible !== false);
}

function boundedMenuPosition(event: MouseEvent, actions: NavigationMenuTreeContextMenuAction[]) {
  const width = 168;
  const height = Math.max(44, 12 + actions.length * 36);
  return {
    x: Math.min(event.clientX, window.innerWidth - width - 8),
    y: Math.min(event.clientY, window.innerHeight - height - 8),
  };
}

function openContextMenu(event: MouseEvent, node: NavigationMenuTreeNode) {
  if (!props.contextMenuEnabled) {
    hideContextMenu();
    return;
  }
  const actions = resolveContextMenuActions(node);
  if (!actions.length) {
    hideContextMenu();
    return;
  }
  contextMenu.node = node;
  contextMenu.actions = actions;
  const position = boundedMenuPosition(event, actions);
  contextActionKey.value = node.key;
  contextMenu.visible = true;
  contextMenu.x = position.x;
  contextMenu.y = position.y;
}

function hideContextMenu() {
  contextMenu.visible = false;
  contextMenu.node = null;
  contextMenu.actions = [];
  contextActionKey.value = '';
}

function handleWindowClick(event: MouseEvent) {
  const target = event.target;
  if (
    target instanceof Node
    && (rootRef.value?.contains(target) || contextMenuRef.value?.contains(target))
  ) {
    return;
  }
  hideContextMenu();
}

function handleWindowContextMenu(event: MouseEvent) {
  const target = event.target;
  if (
    !(target instanceof Node)
    || (!rootRef.value?.contains(target) && !contextMenuRef.value?.contains(target))
  ) {
    hideContextMenu();
  }
}

function handleWindowKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    hideContextMenu();
  }
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

function expandAllNodes() {
  expandedKeys.value = new Set(flattenAllNodes(treeNodes.value).filter((node) => node.children.length).map((node) => node.key));
}

function collapseAllNodes() {
  expandedKeys.value = new Set([props.allNodeKey]);
}

function toCurrentMenuNodes(sourceMenus: NavigationMenuSource[], parentId: number | null = null): FrameworkMenuSourceNode[] {
  return sourceMenus
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
.navigation-menu-tree {
  min-width: 0;
  min-height: 0;
  height: 100%;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  background: var(--xuan-panel);
  overflow: hidden;
}

.navigation-menu-tree__head {
  min-height: 60px;
  border-bottom: 1px solid var(--xuan-border);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
}

.navigation-menu-tree__head-main,
.navigation-menu-tree__head-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.navigation-menu-tree__head-main span {
  color: var(--xuan-text);
  font-weight: 700;
}

.navigation-menu-tree__head-actions {
  justify-content: flex-start;
}

.navigation-menu-tree__head-actions button {
  border: 0;
  border-radius: 999px;
  padding: 3px 8px;
  color: var(--xuan-primary);
  background: color-mix(in srgb, var(--xuan-primary) 8%, transparent);
  cursor: pointer;
  font-size: 12px;
  line-height: 1.4;
  transition: color 150ms ease, background 150ms ease;
}

.navigation-menu-tree__head-actions button:hover {
  background: color-mix(in srgb, var(--xuan-primary) 14%, transparent);
}

.navigation-menu-tree__list {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  gap: 3px;
  padding: 10px 8px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.navigation-menu-tree__row {
  width: 100%;
  min-height: 34px;
  border: 0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 0;
  padding-right: 8px;
  padding-bottom: 0;
  color: #4a5565;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: color 150ms ease, background 150ms ease;
}

.navigation-menu-tree__row:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--xuan-primary) 40%, transparent);
  outline-offset: 1px;
}

.navigation-menu-tree__row:hover {
  color: var(--xuan-primary);
  background: color-mix(in srgb, var(--xuan-primary) 8%, transparent);
}

.navigation-menu-tree__row.active {
  color: var(--xuan-primary);
  background: var(--xuan-active);
  font-weight: 700;
}

.navigation-menu-tree__row.muted {
  color: #94a3b8;
}

.navigation-menu-tree__expand {
  width: 24px;
  min-width: 24px;
  height: 24px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #8a97aa;
  cursor: default;
}

.navigation-menu-tree__expand.visible {
  cursor: pointer;
}

.navigation-menu-tree__expand.visible:hover {
  color: var(--xuan-primary);
  background: color-mix(in srgb, var(--xuan-primary) 12%, transparent);
}

.navigation-menu-tree__expand-icon {
  transition: transform 160ms ease;
}

.navigation-menu-tree__expand-icon.rotated {
  transform: rotate(90deg);
}

.navigation-menu-tree__title {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.navigation-menu-tree__edit {
  width: 24px;
  min-width: 24px;
  height: 24px;
  border: 0;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--xuan-primary);
  background: transparent;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transition: opacity 150ms ease, background 150ms ease;
}

.navigation-menu-tree__row:hover .navigation-menu-tree__edit,
.navigation-menu-tree__edit.visible {
  opacity: 1;
  pointer-events: auto;
}

.navigation-menu-tree__edit:hover {
  background: color-mix(in srgb, var(--xuan-primary) 14%, transparent);
}

.navigation-menu-tree__count {
  min-width: 18px;
  color: var(--xuan-primary);
  font-size: 12px;
  line-height: 1;
  text-align: right;
  white-space: nowrap;
}

.navigation-menu-tree__context-menu {
  position: fixed;
  z-index: 2000;
  width: 168px;
  padding: 6px;
  border: 1px solid var(--xuan-border);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.16);
}

.navigation-menu-tree__context-menu button {
  width: 100%;
  height: 32px;
  border: 0;
  border-radius: 6px;
  padding: 0 10px;
  color: #334155;
  background: transparent;
  text-align: left;
  font-size: 13px;
  cursor: pointer;
}

.navigation-menu-tree__context-menu button:hover:not(:disabled) {
  color: var(--xuan-primary);
  background: #eef6ff;
}

.navigation-menu-tree__context-menu button:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}
</style>
