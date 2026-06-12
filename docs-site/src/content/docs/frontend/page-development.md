---
title: "页面开发规范"
---

本文记录新增页面时的前端、后端、权限、菜单、列权限和测试要求。

## 前端调用链路

前端页面只调用 Gateway 暴露的 `/api/**` 接口，不直接调用 `xuan-product`、`xuan-sales` 这类后端服务名。

```ts
// src/api/product.ts
import request from '@/utils/request';

export interface ProductDTO {
  id: string;
  code: string;
  name: string;
}

export function getProduct(id: string) {
  return request<ProductDTO>({
    url: `/api/products/${id}`,
    method: 'get',
  });
}
```

推荐链路：

```text
Vue 页面 -> xuan-gateway -> 后端业务服务
```

如果某个后端服务还需要调用另一个服务，例如销售服务查询商品快照，则属于后端服务间调用：

```text
Vue 页面 -> xuan-gateway -> xuan-sales -> xuan-product
```

OpenFeign、LoadBalancer、`@LoadBalanced` 都属于后端服务间通信实现，前端不直接关心，也不直接使用服务名。详细说明见：[服务通信](/architecture/service-communication/)。

## 基础开发规范

- API 请求统一走 `src/utils/request.ts`。
- Token 注入、401 刷新、重试逻辑集中处理。
- 页面错误提示统一使用 `useApiError()`，禁止页面里散落 `try/catch + ElMessage`。
- 新增、编辑、删除、审核、导入、导出、打印等按钮必须加权限控制。
- 文案统一走 i18n，常用命名空间包括 `nav.*`、`page.*`、`action.*`、`field.*`、`message.*`、`placeholder.*`。
- 页面结构复用 `page-shell`、`page-header`、`page-title`、`table-card`、`table-pagination`。
- 后端分页页面不要只做前端当前页搜索；大数据搜索应放到后端或搜索索引。

## 页面接入权限体系

新增页面只要涉及菜单、页面权限、按钮权限、列权限任意一种，就视为“页面接入权限体系任务”。必须同步完成：

- 页面组件。
- 前端路由。
- `route meta.title`、`meta.permission`、`meta.titleKey`、`meta.pageKey`。
- 后端 permission seed。
- menu seed。
- 必要 migration。
- 列权限定义。
- `RoleManagement.vue`、`PermissionManagement.vue`、`ColumnPermissionManagement.vue` 映射。
- 回归测试，确保不进入“未映射页面”。

## 后台页面布局规范

页面先分类，再套骨架：

| 类型 | 说明 |
| --- | --- |
| A 类 | 列表/筛选页 |
| B 类 | 单据/表单页 |

列表页要求：

- 标题和首个筛选卡片放在 `page-header` 内。
- ERP 列表页搜索区默认一行优先。
- 刷新、搜索、重置、新增等操作按钮固定在搜索卡片第一行右侧。
- 筛选控件宽度不足时，筛选控件整体换行，不能挤压或遮挡操作按钮。
- `el-select`、日期范围、远程选择器必须有明确宽度或最小宽度。

单据页要求：

- `page-header` 只放标题、面包屑、全局操作按钮。
- 业务表单卡片放在 header 外。
- 不混用列表页和单据页 DOM 结构。

改布局后必须补回归测试，锁住按钮固定右侧、筛选控件不遮挡按钮。

## 单据交互规范

- 列表进入新增、编辑、查看时必须带来源参数，例如 `returnTo`、`from`。
- 返回和关闭标签优先回 `returnTo`。
- 审核流程只确认一次：确认后先静默保存，再调用审核接口。
- 保存成功、审核成功使用统一后续操作弹窗。
- 打印统一使用 `PrintPreviewDialog`，不新开窗口，不跳转当前页。
- 审核、复制、红冲等依赖详情状态的按钮要防闪现：初始化阶段用禁用占位按钮。
- 同一按钮组里只保留一个强主按钮。
- 红冲、删除等风险操作必须二次确认并填写原因。

## 性能与防卡顿规范

- 页面打开分两阶段：首屏 shell 立即渲染，重内容 mounted 后异步加载。
- 首屏不要等待列权限、用户表格设置、复杂下拉、首次列表接口、打印组件。
- 性能敏感入口页不要直接 import 重表格、打印弹窗、复杂弹窗。
- 重内容放到 `DeferredPanel`。
- deferred panel loader 必须缓存 promise，路由预热和页面挂载复用同一个 loader。
- 同组页面可以预热 chunk 和默认第一页数据。
- 当前页面首屏或当前业务动作必须展示的数据，由后端一次性聚合返回，禁止前端或后端 N+1 次请求。
- 重数据可懒加载，实时数据可单独刷新但必须支持批量查询，超大明细必须分页或设置数量上限。
- 性能问题必须看埋点，区分接口慢、chunk 慢、组件 setup 慢、DOM 渲染慢。

## 测试与验证要求

- 权限接入类任务先写失败测试，再实现。
- 页面权限、菜单映射、未映射页面、列权限树等要有回归测试。
- ERP 搜索区布局要有结构测试，保证按钮不被筛选控件挤压。
- 性能敏感页面要测试入口页不直接引入重组件。
- 前端提交前跑类型检查和相关测试。
- 交付说明必须写清楚验证命令和结果。



