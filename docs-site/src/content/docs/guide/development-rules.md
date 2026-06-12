---
title: "开发规范总览"
---

本文记录 Xuan ERP 新项目开发的统一规则。详细落地说明分散在数据库迁移、DDD、权限接入、接口契约、前端页面开发、路由菜单权限和 CI/CD 文档中；本页作为进入开发前的总检查清单。

## 规则分级

| 类型 | 含义 |
| --- | --- |
| 强制规则 | 违反后不能合并或交付，必须修正 |
| 推荐规则 | 默认遵守，特殊情况需要说明原因 |
| 例外情况 | 允许偏离默认规则，但要记录边界和风险 |
| 交付清单 | 交付说明中必须写清楚的内容 |

## 1. 数据库迁移规范

- 所有表结构、字段、索引、约束、初始化数据变更，必须走 Flyway migration。
- 修改前先扫描当前服务 migration 目录，确认最高版本号，再顺序追加新文件。
- 开始修改前必须说明：当前最高版本号、拟新增 migration 文件名、是否存在版本冲突。
- 禁止跳号、复用版本号、修改历史 migration、覆盖历史 migration、重排历史 migration。
- migration 命名统一为 `V{版本号}__{英文描述}.sql`。
- 结构变更尽量使用 `IF NOT EXISTS`，数据回填必须带明确 `WHERE` 条件。
- 租户业务表默认包含 `tenant_id`。
- 业务删除默认走逻辑删除，使用 `deleted_at`。
- 有唯一性约束的业务表，优先使用活动态唯一索引，避免已删除数据占用唯一键。
- 租户级高频查询表建议补 `(tenant_id, deleted_at)` 组合索引。
- 审计日志、库存流水、打印日志等追加型流水表可作为例外，不走业务恢复模型。

## 2. 新页面接入权限体系规范

- 新增页面只要涉及菜单、页面权限、按钮权限、列权限任意一种，就视为“页面接入权限体系任务”。
- 必须同时完成页面组件、前端路由、`route meta.title`、`meta.permission`、`meta.titleKey`、后端 permission seed、menu seed、必要 migration、列权限定义。
- 页面必须补到 `RoleManagement.vue`、`PermissionManagement.vue`、`ColumnPermissionManagement.vue` 的映射中。
- 新页面必须进入正确菜单分组，不能落入“未映射页面”。
- `pageKey`、菜单 `code`、`i18nKey`、`path`、`permission_code`、权限前缀尽量同源。
- 权限树、角色权限树、列权限树优先使用完整菜单目录构树，不使用当前用户可见菜单构树。
- 新页面接入权限体系时，先补失败测试，锁住“不进入未映射页面”的行为。

## 3. 权限设计规范

- 权限码统一使用 `资源:动作`，例如 `warehouse:view`、`erp-sale-draft:add`。
- 后端 Spring Security 内部使用 `PERM_` 前缀，JWT 和前端使用原始权限码。
- 前端按钮统一使用 `v-permission` 控制。
- 路由 `meta.permission` 必须与后端权限码一致。
- 跨业务单据引用数据时，权限归属当前业务场景，不复用被引用模块的查看权限。
- 推荐使用 `source-access` 表达来源单据访问能力，例如 `erp-sale-return-draft:source-access`。
- 前端入口显示、接口请求前置判断、后端 Controller/ApplicationService 校验都应使用当前业务自己的权限。
- 后端 seed 是权限码事实源，前端只消费，不自行创造权限事实。

## 4. 菜单与路由规范

- 菜单 seed 由后端维护，前端路由维护页面承载和权限元数据。
- 叶子菜单必须有 `path`，父级菜单只展开不跳转。
- 菜单 `code` 唯一，`path` 与前端路由一致。
- 菜单 `permissionCode` 为空表示登录可见，否则按权限过滤。
- 新增页面交付时必须说明：`pageKey`、菜单 `code`、路由 `name/path`、权限 `code/prefix`、三个管理页映射，以及为什么不会落入未映射页面。

## 5. 后端开发规范

- 后端代码放在 `D:\xuan-erp\backend`，后续前端代码可在 `D:\xuan-erp` 下创建独立目录。
- 新项目后端统一使用 DDD 分层，不再使用传统 `Controller`、`Service`、`Mapper`、`Entity` 直通式开发模式。
- `interfaces`：Controller、Request DTO、Response DTO、参数校验、权限注解、协议适配。
- `application`：ApplicationService、Command、Query、事务边界、幂等、审计、用例编排。
- `domain`：Aggregate、Domain Entity、Value Object、Domain Service、Domain Event、Repository 接口、业务规则。
- `infrastructure`：Repository 实现、MyBatis Mapper、PO/DO、外部服务 Client、MQ、缓存、文件存储。
- Controller 只做协议适配，不写业务逻辑。
- Controller 必须加 `@PreAuthorize`。
- Controller 入参转换为 Command 或 Query，再交给 ApplicationService。
- ApplicationService 负责编排用例、事务、幂等、审计，不承载复杂领域规则。
- 复杂业务规则必须进入 Aggregate、Value Object 或 Domain Service。
- Repository 接口放在 domain，Repository 实现放在 infrastructure。
- MyBatis Mapper、PO/DO 只能存在于 infrastructure，不能向上泄漏。
- Domain Entity 不等于数据库 PO，不能直接拿 PO 当领域模型。
- DTO、Request、Response、Mapper 对象不能进入 domain。
- Service 命名区分 `XxxApplicationService` 和 `XxxDomainService`，避免泛用 `XxxService`。

