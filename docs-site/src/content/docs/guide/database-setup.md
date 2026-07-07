---
title: "数据库初始化"
---

本文记录各微服务数据库、账号、Flyway migration 和初始化数据策略。

## 迁移规则

涉及数据库结构、字段、索引、约束、初始化数据或 Flyway migration 的修改，必须先扫描 migration 目录确认当前最高版本号，再按顺序追加新 migration。

## 当前远端 PostgreSQL 拓扑

`duaoyunxuan.com` 服务器当前使用 Docker 运行 PostgreSQL。业务服务连接 PostgreSQL 时使用服务调用端口，不使用 SSH 端口或旧 NAS 连接信息。

| 项 | 当前约定 |
| --- | --- |
| 服务器 | `duaoyunxuan.com` |
| PostgreSQL 对外端口 | `9042` |
| PostgreSQL 容器 | `postgres` |
| 初始管理库 | `nacos_config` |
| 当前服务库 owner | `nacos` |
| SSH 端口 | `9043`，只用于服务器管理，不用于 JDBC |

连接格式：

```text
jdbc:postgresql://duaoyunxuan.com:9042/{database}
```

示例：

```text
jdbc:postgresql://duaoyunxuan.com:9042/xuan_tenant
```

注意：早期对接中曾使用过 `duaoyunxuan.synology.me:5433` 的 PostgreSQL 连接信息。当前 Xuan ERP 远端基础设施以 `duaoyunxuan.com:9042` 为准，不要把旧 NAS 地址写入新的 Nacos datasource 或部署配置。

## 服务数据库清单

当前按“每服务独立数据库”准备 14 个业务数据库。数据库只承载本服务自己的表结构和初始化数据，服务之间不跨库 JOIN。

| 服务 | 数据库名 |
| --- | --- |
| `xuan-gateway` | `xuan_gateway` |
| `xuan-tenant` | `xuan_tenant` |
| `xuan-iam` | `xuan_iam` |
| `xuan-audit` | `xuan_audit` |
| `xuan-product` | `xuan_product` |
| `xuan-party` | `xuan_party` |
| `xuan-warehouse` | `xuan_warehouse` |
| `xuan-inventory` | `xuan_inventory` |
| `xuan-sales` | `xuan_sales` |
| `xuan-procurement` | `xuan_procurement` |
| `xuan-finance` | `xuan_finance` |
| `xuan-document` | `xuan_document` |
| `xuan-manufacturing` | `xuan_manufacturing` |
| `xuan-query` | `xuan_query` |

远端已完成一次库级准备：14 个 `xuan_*` 数据库均已创建，owner / 授权为 `nacos`。表结构不在远端手工创建，仍由各服务 Flyway 执行 `V1`。

## 初始化数据范围

当前 V1 初始化数据只放基础配置和基础字典，不造销售单、采购单、库存流水、收付款等业务单据。

| 服务 | V1 初始化数据 |
| --- | --- |
| `xuan-tenant` | 默认租户、租户配置 |
| `xuan-iam` | 基础权限、一级菜单、默认租户菜单授权 |
| `xuan-product` | 默认分类、常用单位 |
| `xuan-party` | 客户类别、供应商类型、送货方式 |
| `xuan-warehouse` | 默认仓库、默认库位 |
| `xuan-finance` | 结算方式、收款方式、付款方式 |
| `xuan-document` | 销售、采购、退货、收付款、盘点、移库、组装拆分默认打印模板 |
| 其它服务 | V1 不写入业务初始化数据 |

默认初始化数据使用 `tenant_id = 1` 作为默认租户约定。实际租户生命周期仍由 `xuan-tenant` 控制。

## Nacos 数据源待配置

本地 `application-*.yml` 主要通过 Nacos 导入公共配置和服务配置，不直接写死 datasource。服务上线前，需要在 Nacos 对应 dataId 中为每个服务配置独立数据库。

示例：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://duaoyunxuan.com:9042/xuan_sales
    username: nacos
    password: ${XUAN_DB_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration
```

生产环境数据库密码不直接明文写入 Git。确需放入 Nacos 时，必须使用加密能力或由部署平台注入，并保留变更记录和操作人。

## 验证记录

远端库级准备和本地 migration 草稿曾完成以下验证：

```text
SERVICE_DATABASES_EXISTS 14/14
ROLLBACK OK total_statements=1825
```

这些验证只说明 14 个数据库存在，以及 V1 migration 能在 PostgreSQL 事务内执行并回滚；不代表生产环境已经执行 Flyway 建表。



