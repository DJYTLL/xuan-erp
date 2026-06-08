---
title: "权限接入规范"
---

后端权限不应拆成多套系统。IAM 仍然是全局权限、菜单、角色和列权限的唯一中心；各业务服务只维护自己拥有的权限定义，并通过 CI/CD 或受控同步流程进入 IAM。

在 DDD 分层中，权限校验通常属于应用层职责：应用服务在执行业务用例前检查当前用户、租户和权限上下文；领域模型只表达业务规则，不直接依赖 IAM、JWT 或 Web 上下文。

## 后端服务负责什么

- 定义本服务拥有的权限码。
- 在接口上声明所需权限。
- 对 super admin、租户状态、服务间调用做统一校验。
- 在新增页面或接口时补齐权限测试。

## IAM 负责什么

- 保存全量权限、菜单、角色、角色授权、列权限。
- 提供登录和授权快照。
- 提供权限管理、角色管理、列权限管理接口。
- 接收受控的权限清单同步。

## 权限码命名

```text
领域:资源:动作
```

示例：

| 权限码 | 说明 |
| --- | --- |
| `product:product:view` | 查看商品 |
| `product:product:create` | 新增商品 |
| `sales:order:audit` | 审核销售单 |
| `inventory:stock:adjust` | 调整库存 |

对于页面级 CRUD，如果资源和领域一致，可以简化为 `sales:view`、`sales:create`、`sales:update`、`sales:delete`。复杂资源保留三级，例如 `finance:receivable:export`。

## 服务内权限清单位置

每个服务必须在自己的工程中维护权限清单，建议使用结构化文件而不是散落在代码注释中：

```text
xuan-sales/
  src/main/resources/permissions/
    menus.yaml
    permissions.yaml
    columns.yaml
```

示例：

```yaml
service: xuan-sales
menus:
  - code: trade:sales
    pageKey: sales-order
    path: /sales/orders
    titleKey: route.salesOrder

permissions:
  - code: sales:view
    menuCode: trade:sales
    action: view
  - code: sales:audit
    menuCode: trade:sales
    action: audit

columns:
  - pageKey: sales-order
    code: sales-order.profit
    label: 毛利
    sensitive: true
```

这份清单是“本服务声明自己有哪些权限点”，不是“本服务保存角色拥有哪些权限”。角色授权仍然只在 IAM 中维护。

## 推荐同步流程

生产环境推荐下面的流程：

1. 开发者新增接口、页面或按钮权限。
2. 在服务内同步更新 `permissions/*.yaml`。
3. CI 扫描所有服务权限清单，检查权限码重复、菜单 code 缺失、pageKey 缺失、列权限格式错误。
4. 部署流水线在目标环境执行 IAM 权限目录同步。
5. IAM 只新增或更新权限目录，不自动给普通角色授权。
6. 业务服务启动时做只读自检，发现 IAM 缺权限则启动失败或告警。

开发环境可以用启动时自动注册提升效率，但测试和生产环境不建议这么做。

## 接口注解示例

```java
@RestController
@RequestMapping("/api/sales/orders")
class SalesOrderController {

    @RequirePermission("sales:view")
    @GetMapping
    PageResult<SalesOrderResponse> page(SalesOrderPageRequest request) {
        return salesOrderQueryService.page(request);
    }

    @RequirePermission("sales:audit")
    @PostMapping("/{id}/audit")
    void audit(@PathVariable Long id, @RequestBody AuditRequest request) {
        salesOrderApplicationService.audit(id, request);
    }
}
```

权限注解只表达“访问这个用例需要什么权限”。租户隔离、单据状态、库存是否足够、客户信用是否允许等业务规则必须继续在应用层和领域层处理。

## 接口接入要求

- 前端路由 meta、菜单 code、后端 permission code 要统一。
- 新增页面时必须同时补菜单、权限、列权限映射。
- 权限树和角色授权树默认使用完整菜单目录构建，不使用当前用户可见菜单构建。
- super admin 只作为旁路，不代表接口可以没有权限定义。

## 新页面接入检查表

新增页面时必须一次性完成：

| 项 | 要求 |
| --- | --- |
| 页面组件 | 完成页面首屏、列表、表单、按钮状态 |
| 前端路由 | `title`、`permission`、`titleKey`、`pageKey` 一起补齐 |
| 菜单 seed | 菜单进入正确分组，不能落入未映射页面 |
| permission seed | 查询、新增、修改、删除、审核、导入导出等动作按需补齐 |
| column seed | 涉及敏感字段时补齐列权限 |
| 后端接口 | 每个首屏接口都有权限码和 super admin 旁路 |
| 回归测试 | 锁住菜单映射、权限树、角色树、列权限树不会遗漏 |

## 常见错误

- 只在前端隐藏按钮，不在后端校验接口权限。
- 业务服务把角色授权表也复制一份，导致 IAM 和业务库权限不一致。
- 服务启动时在生产库自动写权限，缺少审计和回滚。
- 权限树使用“当前用户可见菜单”构建，导致角色管理里看不到未授权页面。
- 新页面只加路由，不加 menu seed 和 permission seed，最后落入“未映射页面”。
