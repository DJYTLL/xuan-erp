# xuan-gateway 基于服务发现的 JWKS 验签设计

## 背景

当前 `xuan-iam` 已经具备以下能力：

- 使用 RSA 私钥签发访问令牌。
- 在 `/.well-known/jwks.json` 发布公钥集合。
- 为访问令牌写入 `kid`。

但 `xuan-gateway` 侧还没有真正接上这条认证链路。现在网关没有从 IAM 拉取 JWKS 做验签，也没有统一的 Bearer Token 认证过滤逻辑，因此“请求先过 gateway，再进入 iam / 业务服务”的入口认证仍不完整。

本次要补齐的，就是网关这一段。

## 目标

- 让 `xuan-gateway` 通过服务发现定位 `xuan-iam`。
- 让 `xuan-gateway` 从 `xuan-iam` 的 `/.well-known/jwks.json` 拉取公钥集合并缓存。
- 让 `xuan-gateway` 使用 JWKS 校验 IAM 签发的访问令牌，而不是持有或配置私钥。
- 保持健康检查等基础端点可匿名访问。

## 本次范围

- 仅在 `xuan-gateway` 内新增 JWKS 获取、缓存、刷新和认证过滤逻辑。
- 复用 `xuan-common-security` 现有的 JWT 解析与 `CurrentUser` 映射能力。
- 增加针对 JWKS 拉取、缓存刷新和网关认证的测试。

## 明确不做

- 本次不改 `xuan-iam` 的签发逻辑与 JWK 发布协议。
- 本次不把“远端 JWKS 拉取能力”抽象成 `xuan-common-security` 的通用基础设施。
- 本次不引入数据库结构、Flyway migration 或初始化数据变更。
- 本次不扩展网关的细粒度鉴权策略，只先完成“是否为有效访问令牌”的入口认证。

## 设计决策

### 1. 通过服务发现定位 IAM

- 使用 Spring Cloud 的 `DiscoveryClient` 查找服务名 `xuan-iam`。
- 取可用实例的基础地址，拼接 `/.well-known/jwks.json` 发起请求。
- 这一层只负责“找到 IAM 并取回 JWKS”，不承载令牌解析逻辑。

原因：

- 用户已确认本次采用服务发现，而不是把 IAM 的固定地址硬编码进网关配置。
- `xuan-gateway` 已经接入 Nacos Discovery，这条路径与当前基础设施方向一致。

### 2. 在 gateway 内实现最小 JWKS Provider

建议新增两层职责：

- `GatewayJwkSetFetcher`
- `CachingGatewayJwkProvider`

职责划分：

- `GatewayJwkSetFetcher`：基于服务发现和 `WebClient` 向 IAM 拉取 `JWKSet`。
- `CachingGatewayJwkProvider`：在内存中缓存最近一次成功拉取的 `JWKSet`，并处理刷新策略。

这样可以把“远端访问”与“本地缓存”拆开，便于测试，也避免把逻辑直接堆进认证过滤器。

### 3. 认证时复用现有 `JwkJwtTokenParser`

- `xuan-common-security` 已经有 `JwkJwtTokenParser`，可完成：
  - `alg`
  - `kid`
  - 签名
  - `iss`
  - `aud`
  - `exp`
  - `tokenType`
- `xuan-gateway` 不重新实现 JWT 验签逻辑，只负责在认证时为解析器提供当前可用的 `JWKSet`。

这样可以保持网关和 IAM / 业务服务后续的令牌校验规则一致，避免出现多套 JWT 语义。

### 4. 未命中 `kid` 时允许一次刷新重试

缓存策略先保持最小可用：

- 网关启动后，第一次收到需要认证的请求时再懒加载 JWKS。
- 若本地缓存存在，优先用缓存验签。
- 若因 `kid` 未命中导致验签失败，立即触发一次 JWKS 刷新并重试一次解析。
- 若刷新后仍未命中或刷新失败，则返回 `401 Unauthorized`。

这样可以覆盖最关键的密钥轮换场景，同时把复杂度控制在当前阶段需要的范围内。

### 5. 以 WebFlux 过滤器接入 Spring Security

建议新增：

- `GatewayBearerAuthenticationWebFilter`
- `GatewaySecurityConfiguration`
- `GatewaySecurityProperties`

