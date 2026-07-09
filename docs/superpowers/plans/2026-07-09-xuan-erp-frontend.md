# Xuan ERP Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the first runnable Vue 3 frontend shell for Xuan ERP under `frontend`, including the approved CareerCompass-style login page, real backend login wiring, language/theme settings, app shell, component management page, dashboard, and product management preview page.

**Architecture:** The frontend is a standalone Vite + Vue 3 + TypeScript app. Authentication, settings, routing, API calls, and pages are split into focused modules so later ERP business pages can reuse the same layout and component patterns.

**Tech Stack:** Vue 3, Vite, TypeScript, Vue Router, Pinia, Axios, Element Plus, Vue I18n, lucide-vue-next.

---

## File Structure

- Create: `frontend/package.json` - frontend dependencies and scripts.
- Create: `frontend/index.html` - Vite entry HTML.
- Create: `frontend/vite.config.ts` - Vue plugin, path alias, and dev server config.
- Create: `frontend/tsconfig.json`, `frontend/tsconfig.node.json`, `frontend/env.d.ts` - TypeScript config.
- Create: `frontend/.env.example` - documented API base URL.
- Create: `frontend/src/main.ts` - app bootstrap.
- Create: `frontend/src/App.vue` - root router outlet.
- Create: `frontend/src/router/index.ts` - route definitions and auth guard.
- Create: `frontend/src/stores/auth.ts` - auth state, login, current-user, logout.
- Create: `frontend/src/stores/settings.ts` - language and theme state.
- Create: `frontend/src/api/http.ts` - Axios instance and interceptors.
- Create: `frontend/src/api/auth.ts` - IAM login/current-user API.
- Create: `frontend/src/types/auth.ts` - auth request/response/current-user types.
- Create: `frontend/src/i18n/index.ts` - i18n setup.
- Create: `frontend/src/i18n/messages.ts` - Chinese and English messages.
- Create: `frontend/src/styles/global.css` - global resets and app shell tokens.
- Create: `frontend/src/styles/login.css` - CareerCompass-style login and animated people.
- Create: `frontend/src/styles/shell.css` - ERP admin shell styles.
- Create: `frontend/src/layouts/AppLayout.vue` - sidebar, topbar, tabs, content shell.
- Create: `frontend/src/views/LoginView.vue` - approved login page.
- Create: `frontend/src/views/DashboardView.vue` - dashboard preview.
- Create: `frontend/src/views/ComponentCenterView.vue` - component management page.
- Create: `frontend/src/views/ProductManagementView.vue` - product management preview page.
- Create: `frontend/src/components/app/AppLogo.vue` - shared Xuan ERP logo.
- Create: `frontend/src/components/app/ThemeSwitcher.vue` - theme color control.
- Create: `frontend/src/components/app/LanguageSwitcher.vue` - language control.
- Create: `frontend/src/components/login/AnimatedPeople.vue` - mouse-following geometric people.
- Create: `frontend/src/components/business/QueryToolbar.vue` - reusable query toolbar.
- Create: `frontend/src/components/business/DataTableShell.vue` - reusable table frame.
- Create: `frontend/src/components/business/PermissionButton.vue` - future permission-aware button.
- Modify: `README.md` - add frontend directory and common commands.

