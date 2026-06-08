---
title: "接口文档位置规范"
---

接口文档用于记录前端可调用接口、内部服务接口、管理接口和事件相关接口。它不是 Controller 代码的简单复制，而是前后端、服务间调用、测试和联调的共同契约。

## 存放位置

接口文档按服务放在：

```text
docs-site/src/content/docs/backend/services/<service-name>/api.md
```

示例：

```text
docs-site/src/content/docs/backend/services/xuan-product/api.md
docs-site/src/content/docs/backend/services/xuan-sales/api.md
docs-site/src/content/docs/backend/services/xuan-inventory/api.md
```

服务总览仍放在：

```text
docs-site/src/content/docs/backend/services/<service-name>/index.md
```

## 推荐目录

```text
backend/services/xuan-product/
  index.md
  api.md
  database.md
  events.md
  permissions.md
```

| 文件 | 内容 |
| --- | --- |
| `index.md` | 服务定位、限界上下文、领域模型、数据所有权总览 |
| `api.md` | HTTP 接口文档，包括前端接口和内部接口 |
| `database.md` | 数据库结构说明，包括表、字段、索引、约束 |
| `events.md` | 发布事件、订阅事件、Topic、幂等键 |
| `permissions.md` | 菜单、接口权限、列权限、权限同步规则 |

## 接口文档格式

每个接口至少记录：

| 项 | 说明 |
| --- | --- |
| 接口名称 | 用业务语言描述用途 |
| 方法与路径 | 例如 `GET /api/products` |
| 调用方 | 前端、内部服务、定时任务、运维后台 |
| 权限码 | 例如 `product:product:view` |
| 租户要求 | 是否需要 tenantId、是否允许平台级调用 |
| 请求参数 | Query、Path、Body、Header |
| 响应结构 | 成功响应、分页结构、关键字段说明 |
| 错误码 | 业务错误码和触发条件 |
| 幂等规则 | 写接口必须说明幂等键或状态机约束 |
| 审计要求 | 是否记录操作日志、删除原因、审核人 |

## 示例

```md
## 查询商品列表

| 项 | 内容 |
| --- | --- |
| 方法 | GET |
| 路径 | /api/products |
| 调用方 | Vue 管理端 |
| 权限码 | product:product:view |
| 租户要求 | 必须在租户上下文中调用 |

### 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| keyword | Query | string | 否 | 商品编码、名称、拼音码 |
| pageNum | Query | number | 是 | 页码 |
| pageSize | Query | number | 是 | 每页数量 |

### 响应字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | string | 商品 ID |
| code | string | 商品编码 |
| name | string | 商品名称 |
```

## 与 OpenAPI 的关系

后续可以从代码生成 OpenAPI/Swagger，但 Markdown 文档仍保留业务语义、权限、租户、幂等、审计和领域规则说明。生成文档解决“接口长什么样”，Markdown 文档解决“接口为什么这样设计、如何安全调用”。
