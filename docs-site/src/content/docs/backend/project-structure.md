---
title: "项目根目录与 Maven 结构"
---

Xuan ERP 采用后端微服务 monorepo。根目录承载 Maven 父工程、公共模块、业务服务、部署脚本和文档站。文档站放在 `docs-site`，后端服务与文档站同级。

## 根目录结构

```text
D:\xuan-erp
  docs-site/
  pom.xml
  xuan-common/
  xuan-common-web/
  xuan-common-security/
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

## 父 POM 职责

根目录 `pom.xml` 只负责版本、插件、模块和构建规范，不承载业务代码。

```xml
<modules>
    <module>xuan-common</module>
    <module>xuan-common-web</module>
    <module>xuan-common-security</module>
    <module>xuan-common-mybatis</module>
    <module>xuan-gateway</module>
    <module>xuan-iam</module>
    <module>xuan-tenant</module>
    <module>xuan-product</module>
    <module>xuan-party</module>
    <module>xuan-warehouse</module>
    <module>xuan-sales</module>
    <module>xuan-procurement</module>
    <module>xuan-inventory</module>
    <module>xuan-manufacturing</module>
    <module>xuan-finance</module>
    <module>xuan-document</module>
    <module>xuan-audit</module>
    <module>xuan-query</module>
</modules>
```

## 公共模块边界

| 模块 | 说明 |
| --- | --- |
| `xuan-common` | 通用异常、返回对象、基础工具 |
| `xuan-common-web` | Web 上下文、TraceId、分页、拦截器 |
| `xuan-common-security` | JWT、当前用户、权限注解、服务间鉴权 |
| `xuan-common-mybatis` | MyBatis 配置、审计字段、租户拦截 |

公共模块不能放商品、销售、库存、客户等业务实体。跨服务共享业务信息时使用接口 DTO、事件 DTO 或 `xuan-query` 读模型。

## 服务模块结构

每个业务服务内部按 DDD 分层：

```text
xuan-sales/
  pom.xml
  src/main/java/com/xuan/erp/sales/
    interfaces/
    application/
    domain/
    infrastructure/
  src/main/resources/
    application.yaml
    db/migration/
```

## 部署目录

```text
deploy/
  docker/
  k8s/
  nacos/
  sentinel/
  rocketmq/
```

## 脚本目录

```text
scripts/
  build/
  deploy/
  permissions/
  migrations/
```