## Task 1: Scaffold Frontend Project

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/env.d.ts`
- Create: `frontend/.env.example`

- [ ] **Step 1: Create `frontend/package.json`**

```json
{
  "name": "xuan-erp-frontend",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --host 127.0.0.1 --port 5173",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview --host 127.0.0.1 --port 5173"
  },
  "dependencies": {
    "@element-plus/icons-vue": "^2.3.1",
    "@vitejs/plugin-vue": "^5.2.4",
    "axios": "^1.7.9",
    "element-plus": "^2.10.7",
    "lucide-vue-next": "^0.468.0",
    "pinia": "^2.3.0",
    "vue": "^3.5.13",
    "vue-i18n": "^10.0.5",
    "vue-router": "^4.5.0"
  },
  "devDependencies": {
    "typescript": "^5.7.2",
    "vite": "^6.0.7",
    "vue-tsc": "^2.2.0"
  }
}
```

- [ ] **Step 2: Create Vite shell files**

`frontend/index.html`:

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Xuan ERP</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

`frontend/vite.config.ts`:

```ts
import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
  },
});
```

- [ ] **Step 3: Create TypeScript config**

`frontend/tsconfig.json`:

```json
{
  "files": [],
  "references": [
    { "path": "./tsconfig.node.json" }
  ],
  "compilerOptions": {
    "target": "ES2022",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "strict": true,
    "jsx": "preserve",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "esModuleInterop": true,
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "noEmit": true,
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.vue", "env.d.ts"]
}
```

`frontend/tsconfig.node.json`:

```json
{
  "compilerOptions": {
    "composite": true,
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

`frontend/env.d.ts`:

```ts
/// <reference types="vite/client" />
```

`frontend/.env.example`:

```dotenv
VITE_API_BASE_URL=http://localhost:8100
```

- [ ] **Step 4: Install dependencies**

Run: `cd D:\xuan-erp\frontend; npm install`

Expected: `node_modules` and `package-lock.json` are created under `frontend`.

## Task 2: Add App Bootstrap And Styles

**Files:**
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/styles/global.css`

- [ ] **Step 1: Create app entry**

`frontend/src/main.ts`:

```ts
import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import { createPinia } from 'pinia';
import App from './App.vue';
import { router } from './router';
import { i18n } from './i18n';
import './styles/global.css';
import './styles/login.css';
import './styles/shell.css';

createApp(App)
  .use(createPinia())
  .use(router)
  .use(i18n)
  .use(ElementPlus)
  .mount('#app');
```

`frontend/src/App.vue`:

```vue
<template>
  <RouterView />
</template>
```

- [ ] **Step 2: Create global styles**

`frontend/src/styles/global.css`:

```css
:root {
  --xuan-primary: #6366f1;
  --xuan-primary-foreground: #ffffff;
  --xuan-bg: #f5f7fb;
  --xuan-panel: #ffffff;
  --xuan-border: #e5e7eb;
  --xuan-text: #111827;
  --xuan-muted: #6b7280;
  --xuan-sidebar: #f7f9fc;
  --xuan-active: #dcecff;
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  width: 100%;
  min-height: 100%;
  margin: 0;
}

body {
  color: var(--xuan-text);
  background: var(--xuan-bg);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

button,
input,
select,
textarea {
  font: inherit;
}

a {
  color: inherit;
  text-decoration: none;
}
```

## Task 3: Add Auth API And Stores

**Files:**
- Create: `frontend/src/types/auth.ts`
- Create: `frontend/src/api/http.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/stores/settings.ts`

- [ ] **Step 1: Define auth types**

`frontend/src/types/auth.ts`:

```ts
export interface LoginRequest {
  tenantId: number;
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType?: string;
  expiresIn?: number;
  tenantId?: number;
  userId?: number;
  username?: string;
}

export interface CurrentUser {
  tenantId: number;
  userId: number;
  username: string;
  displayName?: string;
  permissions?: string[];
}

export interface ApiResponse<T> {
  code?: string | number;
  message?: string;
  data?: T;
}
```

- [ ] **Step 2: Create Axios instance**

`frontend/src/api/http.ts`:

```ts
import axios from 'axios';

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8100',
  timeout: 15000,
});

export function setBearerToken(token: string | null) {
  if (token) {
    http.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete http.defaults.headers.common.Authorization;
  }
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('xuan-erp-token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('xuan-erp-token');
      window.dispatchEvent(new CustomEvent('xuan-auth-expired'));
    }
    return Promise.reject(error);
  },
);
```

- [ ] **Step 3: Create auth API**

`frontend/src/api/auth.ts`:

```ts
import { http } from './http';
import type { ApiResponse, CurrentUser, LoginRequest, LoginResponse } from '@/types/auth';

function unwrap<T>(response: { data: ApiResponse<T> | T }): T {
  const body = response.data;
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as ApiResponse<T>).data as T;
  }
  return body as T;
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  return unwrap<LoginResponse>(await http.post('/api/iam/auth/login', request));
}

export async function getCurrentUser(): Promise<CurrentUser> {
  return unwrap<CurrentUser>(await http.get('/api/iam/auth/current-user'));
}
```

- [ ] **Step 4: Create auth store**

`frontend/src/stores/auth.ts`:

```ts
import { defineStore } from 'pinia';
import { getCurrentUser, login } from '@/api/auth';
import { setBearerToken } from '@/api/http';
import type { CurrentUser, LoginRequest } from '@/types/auth';

interface AuthState {
  token: string | null;
  currentUser: CurrentUser | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('xuan-erp-token'),
    currentUser: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  actions: {
    async login(request: LoginRequest) {
      const response = await login(request);
      this.token = response.accessToken;
      localStorage.setItem('xuan-erp-token', response.accessToken);
      setBearerToken(response.accessToken);
      await this.loadCurrentUser();
    },
    async loadCurrentUser() {
      this.currentUser = await getCurrentUser();
    },
    logout() {
      this.token = null;
      this.currentUser = null;
      localStorage.removeItem('xuan-erp-token');
      setBearerToken(null);
    },
  },
});
```

- [ ] **Step 5: Create settings store**

`frontend/src/stores/settings.ts`:

```ts
import { defineStore } from 'pinia';

