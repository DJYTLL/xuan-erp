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

## 第一阶段接口边界

当前第一阶段先解决“前端能拿到真实菜单树和权限快照”这个最小闭环，直接使用：

- `GET /api/iam/menus/current`
- `GET /api/iam/permissions/current`

第一阶段只复用现有表和快照字段：

- `iam_menu`
- `iam_permission`
- `iam_authorization_snapshot`
- `iam_tenant_column_setting`
- `iam_role_column_setting`

当前本地 `backend/xuan-iam/src/main/resources/db/migration/` 已扫描到最新版本为 `V20__sync_tenant_admin_permissions_by_init_template.sql`。本次文档收口不新增 Flyway migration，不修改历史文件；后续如涉及表、字段、索引、约束、初始化数据或函数调整，必须从 `V21__*.sql` 顺序追加。

第一阶段权限快照重点给前端使用的字段是：

- `menus`
- `routePermissions`
- `buttonPermissions`
- `columnPermissions`
- `authVersion`

下面三个字段目前只是契约占位，第一阶段返回空结构，不代表字段权限、数据范围权限和状态动作权限已经真实落地：

- `fieldPermissions` 返回空对象
- `dataScopes` 返回空列表
- `stateActionRules` 返回空对象

所以第一阶段目标不是“所有细颗粒权限都已配置完成”，而是“前端和 Gateway 之后的业务服务有了统一可消费的当前用户授权快照入口”。

## 权限码命名

```text
资源:动作
```

示例：

| 权限码 | 说明 |
| --- | --- |
| `warehouse:view` | 查看仓库 |
| `warehouse:create` | 新增仓库 |
| `erp-sale-draft:add` | 新增销售草稿 |
| `erp-sale-return-draft:source-access` | 销售退货草稿访问来源单据 |

后端 Spring Security 内部可以使用 `PERM_` 前缀，例如 `PERM_warehouse:view`；JWT、前端路由、按钮权限和权限 seed 统一使用原始权限码，不带 `PERM_`。

跨业务单据引用数据时，权限归属当前业务场景，不复用被引用模块的查看权限。前端入口显示、接口请求前置判断、后端 Controller/ApplicationService 校验都应使用当前业务自己的权限。

## 基础操作统一模板

所有微服务后续的基础操作必须优先复用同一套权限模板，避免每个服务各自发明 CRUD 动作名：

| 场景 | 统一权限模板 |
| --- | --- |
| 主资源查询 | `<domain>:view` |
| 主资源新增 | `<domain>:create` |
| 主资源编辑 | `<domain>:update` |
| 主资源删除 | `<domain>:delete` |
| 生命周期启用 | `<domain>:enable` |
| 生命周期停用 | `<domain>:disable` |
| 配置查询 | `<domain>-config:view` |
| 配置编辑 | `<domain>-config:manage` |

补充约束：

- `audit`、`export`、`import`、`print` 等属于扩展动作，只能在基础模板之上追加。
- 如果某个服务的 V1 已经使用旧权限码，例如把启用和停用聚合成一个 `lifecycle` 权限，必须在服务文档和 seed 草稿中明确新旧映射，不能口头约定。
- 真正的授权关系仍然只在 `xuan-iam` 中保存；业务服务只负责声明自己的权限清单。

## 服务内权限清单位置

每个服务必须在自己的工程中维护权限清单，建议使用结构化文件而不是散落在代码注释中。`xuan-iam` seed 机制尚未统一前，可以先在各服务 `src/main/resources/permissions/` 目录下保存草稿文件：

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
  - code: erp-sale-draft:view
    menuCode: trade:sales
    action: view
  - code: erp-sale-draft:audit
    menuCode: trade:sales
    action: audit
  - code: erp-sale-return-draft:source-access
    menuCode: trade:sales-return
    action: source-access

columns:
  - pageKey: sales-order
    code: sales-order.profit
    label: 毛利
    sensitive: true
```

这份清单是“本服务声明自己有哪些权限点”，不是“本服务保存角色拥有哪些权限”。角色授权仍然只在 IAM 中维护。后端 seed 是权限码事实源，前端只消费，不自行创造权限事实。

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
- 路由 `meta.permission` 必须与后端权限码一致。
- 前端按钮统一使用 `v-permission` 控制。
- 新增页面时必须同时补菜单、权限、列权限映射。
- 权限树和角色授权树默认使用完整菜单目录构建，不使用当前用户可见菜单构建。
- super admin 只作为旁路，不代表接口可以没有权限定义。

## 新页面接入检查表

新增页面只要涉及菜单、页面权限、按钮权限、列权限任意一种，就视为“页面接入权限体系任务”，不是普通页面开发。新增页面时必须一次性完成：

| 项 | 要求 |
| --- | --- |
| 页面组件 | 完成页面首屏、列表、表单、按钮状态 |
| 前端路由 | `title`、`permission`、`titleKey`、`pageKey` 一起补齐 |
| 菜单 seed | 菜单进入正确分组，不能落入未映射页面 |
| permission seed | 查询、新增、修改、删除、审核、导入导出等动作按需补齐 |
| column seed | 涉及敏感字段时补齐列权限 |
| 管理页映射 | 补到 `RoleManagement.vue`、`PermissionManagement.vue`、`ColumnPermissionManagement.vue` 的映射中 |
| 后端接口 | 每个首屏接口都有权限码和 super admin 旁路 |
| 回归测试 | 先补失败测试，锁住菜单映射、权限树、角色树、列权限树不会遗漏 |

命名要尽量同源：`pageKey`、菜单 `code`、`i18nKey`、`path`、`permission_code`、权限前缀应能互相对应。交付时必须说明这些映射，以及为什么不会落入“未映射页面”。

## 常见错误

- 只在前端隐藏按钮，不在后端校验接口权限。
- 业务服务把角色授权表也复制一份，导致 IAM 和业务库权限不一致。
- 服务启动时在生产库自动写权限，缺少审计和回滚。
- 权限树使用“当前用户可见菜单”构建，导致角色管理里看不到未授权页面。
- 新页面只加路由，不加 menu seed 和 permission seed，最后落入“未映射页面”。
