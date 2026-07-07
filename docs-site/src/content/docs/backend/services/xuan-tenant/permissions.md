---
title: "xuan-tenant 权限文档"
---

本文记录 `xuan-tenant` 的菜单、接口权限、列权限和权限同步规则。权限目录最终都落在 `xuan-iam`，业务服务只维护自己拥有的权限清单。

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
| `tenant:create` | 创建租户 | 创建租户主档并提交初始化任务 |
| `tenant:update` | 修改租户 | 修改租户名称、联系人摘要、备注等基础信息 |
| `tenant:lifecycle` | 生命周期操作 | 启用、暂停、停用、恢复租户，并写入生命周期历史 |
| `tenant:delete` | 删除租户 | 逻辑删除租户，删除前必须完成应用层状态和关联校验 |
| `tenant-plan:view` | 查询套餐 | 查看套餐列表、套餐详情和当前租户套餐 |
| `tenant-plan:manage` | 维护套餐 | 新增、修改、启停套餐定义、额度和功能开关 |
| `tenant-plan:assign` | 分配套餐 | 为租户绑定、升级、降级或取消套餐 |
| `tenant-domain:view` | 查询域名 | 查看租户域名、验证状态和主域名 |
| `tenant-domain:manage` | 维护域名 | 新增、验证、停用、删除租户域名 |
| `tenant-contact:view` | 查询联系人 | 查看管理员、商务、技术、财务等租户联系人 |
| `tenant-contact:manage` | 维护联系人 | 新增、修改、删除联系人和调整主联系人 |
| `tenant-config:view` | 查询配置 | 查看租户配置，敏感配置必须脱敏 |
| `tenant-config:manage` | 维护配置 | 更新租户配置、敏感标记和加密标记 |
| `tenant-provision:view` | 查询初始化 | 查看租户初始化任务、步骤、失败原因和重试记录 |
| `tenant-provision:manage` | 管理初始化 | 重试失败初始化步骤、重试死信 Outbox 事件和人工补偿 |
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
- 部署前由受控脚本或 migration 同步到 `xuan-iam`。
- 服务启动时只允许做本地清单自检和告警，不建议直接写 IAM 生产库。
- 新增页面必须同时补菜单、路由 meta、接口权限、列权限映射和回归测试。