export type LocaleCode = 'zh-CN' | 'en-US';
export type ThemeColor = 'blue' | 'green' | 'orange' | 'indigo';

const themeColorMap: Record<ThemeColor, string> = {
  blue: '#1677ff',
  green: '#0f9d76',
  orange: '#f59e0b',
  indigo: '#6366f1',
};

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    locale: (localStorage.getItem('xuan-locale') as LocaleCode) || 'zh-CN',
    themeColor: (localStorage.getItem('xuan-theme-color') as ThemeColor) || 'blue',
  }),
  actions: {
    setLocale(locale: LocaleCode) {
      this.locale = locale;
      localStorage.setItem('xuan-locale', locale);
    },
    setThemeColor(color: ThemeColor) {
      this.themeColor = color;
      localStorage.setItem('xuan-theme-color', color);
      document.documentElement.style.setProperty('--xuan-primary', themeColorMap[color]);
    },
    applyTheme() {
      this.setThemeColor(this.themeColor);
    },
  },
});
```

## Task 4: Add I18n And Router

**Files:**
- Create: `frontend/src/i18n/messages.ts`
- Create: `frontend/src/i18n/index.ts`
- Create: `frontend/src/router/index.ts`

- [ ] **Step 1: Create messages**

`frontend/src/i18n/messages.ts`:

```ts
export const messages = {
  'zh-CN': {
    login: {
      title: 'Welcome back!',
      subtitle: '请输入租户与账号信息',
      tenantId: 'Tenant ID',
      username: 'Username',
      password: 'Password',
      rememberTenant: 'Remember tenant',
      contactAdmin: '联系管理员',
      submit: 'Log in',
      backendTarget: 'Use tenant account',
      accountNote: '账号由租户管理员开通',
      required: '请填写完整登录信息',
      failed: '登录失败，请检查账号或服务状态',
    },
    nav: {
      dashboard: '仪表盘',
      components: '组件管理',
      products: '商品管理',
      inventory: '进销存',
      baseData: '基础资料',
    },
  },
  'en-US': {
    login: {
      title: 'Welcome back!',
      subtitle: 'Please enter your tenant and account details',
      tenantId: 'Tenant ID',
      username: 'Username',
      password: 'Password',
      rememberTenant: 'Remember tenant',
      contactAdmin: 'Contact admin',
      submit: 'Log in',
      backendTarget: 'Use tenant account',
      accountNote: 'Accounts are created by tenant admins',
      required: 'Please complete the login form',
      failed: 'Login failed. Check your account or service status',
    },
    nav: {
      dashboard: 'Dashboard',
      components: 'Component Center',
      products: 'Product Management',
      inventory: 'Inventory',
      baseData: 'Base Data',
    },
  },
};
```

- [ ] **Step 2: Create i18n setup**

`frontend/src/i18n/index.ts`:

```ts
import { createI18n } from 'vue-i18n';
import { messages } from './messages';

export const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('xuan-locale') || 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages,
});
```

- [ ] **Step 3: Create router with auth guard**

`frontend/src/router/index.ts`:

```ts
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import AppLayout from '@/layouts/AppLayout.vue';
import LoginView from '@/views/LoginView.vue';
import DashboardView from '@/views/DashboardView.vue';
import ComponentCenterView from '@/views/ComponentCenterView.vue';
import ProductManagementView from '@/views/ProductManagementView.vue';

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '仪表盘' } },
      { path: 'components', name: 'components', component: ComponentCenterView, meta: { title: '组件管理' } },
      {
        path: 'inventory/products',
        name: 'products',
        component: ProductManagementView,
        meta: { title: '商品管理', permission: 'product:view' },
      },
    ],
  },
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'dashboard' };
  }
  return true;
});
```

## Task 5: Build CareerCompass-Style Login

**Files:**
- Create: `frontend/src/components/login/AnimatedPeople.vue`
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/styles/login.css`

- [ ] **Step 1: Implement animated people component**

Create `AnimatedPeople.vue` using the geometry and mouse-follow behavior from `D:\xuan-erp\.codex\brainstorm-preview\login-v2.html`. The component owns mouse coordinates and sets CSS variables `--pupil-x`, `--pupil-y`, `--face-x`, and `--face-y`.

- [ ] **Step 2: Implement login view**

`LoginView.vue` contains:

- Brand: `Xuan ERP`.
- Left visual side with `AnimatedPeople`.
- Right form with `tenantId`, `username`, `password`.
- Remember tenant checkbox.
- `联系管理员` text button.
- Submit calls `authStore.login({ tenantId, username, password })`.
- On success, route to query redirect or `/dashboard`.
- On failure, show `ElMessage.error(t('login.failed'))`.

- [ ] **Step 3: Implement login CSS**

