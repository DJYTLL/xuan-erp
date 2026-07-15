# 前端接入 IAM 第一阶段接口执行文档

## 文档目的

这份文档给前端窗口直接执行，用来把当前前端从“本地静态菜单 + `currentUser.permissions` 权限判断”改成“消费 `xuan-iam` 第一阶段真实接口”。

当前目标只做第一阶段最小闭环：

- 登录后能拿到当前用户信息
- 登录后能拿到当前菜单树和权限快照
- 左侧菜单改为后端返回
- 路由守卫改为后端权限快照判断
- 按钮权限改为后端权限快照判断

这份文档不要求前端现在就完成：

- 字段编辑权限
- 数据范围权限
- 状态动作权限
- 权限管理后台页面

## 当前现状

当前前端代码仍然是旧链路：

- `src/api/auth.ts`
  - 已调用 `POST /api/iam/auth/login`
  - 已调用 `GET /api/iam/auth/current-user`
- `src/stores/auth.ts`
  - 登录后把 `currentUser` 放进 `authStore`
  - `hasPermission()` 直接判断 `currentUser.permissions`
- `src/config/navigation.ts`
  - 左侧菜单是前端本地静态写死
- `src/layouts/AppLayout.vue`
  - 用静态菜单再做一次前端权限过滤
- `src/router/index.ts`
  - 路由守卫用 `route.meta.permission + authStore.hasPermission()`

所以现在前端虽然“有权限控制”，但还没有真正接上：

- `GET /api/iam/menus/current`
- `GET /api/iam/permissions/current`

## 第一阶段接口清单

前端第一阶段需要理解这 4 个接口：

1. `POST /api/iam/auth/login`
2. `GET /api/iam/auth/current-user`
3. `GET /api/iam/menus/current`
4. `GET /api/iam/permissions/current`

推荐使用方式：

- 登录时调用 `POST /api/iam/auth/login`
- 登录成功后，不需要再额外调一次 `current-user`，因为登录响应里已经带 `currentUser`
- 登录成功后，应立即调用 `GET /api/iam/permissions/current`
- 页面刷新后，如果本地仍有 token，先调用 `GET /api/iam/auth/current-user`，再调用 `GET /api/iam/permissions/current`
- `GET /api/iam/menus/current` 在第一阶段主要用于调试、单独排查菜单树；正常页面初始化可以直接使用 `permissions/current` 里的 `menus`

也就是说，前端正常运行时的主接口应该是：

- 身份上下文：`/api/iam/auth/current-user`
- 权限快照：`/api/iam/permissions/current`

而不是单独同时维护两套菜单来源。

## 后端真实返回结构

### 1. 登录接口

`POST /api/iam/auth/login`

请求体：

```json
{
  "tenantId": 1001,
  "username": "admin",
  "password": "123456"
}
```

响应体核心结构：

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "xxxxx",
    "accessTokenExpiresAt": "2026-07-10T14:30:00+08:00",
    "currentUser": {
      "userId": 1,
      "tenantId": 1001,
      "username": "admin",
      "roles": ["tenant_admin"],
      "authVersion": 7,
      "permissions": ["product:view", "purchase:order:view"]
    }
  }
}
```

用途：

- 保存 token
- 保存当前登录用户基础信息
- 登录成功后立刻进入“拉权限快照”步骤

### 2. 当前用户接口

`GET /api/iam/auth/current-user`

响应体核心结构：

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "userId": 1,
    "tenantId": 1001,
    "username": "admin",
    "roles": ["tenant_admin"],
    "authVersion": 7,
    "permissions": ["product:view", "purchase:order:view"]
  }
}
```

用途：

- 刷新页面后恢复顶部用户信息、租户信息、用户名
- 刷新页面后恢复最轻量身份上下文

注意：

- 这个接口仍然只返回轻量身份信息
- 它不是第一阶段动态菜单和完整前端权限控制的主数据源

### 3. 当前权限快照接口

`GET /api/iam/permissions/current`

