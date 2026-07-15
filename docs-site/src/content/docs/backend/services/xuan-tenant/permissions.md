---
title: "xuan-tenant 权限文档"
---

本文记录 `xuan-tenant` 的菜单、接口权限、列权限和权限同步规则。权限目录最终都落在 `xuan-iam`，业务服务只维护自己拥有的权限清单。

## 当前实现说明

- 平台统一模板要求所有微服务的基础操作优先使用 `view/create/update/delete`。
- `xuan-tenant` 当前已经落地的 V1 权限码继续保留，避免接口、测试和前端约定反复震荡。
- 启用和停用当前统一收敛到 `tenant:lifecycle`，等平台需要精细化时再受控拆分为 `tenant:enable` 和 `tenant:disable`。
- 配置写操作当前统一使用 `tenant-config:manage`，语义上对应配置编辑能力。

## 菜单与 pageKey

| 项 | 值 |
| --- | --- |
| 菜单 code | `system:tenant` |
| 默认 pageKey | `tenant` |
| 权限前缀 | `tenant` |

## 接口权限

| 权限码 | 动作 | 说明 |
| --- | --- | --- |
| `tenant:view` | 查询租户 | 访问租户列表、详情、生命周期历史和下拉引用数据 |
| `tenant:create` | 创建租户 | 创建租户主档并异步启动首期编排入口，创建成功后立即返回 `PROVISIONING` |
| `tenant:update` | 修改租户 | 修改租户名称、联系人摘要、备注等基础信息 |
| `tenant:lifecycle` | 生命周期操作 | 当前聚合启用、暂停、停用、恢复等动作，对应统一模板里的启用/停用类能力 |
| `tenant:delete` | 删除租户 | 逻辑删除租户，删除前必须完成应用层状态和关联校验 |
| `tenant-plan:view` | 查询套餐 | 查看套餐列表、套餐详情和当前租户套餐 |
| `tenant-plan:manage` | 维护套餐 | 新增、修改、启停套餐定义、额度和功能开关 |
| `tenant-plan:assign` | 分配套餐 | 为租户绑定、升级、降级或取消套餐 |
| `tenant-domain:view` | 查询域名 | 查看租户域名、验证状态和主域名 |
| `tenant-domain:manage` | 维护域名 | 新增、验证、停用、删除租户域名 |
| `tenant-contact:view` | 查询联系人 | 查看管理员、商务、技术、财务等租户联系人 |
| `tenant-contact:manage` | 维护联系人 | 新增、修改、删除联系人和调整主联系人 |
| `tenant-config:view` | 查询配置 | 查看租户配置，敏感配置必须脱敏 |
| `tenant-config:manage` | 维护配置 | 当前聚合配置创建、修改和删除写操作，语义上对应统一模板里的配置编辑能力 |
| `tenant-provision:view` | 查询初始化 | 查看租户初始化任务、步骤、失败原因和重试记录 |
| `tenant-provision:manage` | 管理初始化 | 面向运维动作，只用于重试失败初始化步骤、处理死信 Outbox 事件和人工补偿，不承担创建租户或发起编排入口职责 |
| `tenant-provision:callback` | 初始化回执 | IAM 和业务服务回写初始化步骤成功或失败结果，不授予普通租户管理员 |
| `tenant:export` | 导出 | 导出租户、套餐、域名、联系人、配置或初始化记录，按需启用 |

## 列权限

| pageKey | 字段范围 | 说明 |
| --- | --- | --- |
| `tenant` | 联系人姓名、手机号、邮箱、停用原因、敏感配置摘要 | 默认脱敏，授权后按页面列权限展示 |
| `tenant-contact` | 联系人手机号、邮箱、备注 | 联系人列表页按列权限展示 |
| `tenant-config` | 配置值、敏感标记、加密标记 | `is_sensitive` 或 `is_encrypted` 配置默认不返回明文 |
| `tenant-provision` | 初始化失败原因、Outbox 错误信息 | 运维排障权限可见，普通租户管理员不可见 |

## 同步规则

- 权限清单在代码提交阶段由 CI 扫描和校验。
- `src/main/resources/permissions/` 当前保存本服务的临时 seed 草稿，待 `xuan-iam` seed 机制落地后统一收口。
- 部署前由受控脚本或 migration 同步到 `xuan-iam`。
- 服务启动时只允许做本地清单自检和告警，不建议直接写 IAM 生产库。
- 新增页面必须同时补菜单、路由 meta、接口权限、列权限映射和回归测试。
- 权限职责必须分层：`tenant:create` 负责创建租户并异步启动首期编排，`tenant-provision:view` 负责查看任务/步骤/失败原因，`tenant-provision:manage` 只负责重试、死信和人工补偿等运维动作，`tenant-provision:callback` 只负责服务回写初始化结果。