Move the verified `login-v2.html` CSS into `frontend/src/styles/login.css`, replacing static HTML selectors with Vue class names. Keep desktop two-column layout and mobile single-column layout.

- [ ] **Step 4: Manual visual check**

Run: `npm run dev`

Open: `http://127.0.0.1:5173/login`

Expected:

- Desktop shows gray left panel and centered right form.
- The people illustration watches the cursor.
- Mobile hides the left panel.
- Form fields match the real backend login contract.

## Task 6: Build App Shell And Global Controls

**Files:**
- Create: `frontend/src/components/app/AppLogo.vue`
- Create: `frontend/src/components/app/ThemeSwitcher.vue`
- Create: `frontend/src/components/app/LanguageSwitcher.vue`
- Create: `frontend/src/layouts/AppLayout.vue`
- Create: `frontend/src/styles/shell.css`

- [ ] **Step 1: Create logo and switchers**

`AppLogo.vue` renders the square `X` mark and `Xuan ERP` label. `ThemeSwitcher.vue` exposes blue, green, orange, and indigo color swatches. `LanguageSwitcher.vue` switches `zh-CN` and `en-US` and updates `i18n.global.locale.value`.

- [ ] **Step 2: Create app layout**

`AppLayout.vue` includes:

- Sidebar with search and nested menu items.
- Top breadcrumb area.
- Tenant pill with `default`.
- Language switcher.
- Theme switcher.
- User avatar and logout action.
- Tab strip with dashboard and current route.
- Main `<RouterView />`.

- [ ] **Step 3: Create shell CSS**

`shell.css` implements the approved ERP screenshot structure:

- `240px` sidebar.
- `48px` topbar.
- Light gray content background.
- White content panels.
- Dense toolbar and table spacing.
- Responsive sidebar collapse behavior for narrow screens.

## Task 7: Build Initial Pages And Reusable Business Components

**Files:**
- Create: `frontend/src/components/business/QueryToolbar.vue`
- Create: `frontend/src/components/business/DataTableShell.vue`
- Create: `frontend/src/components/business/PermissionButton.vue`
- Create: `frontend/src/views/DashboardView.vue`
- Create: `frontend/src/views/ComponentCenterView.vue`
- Create: `frontend/src/views/ProductManagementView.vue`

- [ ] **Step 1: Create reusable business components**

`QueryToolbar.vue` renders a bordered search panel with slots for fields and actions. `DataTableShell.vue` wraps table toolbar, table area, and pagination slot. `PermissionButton.vue` accepts `permission`, `type`, and `disabledReason`; for this slice it renders normally and preserves the permission prop for future IAM integration.

- [ ] **Step 2: Create dashboard page**

`DashboardView.vue` displays four metric cards, business trend bars, and system status list. Use static preview data.

- [ ] **Step 3: Create component management page**

`ComponentCenterView.vue` displays cards for query forms, status tags, permission buttons, table toolbars, drawer/modal forms, and theme/language previews.

- [ ] **Step 4: Create product management page**

`ProductManagementView.vue` displays:

- Title `商品管理`.
- Query fields: name, code, shortName, barcode, category, status.
- Buttons: reset, search, import, import result, add.
- Table columns: sequence, code, name, category, unit, default warehouse, price, cost, actions.
- Static rows matching the approved preview.

## Task 8: Documentation And Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README**

Add a `前端工程` section:

```markdown
## 前端工程

前端工程位于：

```text
D:\xuan-erp\frontend
```

常用命令：

```powershell
cd D:\xuan-erp\frontend
npm install
npm run dev
npm run build
```

默认开发接口地址通过 `VITE_API_BASE_URL` 配置，参考 `frontend\.env.example`。
```

- [ ] **Step 2: Build verification**

Run: `cd D:\xuan-erp\frontend; npm run build`

Expected: TypeScript and Vite build complete successfully.

- [ ] **Step 3: Runtime verification**

Run: `cd D:\xuan-erp\frontend; npm run dev`

Expected: Vite serves at `http://127.0.0.1:5173`.

Open these routes:

- `http://127.0.0.1:5173/login`
- `http://127.0.0.1:5173/dashboard`
- `http://127.0.0.1:5173/components`
- `http://127.0.0.1:5173/inventory/products`

Expected:

- `/login` is public.
- Protected pages redirect to `/login` until token exists.
- Login page matches approved v2 preview.
- App shell matches ERP screenshot direction.

## Self-Review

- Spec coverage: login, real backend contract, language switch, theme switch, app shell, component management, dashboard, and product preview are each mapped to tasks.
- Placeholder scan: no unfinished marker words or vague empty tasks remain.
- Type consistency: `LoginRequest`, `LoginResponse`, and `CurrentUser` are defined before API/store usage and match the backend request field names.
- Scope check: no database, Flyway, backend API, Google login, sign-up, or password reset work is included.