响应体核心结构：

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "menus": [
      {
        "code": "inventory",
        "title": "进销存",
        "i18nKey": "nav.inventory",
        "path": null,
        "icon": "PackageOpen",
        "permissionCode": null,
        "sortNo": 10,
        "children": [
          {
            "code": "products",
            "title": "商品管理",
            "i18nKey": "nav.products",
            "path": "/inventory/products",
            "icon": null,
            "permissionCode": "product:view",
            "sortNo": 20,
            "children": []
          }
        ]
      }
    ],
    "routePermissions": ["product:view", "purchase:order:view"],
    "buttonPermissions": ["product:view", "purchase:order:view"],
    "columnPermissions": {
      "product-list": ["costPrice", "supplierName"]
    },
    "fieldPermissions": {},
    "dataScopes": [],
    "stateActionRules": {},
    "authVersion": 7
  }
}
```

用途：

- 作为前端权限系统的主数据源
- 动态渲染左侧菜单
- 路由守卫判断
- 按钮权限判断
- 列权限判断

### 4. 当前菜单树接口

`GET /api/iam/menus/current`

响应体核心结构：

```json
{
  "code": "0",
  "message": "success",
  "data": [
    {
      "code": "inventory",
      "title": "进销存",
      "i18nKey": "nav.inventory",
      "path": null,
      "icon": "PackageOpen",
      "permissionCode": null,
      "sortNo": 10,
      "children": []
    }
  ]
}
```

用途：

- 菜单树单独调试
- 菜单接口排错

正常前端流程里不建议再单独调用一次，因为 `permissions/current` 已经包含 `menus`。

## 第一阶段哪些字段现在可用

第一阶段前端应直接使用：

- `menus`
- `routePermissions`
- `buttonPermissions`
- `columnPermissions`
- `authVersion`

第一阶段先不要做真实业务消费的字段：

- `fieldPermissions`
- `dataScopes`
- `stateActionRules`

说明：

- 这 3 个字段当前是契约占位，后端会返回空结构
- 前端类型要保留它们，但第一阶段不要把 UI 逻辑建立在这 3 个字段上

## 推荐前端状态拆分

推荐把前端状态拆成两块，不要再全部塞进 `authStore`：

### 1. `authStore`

只负责：

- token
- `currentUser`
- 登录
- 登出
- 页面刷新后恢复当前用户

### 2. `authorizationStore`

新增一个 store，例如：

- `src/stores/authorization.ts`

只负责：

- `menus`
- `routePermissions`
- `buttonPermissions`
- `columnPermissions`
- `authVersion`
- 加载权限快照
- 清空权限快照

推荐状态结构：

```ts
type CurrentMenuNode = {
  code: string;
  title: string;
  i18nKey: string | null;
  path: string | null;
  icon: string | null;
  permissionCode: string | null;
  sortNo: number;
  children: CurrentMenuNode[];
};

