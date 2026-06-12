---
title: "路由菜单权限"
---

本文记录前端路由、菜单、权限 code、pageKey 和 titleKey 的对应规则。

## 路由 meta 规范

新增页面路由必须同时维护页面承载信息和权限元数据：

```ts
{
  path: '/sales/orders',
  name: 'SalesOrderList',
  component: () => import('@/views/sales/SalesOrderList.vue'),
  meta: {
    title: '销售单',
    titleKey: 'nav.salesOrder',
    permission: 'erp-sale-draft:view',
    pageKey: 'sales-order',
  },
}
```

要求：

- `meta.title` 用于兜底显示。
- `meta.titleKey` 必须能在 i18n 文案中找到。
- `meta.permission` 必须与后端权限码一致。
- `meta.pageKey` 必须与列权限、菜单 seed、管理页映射保持一致。

## 菜单 code 规范

- 菜单 seed 由后端维护，前端路由只维护页面承载和 meta。
- 菜单 `code` 唯一。
- 叶子菜单必须有 `path`，父级菜单只展开不跳转。
- 叶子菜单 `path` 必须与前端路由一致。
- 菜单 `permissionCode` 为空表示登录可见，否则按权限过滤。

## permission code 规范

- 权限码统一使用 `资源:动作`，例如 `warehouse:view`、`erp-sale-draft:add`。
- 后端 Spring Security 内部可使用 `PERM_` 前缀，JWT 和前端使用原始权限码。
- 前端入口显示、接口请求前置判断、按钮 `v-permission` 都使用当前业务自己的权限码。
- 跨业务单据引用来源数据时，不复用来源模块查看权限，使用当前业务的 `source-access` 权限。

## 未映射页面检查

新页面接入权限体系时，必须同步检查：

- `pageKey` 已被菜单 seed 引用。
- 路由 `path` 与菜单 `path` 一致。
- 权限码已进入后端 permission seed。
- 页面补到 `RoleManagement.vue`、`PermissionManagement.vue`、`ColumnPermissionManagement.vue` 的映射中。
- 权限树、角色权限树、列权限树使用完整菜单目录构建，不使用当前用户可见菜单构建。
- 已补失败测试，锁住新页面不会进入“未映射页面”。

交付说明必须写清楚：`pageKey`、菜单 `code`、路由 `name/path`、权限 `code/prefix`、三个管理页映射，以及为什么不会落入未映射页面。