## 6. 前端开发规范

- API 请求统一走 `src/utils/request.ts`。
- Token 注入、401 刷新、重试逻辑集中处理。
- 页面错误提示统一使用 `useApiError()`，禁止页面里散落 `try/catch + ElMessage`。
- 新增、编辑、删除、审核、导入、导出、打印等按钮必须加权限控制。
- 文案统一走 i18n，常用命名空间包括 `nav.*`、`page.*`、`action.*`、`field.*`、`message.*`、`placeholder.*`。
- 页面结构复用 `page-shell`、`page-header`、`page-title`、`table-card`、`table-pagination`。
- 后端分页页面不要只做前端当前页搜索；大数据搜索应放到后端或搜索索引。

## 7. 后台页面布局规范

- 页面先分类，再套骨架：A 类是列表/筛选页，B 类是单据/表单页。
- 列表页：标题和首个筛选卡片放在 `page-header` 内。
- 单据页：`page-header` 只放标题、面包屑、全局操作按钮，业务表单卡片放在 header 外。
- 不混用列表页和单据页 DOM 结构。
- ERP 列表页搜索区默认一行优先。
- 刷新、搜索、重置、新增等操作按钮固定在搜索卡片第一行右侧。
- 筛选控件宽度不足时，筛选控件整体换行，不能挤压或遮挡操作按钮。
- `el-select`、日期范围、远程选择器必须有明确宽度或最小宽度。
- 改布局后补回归测试，锁住按钮固定右侧、筛选控件不遮挡按钮。

## 8. 单据交互规范

- 列表进入新增、编辑、查看时必须带来源参数，例如 `returnTo`、`from`。
- 返回和关闭标签优先回 `returnTo`。
- 审核流程只确认一次：确认后先静默保存，再调用审核接口。
- 保存成功、审核成功使用统一后续操作弹窗。
- 打印统一使用 `PrintPreviewDialog`，不新开窗口，不跳转当前页。
- 审核、复制、红冲等依赖详情状态的按钮要防闪现：初始化阶段用禁用占位按钮。
- 同一按钮组里只保留一个强主按钮。
- 红冲、删除等风险操作必须二次确认并填写原因。

## 9. 性能与防卡顿规范

- 页面打开分两阶段：首屏 shell 立即渲染，重内容 mounted 后异步加载。
- 首屏不要等待列权限、用户表格设置、复杂下拉、首次列表接口、打印组件。
- 性能敏感入口页不要直接 import 重表格、打印弹窗、复杂弹窗。
- 重内容放到 `DeferredPanel`。
- deferred panel loader 必须缓存 promise，路由预热和页面挂载复用同一个 loader。
- 同组页面可以预热 chunk 和默认第一页数据。
- 当前页面首屏或当前业务动作必须展示的数据，由后端一次性聚合返回，禁止前端或后端 N+1 次请求。
- 重数据可懒加载，实时数据可单独刷新但必须支持批量查询，超大明细必须分页或设置数量上限。
- 性能问题必须看埋点，区分接口慢、chunk 慢、组件 setup 慢、DOM 渲染慢。

## 10. 测试与验证规范

- 权限接入类任务先写失败测试，再实现。
- 页面权限、菜单映射、未映射页面、列权限树等要有回归测试。
- ERP 搜索区布局要有结构测试，保证按钮不被筛选控件挤压。
- 性能敏感页面要测试入口页不直接引入重组件。
- 前端提交前跑类型检查和相关测试。
- 后端提交前至少跑编译检查，重要业务跑专项测试。
- 交付说明必须写清楚验证命令和结果。

## 11. 业务收口与提交规范

- 一个业务一个提交。
- 只暂存本次业务相关文件，避免 `git add .`。
- 根 README 记录业务变更：本次变更、影响范围、验证方式。
- 提交信息使用中文动宾短句，例如“修复库位分页异常”。
- 禁止模糊提交信息，例如“修改一下”“更新代码”“临时提交”。
- 生成文件默认不提交，除非本次业务确实需要。
- 收口脚本按暂存范围执行最小校验：前端改动跑前端检查，后端改动跑后端检查。

## 12. 文档与模板规范

- 根 README 作为项目总入口。
- 前后端分别保留 `DEVELOPMENT_GUIDE.md`。
- 新页面保留前端模板、后端模板、模块模板。
- API、权限、菜单、迁移、部署、回滚都应有独立文档。
- 文档里如果出现规则冲突，迁移到新项目时必须先统一口径，不能原样照搬互相矛盾的旧描述。
- 重大架构决定使用 ADR 记录，例如 DDD、Flyway、权限模型、Nacos、服务通信、防 N+1。