type CurrentPermissionSnapshot = {
  menus: CurrentMenuNode[];
  routePermissions: string[];
  buttonPermissions: string[];
  columnPermissions: Record<string, string[]>;
  fieldPermissions: Record<string, string[]>;
  dataScopes: string[];
  stateActionRules: Record<string, string[]>;
  authVersion: number;
};
```

## 前端具体修改清单

### 1. 修改 `src/types/auth.ts`

补充：

- `CurrentMenuNode`
- `CurrentPermissionSnapshot`

保留现有：

- `LoginRequest`
- `CurrentUser`
- `LoginResponse`
- `ApiResponse`

### 2. 修改 `src/api/auth.ts`

新增：

- `getCurrentPermissionSnapshot()`
- `getCurrentMenus()`，用于排查和调试，非主流程必须

推荐接口定义：

```ts
export async function getCurrentPermissionSnapshot(): Promise<CurrentPermissionSnapshot> {
  const response = await http.get<ApiResponse<CurrentPermissionSnapshot> | CurrentPermissionSnapshot>(
    '/api/iam/permissions/current',
  );
  return unwrap<CurrentPermissionSnapshot>(response.data);
}
```

### 3. 新增 `src/stores/authorization.ts`

建议提供：

- `snapshot`
- `menus`
- `routePermissions`
- `buttonPermissions`
- `columnPermissions`
- `loadPermissionSnapshot()`
- `clear()`
- `hasRoutePermission()`
- `hasButtonPermission()`

建议按钮和路由的超管旁路规则保持一致：

- 如果权限中包含 `*`
- 或包含 `admin:*`
- 则直接放行

### 4. 修改 `src/stores/auth.ts`

当前 `authStore.hasPermission()` 不应再作为主权限来源。

建议改法：

- `authStore` 保留身份职责
- 去掉或弱化 `hasPermission()`
- 登录成功后：
  - 保存 token
  - 保存 `currentUser`
  - 由调用方或 store 内部继续加载 `authorizationStore.loadPermissionSnapshot()`
- `logout()` 时同时清空 `authorizationStore`

推荐登录后链路：

1. `login`
2. 保存 token
3. 保存 `currentUser`
4. `authorizationStore.loadPermissionSnapshot()`
5. 跳首页

推荐刷新后链路：

1. 从本地恢复 token
2. `authStore.loadCurrentUser()`
3. `authorizationStore.loadPermissionSnapshot()`

### 5. 修改 `src/router/index.ts`

当前路由守卫仍然直接读取 `authStore.hasPermission()`。

建议改成：

- 未登录：跳登录页
- 已登录但 `currentUser` 未加载：先加载 `currentUser`
- 已登录但权限快照未加载：再加载权限快照
- 路由上有 `meta.permission` 时，用 `authorizationStore.hasRoutePermission()` 判断

注意：

- 第一阶段不要求路由表动态注册
- 当前静态路由表可以继续保留
- 但“能不能进入路由”的判断必须改成依赖权限快照

### 6. 修改 `src/layouts/AppLayout.vue`

当前问题：

- 菜单来自 `createNavigationMenu(t)`
- 再用 `authStore.hasPermission()` 本地过滤

第一阶段应改成：

- 左侧菜单主数据源改为 `authorizationStore.snapshot.menus`
- 不再把静态菜单当作事实源

建议：

- 保留当前渲染组件结构
- 新增一个把后端 `CurrentMenuNode` 转成页面渲染节点的转换函数
- 当前静态菜单里的图标、`keepAlive`、`affixTab`、`closable` 等前端显示元数据，可以先通过“本地映射表”补齐

原因：

- 后端菜单树负责“菜单是否存在、是否可见、路径是什么”
- 前端仍然可以负责“某个菜单配什么图标、是否缓存、是否固定标签页”

推荐做法不是继续保留整棵静态菜单树，而是只保留一份轻量前端展示元数据映射，例如：

```ts
const menuUiMetaMap = {
  dashboard: { icon: LayoutDashboard, affixTab: true, closable: false, keepAlive: true },
  products: { keepAlive: true },
};
```

### 7. 修改 `src/config/navigation.ts`

当前整份静态菜单配置不再适合作为事实源。

第一阶段建议二选一：

1. 保留文件，但重构成“菜单 UI 元数据映射表”
2. 新建 `src/config/menu-ui-meta.ts`，把这个职责从 `navigation.ts` 拆出去

不建议继续保留：

- 完整静态菜单树
- 再从静态菜单树上做权限过滤

### 8. 修改按钮权限组件

当前 [PermissionButton.vue](D:\xuan-erp\frontend\src\components\business\PermissionButton.vue) 还没有真正做权限判断。

第一阶段应改成：

- 接收 `permission`
- 读取 `authorizationStore.hasButtonPermission(permission)`
- 无权限时：
  - 隐藏按钮，或者
  - 禁用按钮并显示原因

这一步至少要保证“按钮组件本身真的能读权限快照”。

## 推荐实施顺序

前端窗口建议按这个顺序做，不要乱序：

1. 补类型
2. 补 `api/auth.ts` 新接口
3. 新增 `authorizationStore`
4. 调整登录后和刷新后的加载顺序
5. 改路由守卫
6. 改左侧菜单来源
7. 改按钮权限组件
8. 最后清理静态菜单残留

## 第一阶段验收标准

前端改完后，至少满足下面几点：

1. 登录成功后，前端会调用 `GET /api/iam/permissions/current`
2. 刷新页面后，前端会重新恢复：
   - `currentUser`
   - 权限快照
3. 左侧菜单来自后端返回的 `menus`
4. 路由权限判断来自 `routePermissions`
5. 按钮权限判断来自 `buttonPermissions`
6. `columnPermissions` 能被 store 保存并可供后续页面读取
7. 当前阶段不会因为 `fieldPermissions`、`dataScopes`、`stateActionRules` 是空结构而报错

## 第一阶段明确不做什么

前端窗口这轮不要扩大成下面这些任务：

- 不做动态路由注册重构
- 不做角色权限管理页面
- 不做列权限完整页面渲染体系
- 不做字段级只读/可编辑控制
- 不做数据范围过滤
- 不做状态动作权限控制
- 不做整套 UI 大重构

这轮只做“把 IAM 第一阶段真实接口接上并跑通最小权限闭环”。

## 给前端窗口的最终一句话

前端窗口拿到这份文档后，目标不是“重写权限系统”，而是把当前前端从：

- 静态菜单
- `currentUser.permissions`

改成：

- 后端 `menus`
- 后端 `permissions/current`

并保证登录后、刷新后、路由进入、按钮显示这四条链路都能稳定工作。
