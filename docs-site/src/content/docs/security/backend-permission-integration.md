---
title: "后端权限接入"
---

后端权限接入是权限体系在业务服务中的落地方式。它的目标不是让每个业务服务各自维护一套授权系统，而是让所有接口、按钮、菜单和列权限都能被 IAM 统一管理，同时由业务服务在本地完成必要的访问校验。

本文也是业务服务接入 `xuan-common-security` 的统一配置模板。后续新增 `xuan-product`、`xuan-sales`、`xuan-inventory` 等服务时，优先复制本文配置，再只替换服务名、权限码和页面标识。

## 接入边界

| 组件 | 职责 |
| --- | --- |
| IAM | 保存权限目录、菜单、角色授权、列权限和授权快照 |
| Gateway | 校验登录态，注入可信用户、租户和 TraceId 上下文 |
| 业务服务 | 声明本服务权限点，并在接口和应用服务中执行权限校验 |
| 前端 | 根据授权快照控制菜单、按钮、路由和列展示 |

业务服务只声明“本服务有哪些权限点”，不保存“某个角色拥有哪些权限”。角色授权关系只能归 IAM。

## 新服务最小接入

业务服务接入安全能力时，默认只做三件事：

1. 依赖 `xuan-common-security`。
2. 在本地 `application-dev.yml` / `application-prod.yml` 导入 Nacos 的 `xuan-<service>.yaml`。
3. 在 Nacos 的 `xuan-<service>.yaml` 配置 `xuan.security.jwt.*` 和 `xuan.security.permission.*`。

如果服务没有特殊匿名接口、特殊响应体或多条过滤链，不要复制 `SecurityFilterChain`。`xuan-common-security` 已经提供默认 Servlet 安全链：文档和健康检查放行，其余接口需要认证，并自动接入 `BusinessJwtAuthenticationFilter` 和 `GatewayIdentityAuthenticationFilter`。

只有下面场景才允许服务自定义安全配置：

| 场景 | 示例 | 要求 |
| --- | --- | --- |
| 登录前内部接口 | IAM 登录前查询租户状态 | 只开放明确路径，不能开放整个 `/internal/**` |
| 独立观测接口 | 审计观测或健康诊断接口 | 独立过滤链加 `@Order`，避免影响业务 API |
| 统一业务错误体 | 服务需要返回统一 JSON 错误格式 | 仍复用公共 JWT 和 Gateway 身份过滤器 |
| 非 Servlet 服务 | Gateway WebFlux | 由该服务自己的安全模块负责，不套用 Servlet 模板 |

## Maven 依赖

普通业务服务需要依赖公共安全模块。依赖版本由后端父工程统一管理，业务模块不要单独声明版本。

```xml
<dependency>
    <groupId>com.xuan.erp</groupId>
    <artifactId>xuan-common-security</artifactId>
</dependency>
```

如果业务服务使用 `@PreAuthorize`、`@xuanPermission`、列权限或状态动作权限，不要再新建一套本地权限表达式。

## 配置放在哪里

本地 `application-dev.yml` / `application-prod.yml` 只放服务启动、Nacos 连接和 Nacos 配置导入。真正的业务安全配置放到 Nacos 的 `xuan-<service>.yaml`。

本地文件示例：

```yaml
spring:
  # Spring Boot 配置导入入口。
  config:
    # Nacos 配置导入列表。
    import:
      # 导入基础设施统一配置，例如数据库、Redis、MQ、Seata 地址。
      - optional:nacos:xuan-infra.yaml
      # 导入所有服务共享的公共配置。
      - optional:nacos:xuan-common.yaml
      # 导入当前业务服务自己的配置；新增服务时只替换 xuan-product 为真实服务名。
      - optional:nacos:xuan-product.yaml
  # Spring Cloud 相关配置。
  cloud:
    # Nacos 连接配置。
    nacos:
      # Nacos 用户名；当前项目 dev/prod 本地引导文件保持固定 nacos。
      username: nacos
      # Nacos 密码；当前项目 dev/prod 本地引导文件保持固定 nacos。
      password: nacos
      # Nacos 注册发现配置。
      discovery:
        # Nacos 服务地址；按环境填写真实地址。
        server-addr: duaoyunxuan.top:9041
        # Nacos 命名空间；dev 环境填写 dev，prod 环境填写 prod。
        namespace: dev
        # Nacos 分组；Xuan ERP 后端统一使用 XUAN_ERP_GROUP。
        group: XUAN_ERP_GROUP
      # Nacos 配置中心配置。
      config:
        # Nacos 配置中心地址；通常与注册发现地址一致。
        server-addr: duaoyunxuan.top:9041
        # Nacos 配置命名空间；必须与当前环境一致。
        namespace: dev
        # Nacos 配置分组；Xuan ERP 后端统一使用 XUAN_ERP_GROUP。
        group: XUAN_ERP_GROUP
        # Nacos 配置文件类型；业务服务配置统一使用 yaml。
        file-extension: yaml
```

Nacos `xuan-<service>.yaml` 示例：