职责说明：

- `GatewayBearerAuthenticationWebFilter`
  - 解析 Bearer Token
  - 调用 JWKS Provider + `JwkJwtTokenParser`
  - 将 `CurrentUser` 转成认证上下文
- `GatewaySecurityConfiguration`
  - 注册安全过滤链
  - 放行 `/actuator/health`、`/actuator/info`
  - 要求其他业务路由默认经过认证
- `GatewaySecurityProperties`
  - 绑定 issuer、audience、缓存刷新相关参数

## 组件草图

### `GatewayJwkSetFetcher`

输入：

- 服务名，默认 `xuan-iam`

输出：

- `JWKSet`

行为：

- 通过 `DiscoveryClient` 获取实例列表。
- 选择一个可用实例，拼接 `/.well-known/jwks.json`。
- 使用 `WebClient` 拉取并反序列化为 `JWKSet`。

### `CachingGatewayJwkProvider`

输入：

- `GatewayJwkSetFetcher`

输出：

- 当前可用 `JWKSet`

行为：

- 维护内存缓存。
- 提供“读取当前缓存或首次加载”的能力。
- 提供“强制刷新”的能力。
- 在未知 `kid` 场景下供认证层触发一次刷新。

### `GatewayBearerAuthenticationWebFilter`

行为：

- 从请求头提取 Bearer Token。
- 用当前缓存构造 `JwkJwtTokenParser` 并尝试解析。
- 若解析失败原因为未知 `kid`，触发 Provider 刷新后再重试一次。
- 解析成功后，把 `CurrentUser` 写入 Reactor 安全上下文。
- 解析失败时返回 `401`。

## 请求链路

1. 客户端携带 IAM 签发的访问令牌请求 `xuan-gateway`。
2. 网关过滤器解析 Bearer Token。
3. 网关从本地缓存取 `JWKSet`，必要时通过服务发现向 `xuan-iam` 拉取 `/.well-known/jwks.json`。
4. 网关使用 `JwkJwtTokenParser` 校验签名与声明。
5. 校验通过后，请求继续路由到下游服务。
6. 校验失败则由网关直接返回 `401`。

## 配置建议

建议在 `xuan-gateway` 增加独立安全配置前缀，例如：

- `xuan.gateway.security.enabled`
- `xuan.gateway.security.issuer`
- `xuan.gateway.security.audience`
- `xuan.gateway.security.iam-service-name`

当前阶段不建议把过多缓存细节暴露成配置项，先保留最少必要参数。

## 测试策略

遵循先测试、后实现，测试重点放在三层：

### 1. `GatewayJwkSetFetcher` 单元测试

覆盖：

- 能从服务发现结果拼出 IAM JWKS 地址。
- 能正确解析 `/.well-known/jwks.json` 返回体。
- 无可用实例时给出明确失败。

### 2. `CachingGatewayJwkProvider` 单元测试

覆盖：

- 首次访问触发拉取。
- 已有缓存时直接复用。
- 未知 `kid` 触发一次刷新。
- 刷新失败时返回认证失败结果。

### 3. 网关安全集成测试

覆盖：

- 无 Token 请求受保护路由返回 `401`。
- 合法访问令牌可通过认证。
- 未知 `kid` 在刷新后仍不存在时返回 `401`。
- 过期令牌返回 `401`。
- `/actuator/health` 与 `/actuator/info` 可匿名访问。

## 验收标准

- `xuan-gateway` 能通过服务发现找到 `xuan-iam`。
- `xuan-gateway` 能拉取并缓存 `/.well-known/jwks.json`。
- IAM 签发的合法访问令牌可通过网关认证。
- 未知 `kid` 会触发一次刷新重试。
- 健康检查端点保持匿名可访问。
- 网关自身不再持有或依赖 IAM 私钥。

## 实现边界

本次实现预计会落在以下位置：

- `backend/xuan-gateway/src/main/java/.../config`
- `backend/xuan-gateway/src/main/java/.../security`
- `backend/xuan-gateway/src/test/java/...`
- `backend/xuan-gateway/src/main/resources/application*.yml`

不涉及：

- `xuan-iam` 业务逻辑改造
- `xuan-common-security` 公共模块重构
- 数据库或 Flyway 迁移变更
