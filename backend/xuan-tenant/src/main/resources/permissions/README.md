# xuan-tenant permissions

本目录用于暂存 `xuan-tenant` 的菜单、权限和列权限 seed 草稿，等 `xuan-iam` 的统一 seed 机制落地后再统一收口。当前阶段这里不是运行时自动注册入口，而是本服务的权限边界说明和受控同步输入。

## 平台统一模板

所有微服务后续的基础操作默认使用同一套权限模板：

- 主资源基础动作：`<domain>:view`、`<domain>:create`、`<domain>:update`、`<domain>:delete`
- 生命周期扩展动作：按需使用 `<domain>:enable`、`<domain>:disable`
- 配置类资源：默认使用 `<domain>-config:view` 和 `<domain>-config:manage`
- 审核、导出、导入、打印等扩展动作按业务需要追加，但不能丢失基础模板

## xuan-tenant 当前映射

`xuan-tenant` 已统一使用平台基础权限模板，历史 `tenant:lifecycle` 仅保留为 IAM 迁移兼容来源，不再作为接口和前端正式权限码：

| 统一模板语义 | 当前权限码 | 说明 |
| --- | --- | --- |
| `tenant:view` | `tenant:view` | 租户查询能力 |
| `tenant:create` | `tenant:create` | 租户创建能力 |
| `tenant:update` | `tenant:update` | 租户编辑能力 |
| `tenant-domain:view` | `tenant-domain:view` | 域名读取能力 |
| `tenant-domain:manage` | `tenant-domain:manage` | 当前聚合域名创建、编辑、验证和删除写操作 |
| `tenant-contact:view` | `tenant-contact:view` | 联系人读取能力 |
| `tenant-contact:manage` | `tenant-contact:manage` | 当前聚合联系人创建、编辑和删除写操作 |
| `tenant:delete` | `tenant:delete` | 租户删除能力 |
| `tenant:enable` | `tenant:enable` | 租户启用、恢复能力 |
| `tenant:disable` | `tenant:disable` | 租户暂停、停用、冻结能力 |
| `tenant-config:view` | `tenant-config:view` | 配置读取能力 |
| `tenant-config:edit` | `tenant-config:manage` | 当前用 `manage` 聚合配置写操作，后续如平台统一改成 `edit`，需要走受控演进 |
| `tenant-plan:view` | `tenant-plan:view` | 套餐和套餐分配查询能力 |
| `tenant-plan:update` | `tenant-plan:manage` | 当前用 `manage` 聚合套餐创建、编辑、启停和删除 |
| `tenant-plan:assign` | `tenant-plan:assign` | 套餐分配关系维护能力 |

## 目录说明

- `menus.yaml`：菜单 seed 草稿
- `permissions.yaml`：权限 seed 草稿
- `columns.yaml`：列权限 seed 草稿

这些文件由业务服务维护“本服务有哪些权限点”，不保存角色授权关系；角色、菜单授权和列授权关系仍然只归 `xuan-iam`。
