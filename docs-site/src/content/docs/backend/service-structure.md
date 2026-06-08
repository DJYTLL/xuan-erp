---
title: "服务工程结构"
---

Xuan ERP 后端建议采用微服务 monorepo 管理，并以 DDD 作为业务服务的默认设计模式。根目录承载父 POM、公共模块、业务服务、部署脚本和文档站。每个业务服务保持独立启动、独立配置、独立数据库迁移和独立发布。

## 推荐目录

```text
D:\xuan-erp
  docs-site/
  pom.xml
  xuan-common/
  xuan-common-security/
  xuan-common-web/
  xuan-common-mybatis/
  xuan-gateway/
  xuan-iam/
  xuan-tenant/
  xuan-product/
  xuan-party/
  xuan-warehouse/
  xuan-sales/
  xuan-procurement/
  xuan-inventory/
  xuan-manufacturing/
  xuan-finance/
  xuan-document/
  xuan-audit/
  xuan-query/
  deploy/
  scripts/
```

## 父工程职责

- 统一 Java 21。
- 统一 Spring Boot、Spring Cloud、Spring Cloud Alibaba BOM。
- 统一 Maven 插件版本。
- 统一 Checkstyle、测试、打包和 Docker 构建基础配置。
- 不承载具体业务代码。

## 公共模块边界

公共模块只放跨服务稳定复用的基础能力，避免把业务模型放进去形成隐性耦合。

| 模块 | 职责 |
| --- | --- |
| `xuan-common` | 通用异常、返回结构、工具类、常量 |
| `xuan-common-web` | Web 拦截器、分页、请求上下文、TraceId |
| `xuan-common-security` | JWT 解析、当前用户、权限注解、服务间鉴权 |
| `xuan-common-mybatis` | MyBatis Plus 基础配置、租户拦截、审计字段填充 |

商品、客户、库存、销售等业务实体不应进入公共模块。跨服务需要共享业务信息时，优先通过接口 DTO、事件 DTO 或查询服务读模型解决。

## 业务服务内部结构

承载业务复杂度的微服务应按 DDD 分层组织代码：

```text
xuan-product/
  src/main/java/com/xuan/erp/product/
    interfaces/
    application/
    domain/
    infrastructure/
```

| 目录 | 职责 |
| --- | --- |
| `interfaces` | Controller、请求响应 DTO、Assembler |
| `application` | 应用服务、命令、查询、事务编排、权限检查 |
| `domain` | 聚合根、实体、值对象、领域服务、仓储接口、领域事件 |
| `infrastructure` | Mapper、数据库实现、Feign、MQ、配置、第三方适配 |

详细规则见：[DDD 分层与领域建模](/backend/ddd/)。




