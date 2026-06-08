---
title: "xuan-iam"
---

xuan-iam 是 Xuan ERP 的IAM 权限中心，主要负责：登录、用户、角色、权限、菜单、列权限、用户租户关系、授权快照。

## 服务定位

| 项 | 内容 |
| --- | --- |
| 服务名 | `xuan-iam` |
| 职责 | 登录、用户、角色、权限、菜单、列权限、用户租户关系、授权快照 |
| 权限前缀 | `iam` |
| 数据库/Schema | `xuan_iam` |
| 事件 Topic | `xuan-iam-event` |
| Java 包名 | `com.xuan.erp.iam` |

## 限界上下文

身份与访问控制上下文。IAM 是权限、菜单、角色、列权限和授权关系的唯一数据归属服务。

## 领域模型

| 类型 | 名称 | 说明 |
| --- | --- | --- |
| 聚合根 | User, Role, PermissionCatalog, MenuCatalog | 本服务内部一致性边界 |
| 值对象 | UserId, RoleId, PermissionCode, PageKey | 表达业务含义，不直接使用原始字符串或数字散落在业务代码中 |
| 领域服务 | AuthorizationPolicy, PermissionCatalogSyncPolicy | 处理无法自然归属到单个实体的领域规则 |

## 数据所有权

本服务只直接读写 `xuan_iam` 下属于自身的数据表。跨服务需要的数据通过接口、领域事件或 `xuan-query` 读模型获取，不直接跨库 Join。

详细数据库结构见：[数据库结构](./database/)。

## 接口文档

详细接口见：[接口文档](./api/)。

## 权限清单

详细权限见：[权限文档](./permissions/)。

## 事件

详细事件见：[事件文档](./events/)。