```yaml
xuan:
  # Xuan ERP 扩展配置根节点。
  security:
    # 业务服务 JWT/JWK 验签配置；只保存验签参数，不保存 IAM 私钥。
    jwt:
      # 是否启用业务服务 JWT/JWK 自动配置；受保护业务服务默认开启。
      enabled: true
      # JWT 签发方；必须与 xuan-iam 签发 access token 时的 issuer 一致。
      issuer: xuan-iam
      # JWT 受众；普通业务服务在通过 Gateway 接收请求时保持与 IAM 签发受众一致。
      audience: xuan-gateway
      # IAM 服务名；服务已接入 Nacos 时优先用服务发现，不写死主机和端口。
      iam-service-name: xuan-iam
      # IAM JWKS 公钥集路径；会拼接到服务发现解析出的 IAM 实例地址后面。
      jwk-set-path: /.well-known/jwks.json
      # IAM JWKS 固定地址；只有未接入服务发现的本地调试场景才配置，正常 Nacos 环境可删除。
      # jwk-set-uri: http://127.0.0.1:8110/.well-known/jwks.json
      # JWK 本地缓存配置；用于减少业务服务反复访问 IAM 公钥端点。
      cache:
        # JWK 正向缓存 TTL；在这段时间内命中同一个 kid 不再请求 IAM。
        positive-cache-ttl: 10m
        # JWK 陈旧缓存兜底 TTL；IAM 短暂不可用时允许继续使用旧公钥的时间。
        stale-cache-ttl: 30m
        # 未知 kid 负缓存 TTL；避免伪造 kid 的 token 反复触发远程刷新。
        negative-cache-ttl: 30s
        # JWK 后台刷新间隔；用于提前感知 IAM 公钥轮换。
        refresh-interval: 5m
    # 业务服务权限快照配置；用于 @xuanPermission、列权限和状态动作权限。
    permission:
      # 是否启用权限快照远程加载和统一权限表达式；需要接口授权的业务服务默认开启。
      enabled: true
      # IAM 服务名；服务已接入 Nacos 时优先用服务发现访问权限快照接口。
      iam-service-name: xuan-iam
      # IAM 当前用户权限快照路径；业务服务会携带当前 Bearer Token 调用该接口。
      iam-snapshot-path: /api/iam/permissions/current
      # IAM 当前用户权限快照固定地址；只有未接入服务发现的本地调试场景才配置，正常 Nacos 环境可删除。
      # iam-snapshot-uri: http://127.0.0.1:8110/api/iam/permissions/current
      # 权限快照本地缓存配置；缓存 key 包含 tenantId、userId、authVersion。
      cache:
        # 权限快照 TTL；权限版本变化后会自动换 key 并重新加载。
        ttl: 30s
```

配置规则：

- 同时存在服务发现和固定 URI 时，优先使用服务发现。
- Nacos 环境推荐配置 `iam-service-name` 和标准 path，不推荐写固定 IP。
- JWT 里只放轻量身份和基础权限码，不放完整菜单树、角色授权树、列权限模板。
- 权限事实源始终是 IAM，业务服务只读取快照和执行本地判断。
- TTL 使用 Spring Boot Duration 格式，例如 `30s`、`5m`、`PT30S`。

## 后端接入流程

新增一个后端接口或页面时，应同时完成：

1. 在服务权限清单中声明 permission code。
2. 在 Controller 或应用服务入口声明所需权限。
3. 在 IAM 权限目录中同步该权限。
4. 前端路由、菜单 code、permission code、pageKey 保持一致。
5. 如涉及敏感字段，同时声明列权限。
6. 增加权限回归测试，确保无权限用户不能访问接口。

## 权限码关系

权限码使用：

```text
领域:资源:动作
```

如果资源和领域一致，可以简化为：

```text
领域:动作
```

示例：

| 场景 | 权限码 |
| --- | --- |
| 查看商品 | `product:view` |
| 新增商品 | `product:create` |
| 审核销售单 | `sales:audit` |
| 调整库存 | `inventory:stock:adjust` |
| 查看应收明细 | `finance:receivable:view` |

## 请求校验链路

推荐校验链路：

```text
前端路由权限
  -> Gateway 登录态校验
  -> Gateway 注入用户和租户上下文
  -> 业务服务接口权限校验
  -> 应用层资源权限和租户状态校验
  -> 列权限过滤
  -> 审计日志
```

前端隐藏按钮只用于提升体验，不作为安全边界。后端接口必须独立校验权限。

## 业务代码模板

Controller 入口使用统一权限表达式：

```java
@RestController
@RequestMapping("/api/products")
class ProductController {

    @Operation(summary = "分页查询商品")
    @PreAuthorize("@xuanPermission.has('product:view')")
    @GetMapping
    PageResult<ProductResponse> page(ProductPageRequest request) {
        return productQueryService.page(request);
    }

    @Operation(summary = "删除商品")
    @PreAuthorize("@xuanPermission.hasAndCanStateAction('product:delete', 'product', #stateCode, 'delete')")
    @DeleteMapping("/{id}")
    void delete(@PathVariable("id") Long id, @RequestParam String stateCode) {
        productApplicationService.delete(id);
    }
}
```

