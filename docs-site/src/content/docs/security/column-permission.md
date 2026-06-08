---
title: "列权限"
---

列权限由 IAM 统一管理，业务服务负责按列权限过滤敏感字段。

## 设计目标

- 前端表格、详情页、导出数据都遵守同一套字段可见规则。
- 敏感字段即使前端隐藏，后端也不能返回。
- 列权限跟页面绑定，使用稳定的 `pageKey`，不跟路由 path 强绑定。
- super admin 默认可见全部字段，但敏感导出仍保留审计。

## pageKey 规则

`pageKey` 是页面和列权限的稳定标识。它不应该随着菜单标题、路由路径、前端组件文件名变化而变化。

命名建议：

```text
领域-资源
```

示例：

| 页面 | pageKey |
| --- | --- |
| 商品列表 | `product-list` |
| 销售单列表 | `sales-order` |
| 销售退货列表 | `sales-return` |
| 库存余额 | `inventory-balance` |
| 应收明细 | `finance-receivable` |

## 列权限定义格式

每个服务维护自己的列权限清单，最终同步到 IAM。

```yaml
service: xuan-sales
pageKey: sales-order
columns:
  - code: sales-order.customerPhone
    label: 客户手机号
    field: customerPhone
    sensitive: true
  - code: sales-order.profit
    label: 毛利
    field: profit
    sensitive: true
  - code: sales-order.remark
    label: 内部备注
    field: remark
    sensitive: false
```

## 前端隐藏规则

前端根据 IAM 返回的授权快照决定字段是否显示：

| 场景 | 处理 |
| --- | --- |
| 表格列 | 无权限则不渲染该列 |
| 详情字段 | 无权限则隐藏字段或显示统一占位 |
| 表单字段 | 无编辑权限则禁用或隐藏 |
| 导出字段 | 导出前使用后端返回的可导出字段清单 |

前端隐藏只是体验优化，不作为安全边界。

## 后端字段过滤规则

后端必须在响应和导出两个位置执行字段过滤：

```text
Controller -> Application Service -> Query Result -> ColumnPermissionFilter -> Response DTO
```

要求：

- 列权限过滤应发生在返回前，不能只依赖前端。
- 导出接口必须复用同一套列权限判断。
- Query Service 聚合多个服务字段时，必须按最终页面 `pageKey` 过滤。
- 字段过滤后不要把原始敏感字段写入普通操作日志。

## 与接口权限的关系

接口权限决定“能不能访问这个页面或动作”，列权限决定“访问后能看到哪些字段”。

示例：

| 权限 | 结果 |
| --- | --- |
| 有 `sales:view`，无 `sales-order.profit` | 能看销售单，但看不到毛利 |
| 无 `sales:view`，有 `sales-order.profit` | 不能进入销售单页面 |
| 有 `sales:export`，无 `sales-order.customerPhone` | 能导出，但导出文件不包含客户手机号 |

## 测试要求

- 普通角色看不到未授权敏感列。
- super admin 能看到全部列。
- 导出结果和页面结果使用同样字段过滤。
- `RoleManagement.vue`、`PermissionManagement.vue`、`ColumnPermissionManagement.vue` 中的 pageKey 映射一致。



