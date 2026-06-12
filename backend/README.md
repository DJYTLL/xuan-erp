# Xuan ERP Backend

Xuan ERP 后端代码目录，采用 Maven 多模块和 DDD 分层组织。

## 目录定位

```text
D:\xuan-erp
  docs-site/
  backend/
```

`backend` 只放后端代码、后端部署脚本和后端构建脚本。前端后续可在 `D:\xuan-erp` 下创建独立目录。

## 模块结构

公共模块：

- `xuan-common`
- `xuan-common-web`
- `xuan-common-security`
- `xuan-common-mybatis`

服务模块：

- `xuan-gateway`
- `xuan-iam`
- `xuan-tenant`
- `xuan-product`
- `xuan-party`
- `xuan-warehouse`
- `xuan-sales`
- `xuan-procurement`
- `xuan-inventory`
- `xuan-manufacturing`
- `xuan-finance`
- `xuan-document`
- `xuan-audit`
- `xuan-query`

## DDD 分层

业务服务默认按四层组织：

```text
interfaces/
application/
domain/
infrastructure/
```

- `interfaces`：Controller、Request DTO、Response DTO、参数校验、权限注解、协议适配。
- `application`：ApplicationService、Command、Query、事务边界、幂等、审计、用例编排。
- `domain`：Aggregate、Domain Entity、Value Object、Domain Service、Domain Event、Repository 接口、业务规则。
- `infrastructure`：Repository 实现、MyBatis Mapper、PO/DO、外部服务 Client、MQ、缓存、文件存储。

## 构建

```powershell
mvn clean compile
```

后端基线是 Java 21。若本机 Maven 默认使用旧 JDK，需要先切换 `JAVA_HOME`。