应用服务中获取当前用户：

```java
CurrentUser currentUser = CurrentUserHolder.required();
Long tenantId = currentUser.tenantId();
Long operatorId = currentUser.userId();
```

列表响应接入列权限：

```java
List<ProductResponse> filteredRows = columnPermissionRecordApplier.apply(
        currentUser,
        accessToken,
        "product-list",
        rows,
        ProductResponse.class);
```

命令入口接入状态动作权限：

```java
stateActionPermissionGuard.requireAllowed("product", currentState, "delete");
```

这些模板只负责安全判断。库存是否足够、单据是否允许流转、金额是否正确等业务不变量仍然放在应用层和领域模型中处理。

## super admin 规则

`super_admin` 可以旁路普通授权判断，但不能旁路基础安全规则：

- Token 必须有效。
- 租户状态必须可用。
- 接口仍然必须声明权限码。
- 敏感操作必须记录审计日志。
- 敏感字段导出仍应保留审计。

## 与后端开发规范的关系

本文从权限体系视角说明后端如何接入 IAM。具体到代码组织、权限清单文件、接口注解、CI/CD 扫描和测试要求，见 [权限接入规范](/backend/permission-integration/)。

## 新业务服务接入检查表

| 检查项 | 要求 |
| --- | --- |
| 依赖 | 服务已依赖 `xuan-common-security`，没有复制 JWT/JWK Provider、权限快照 Provider 或权限表达式 |
| Nacos 导入 | 本地 `application-dev.yml` / `application-prod.yml` 已导入 `optional:nacos:xuan-<service>.yaml` |
| Nacos 安全配置 | `xuan-<service>.yaml` 已配置 `xuan.security.jwt.*` 和 `xuan.security.permission.*` |
| Gateway 路由 | `xuan-gateway` 已配置 `/api/<domain>/**` 到目标服务 |
| IAM 权限目录 | 菜单、接口权限、按钮权限、列权限、状态动作资源已进入 IAM 管理范围 |
| Controller | 每个受保护接口都有 Swagger 注解和 `@PreAuthorize` |
| 当前用户 | 应用服务使用 `CurrentUserHolder`，不从 Header 或 JWT 字符串重复解析用户 |
| 列权限 | 敏感列表、详情和导出使用 `ColumnPermissionRecordApplier` 或同等公共能力 |
| 状态动作 | 状态相关命令使用 `StateActionPermissionGuard` 或 `@xuanPermission.hasAndCanStateAction(...)` |
| 前端 | 路由、菜单、按钮、页面权限、列权限使用 IAM 授权快照，不硬编码角色名 |
| 测试 | 至少覆盖有权限 200、无权限 403、未登录 401、权限快照不可用 503 或等价场景 |

## 重复安全配置检查

业务服务禁止重复实现下面能力：

| 能力 | 统一来源 | 不应重复出现 |
| --- | --- | --- |
| Bearer Token 解析 | `BearerTokenResolver` | 服务内再写 `Authorization` 字符串解析 |
| JWT/JWK 验签 | `JwkJwtTokenParser` + `CachingJwkKeyProvider` | 服务内再写 JWK 拉取、kid 缓存、签名校验 |
| 当前用户上下文 | `CurrentUserHolder` | 服务内从 Header 或 JWT claims 手动拼用户对象 |
| 权限快照加载 | `RemoteIamPermissionSnapshotProvider` + `CachedPermissionSnapshotProvider` | 服务内直接调用 IAM 并自建缓存 |
| 方法权限表达式 | `@xuanPermission` | 服务内新建 `hasPermission` Bean 或角色名判断 |
| 列权限过滤 | `ColumnPermissionRecordApplier` | 每个 Controller 手写字段置空 / 脱敏逻辑 |
| 状态动作权限 | `StateActionPermissionGuard` | 每个服务自行解析 `stateActionRules` |

当前代码里保留的服务自定义安全配置：

| 服务 | 文件 | 是否重复 | 处理意见 |
| --- | --- | --- | --- |
| `xuan-tenant` | `TenantSecurityConfiguration` | 部分重复 | 因为需要开放 IAM 登录前租户状态内部接口，可以暂时保留；后续可把“额外匿名路径”抽成公共配置项后再删除该类 |
| `xuan-audit` | `AuditSecurityConfiguration` | 非普通业务模板 | 该服务有观测接口和审计接口的特殊放行策略，暂不按普通业务服务收敛 |
| `xuan-iam` | `IamSecurityConfiguration` | 不属于业务服务重复配置 | IAM 是登录、Token 签发和权限事实源，保留自有安全配置 |
| `xuan-gateway` | `GatewaySecurityConfiguration` | 不属于业务服务重复配置 | Gateway 是入口验签和路由层，使用 WebFlux 安全链，不能套用 Servlet 业务服务模板 |

新增普通业务服务时，如果只是“文档、健康检查放行，其余接口登录后访问”，不要新增服务私有 `SecurityFilterChain`。
