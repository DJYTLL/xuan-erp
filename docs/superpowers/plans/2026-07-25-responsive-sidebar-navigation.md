# Responsive Sidebar Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把壳层导航改成桌面完整侧栏、中窄屏真收起、手机抽屉三段式响应式导航，并补上回归验证。

**Architecture:** 继续以 `frontend/src/layouts/AppLayout.vue` 作为壳层状态中心，把“用户手动收起”和“响应式紧凑态”拆成独立状态，再由计算属性汇总出真实 `isSidebarCollapsed`。样式层把现有 `1040px` 以下“直接隐藏文字”的规则收回到 `.sidebar-collapsed` 语义中，并给 `720px` 以下补抽屉入口和移动端导航容器。

**Tech Stack:** Vue 3 `script setup`、Vue Router、Element Plus `el-drawer`、项目现有 Node 校验脚本、Vite 构建。

---

### Task 1: 先补失败的导航响应式验证

**Files:**
- Modify: `frontend/scripts/verify-sidebar-active-ancestor-open.mjs`
- Modify: `frontend/package.json`

- [ ] **Step 1: 先扩展验证脚本，要求它断言新的响应式状态模型和移动端入口**

```js
assert(
  layoutSource.includes('const manualSidebarCollapsed = ref(false);'),
  'AppLayout should track user initiated sidebar collapse separately from responsive compact mode.',
);
assert(
  layoutSource.includes('const isCompactViewport = ref(false);'),
  'AppLayout should track compact viewport mode for tablet widths.',
);
assert(
  layoutSource.includes('const isMobileViewport = ref(false);'),
  'AppLayout should track mobile viewport mode for drawer navigation.',
);
assert(
  layoutSource.includes('const mobileNavVisible = ref(false);'),
  'AppLayout should expose a mobile drawer visibility state.',
);
assert(
  layoutSource.includes('const isSidebarCollapsed = computed(() => manualSidebarCollapsed.value || isCompactViewport.value);'),
  'AppLayout should derive the actual collapsed state from manual and responsive compact sources.',
);
assert(
  layoutSource.includes('<el-drawer v-model="mobileNavVisible"'),
  'AppLayout should render a mobile navigation drawer.',
);
```

- [ ] **Step 2: 先跑脚本，确认它在当前实现下失败**

Run: `npm run verify:sidebar-active-ancestor-open`

Expected: FAIL，提示缺少 `manualSidebarCollapsed`、`isCompactViewport` 或移动端抽屉相关标记。

- [ ] **Step 3: 如果需要，把脚本命令名保留不变，不额外引入新命令**

```json
{
  "scripts": {
    "verify:sidebar-active-ancestor-open": "node scripts/verify-sidebar-active-ancestor-open.mjs"
  }
}
```

- [ ] **Step 4: 暂不修实现，只保留失败测试作为红灯基线**

Run: `npm run verify:sidebar-active-ancestor-open`

Expected: 继续 FAIL，失败点仍然指向新响应式导航需求未实现。

### Task 2: 实现三段式导航状态和模板闭环

**Files:**
- Modify: `frontend/src/layouts/AppLayout.vue`

- [ ] **Step 1: 加入视口与移动抽屉状态，把真实收起态改成计算属性**

```ts
const manualSidebarCollapsed = ref(false);
const isCompactViewport = ref(false);
const isMobileViewport = ref(false);
const mobileNavVisible = ref(false);
const isSidebarCollapsed = computed(() => manualSidebarCollapsed.value || isCompactViewport.value);

function collapseSidebar() {
  if (isMobileViewport.value) {
    mobileNavVisible.value = true;
    return;
  }
  manualSidebarCollapsed.value = !manualSidebarCollapsed.value;
}
```

- [ ] **Step 2: 增加视口同步逻辑，让 `1040px` 以下进入真收起，`720px` 以下进入手机抽屉**

```ts
function syncViewportState() {
  if (typeof window === 'undefined') {
    return;
  }
  isMobileViewport.value = window.innerWidth <= 720;
  isCompactViewport.value = window.innerWidth <= 1040 && !isMobileViewport.value;
  if (!isMobileViewport.value) {
    mobileNavVisible.value = false;
  }
}

onMounted(async () => {
  syncViewportState();
  window.addEventListener('resize', syncViewportState);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewportState);
});
```

- [ ] **Step 3: 给顶部栏补移动菜单入口，并在手机态渲染抽屉导航**

```vue
<button
  v-if="isMobileViewport"
  class="shell-icon-button mobile-nav-toggle"
  type="button"
  aria-label="打开导航菜单"
  @click="mobileNavVisible = true"
>
  <PanelLeftOpen :size="16" />
</button>

<el-drawer v-model="mobileNavVisible" title="导航菜单" direction="ltr" size="280px">
  <nav class="mobile-nav" aria-label="mobile navigation">
    <!-- 复用同一份 filteredMenuGroups 与点击逻辑 -->
  </nav>
</el-drawer>
```

- [ ] **Step 4: 让菜单点击在手机态自动关抽屉，并保持当前高亮与祖先展开逻辑**

```ts
async function navigateToMenuPath(path: string) {
  await router.push(path);
  if (isMobileViewport.value) {
    mobileNavVisible.value = false;
  }
}

async function handleFlyoutMenuClick(item: MenuNode) {
  if (item.disabled || !item.path) {
    return;
  }
  await navigateToMenuPath(item.path);
}
```

- [ ] **Step 5: 跑红灯脚本，确认状态和模板标记补齐后转绿**

Run: `npm run verify:sidebar-active-ancestor-open`

Expected: PASS。

### Task 3: 收口样式断点并验证构建

**Files:**
- Modify: `frontend/src/styles/shell.css`

- [ ] **Step 1: 把 `1040px` 以下的“假收起”样式撤回到 `.sidebar-collapsed` 语义**

```css
@media (max-width: 1040px) {
  .app-shell {
    grid-template-columns: 76px minmax(0, 1fr);
  }

  .app-content {
    padding: 20px;
  }
}
```

- [ ] **Step 2: 给手机态补顶部按钮、抽屉和移动导航滚动区样式**

```css
.mobile-nav-toggle {
  flex: 0 0 auto;
}

.mobile-nav {
  display: grid;
  gap: 10px;
  min-height: 0;
}

@media (max-width: 720px) {
  .app-sidebar {
    display: none;
  }

  .app-topbar {
    align-items: stretch;
  }
}
```

- [ ] **Step 3: 运行导航与壳层验证**

Run: `npm run verify:sidebar-active-ancestor-open`
Expected: PASS

Run: `npm run verify:sidebar-navigation`
Expected: PASS

Run: `npm run verify:shell-framework-enhancements`
Expected: PASS

- [ ] **Step 4: 运行完整前端构建验证**

Run: `npm run build`

Expected: exit 0，`vue-tsc --noEmit && vite build` 通过。

- [ ] **Step 5: 运行差异卫生检查**

Run: `git diff --check`

Expected: no output。
