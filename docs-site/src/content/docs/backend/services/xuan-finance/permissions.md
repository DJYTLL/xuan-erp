---
title: "xuan-finance 权限文档"
---

本文记录 `xuan-finance` 的菜单、接口权限、列权限和权限同步规则。权限目录最终都落在 `xuan-iam`，业务服务只维护自己拥有的权限清单。

## 菜单与 pageKey

| 项 | 值 |
| --- | --- |
| 菜单 code | `finance:settlement` |
| 默认 pageKey | `finance` |
| 权限前缀 | `finance` |

## 接口权限

| 权限码 | 动作 | 说明 |
| --- | --- | --- |
| `finance:view` | 查询 | 访问列表、详情和下拉引用数据 |
| `finance:create` | 新增 | 创建业务数据 |
| `finance:update` | 修改 | 修改业务数据 |
| `finance:delete` | 删除 | 删除、作废或停用业务数据 |
| `finance:audit` | 审核 | 审核、反审核、红冲等强业务动作，按需启用 |
| `finance:export` | 导出 | 导出列表或明细数据，按需启用 |

## 列权限

| pageKey | 字段范围 | 说明 |
| --- | --- | --- |
| `finance` | 账户、金额、成本、利润、核销明细等敏感列 | 具体字段开发时按页面补齐 |

## 同步规则

- 权限清单在代码提交阶段由 CI 扫描和校验。
- 部署前由受控脚本或 migration 同步到 `xuan-iam`。
- 服务启动时只允许做本地清单自检和告警，不建议直接写 IAM 生产库。
- 新增页面必须同时补菜单、路由 meta、接口权限、列权限映射和回归测试。
