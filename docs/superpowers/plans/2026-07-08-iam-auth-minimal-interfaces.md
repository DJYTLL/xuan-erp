# IAM Auth Minimal Interfaces Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `xuan-iam` 补齐最小登录、公钥发布、Bearer 鉴权和当前用户接口闭环。

**Architecture:** 保持现有四层结构，在 `xuan-iam` 内新增登录应用服务与 JWT 签发组件；使用 `xuan-common-security` 已有 `BearerTokenResolver`、`JwkJwtTokenParser` 和 `CurrentUser`，通过服务内过滤器把 Bearer Token 解析进 Spring Security 上下文；当前用户接口直接读取认证上下文返回轻量身份信息。

**Tech Stack:** Spring Boot, Spring Security, MyBatis, Nimbus JOSE JWT, JUnit 5, Mockito

---

### Task 1: 增加登录与 Bearer 鉴权失败测试

**Files:**
- Create: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamAuthenticationApplicationServiceTest.java`
- Create: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamBearerTokenAuthenticationFilterTest.java`
- Modify: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamChineseDocumentationTest.java`
- Modify: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamDddSkeletonTest.java`

- [ ] 写登录成功、密码错误、禁用用户的失败/成功用例
- [ ] 运行定向测试，确认新增用例先失败
- [ ] 写 Bearer 过滤器把 `CurrentUser` 放入 `SecurityContext` 的失败用例
- [ ] 运行过滤器测试，确认先失败

### Task 2: 实现 JWT 签发、公钥发布与当前用户接口

**Files:**
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/service/IamAuthenticationApplicationService.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/application/query/IamLoginView.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/controller/IamAuthenticationController.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/dto/IamLoginRequest.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/dto/IamLoginResponse.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/dto/IamCurrentUserResponse.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/interfaces/assembler/IamAuthenticationAssembler.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/config/IamSecurityConfiguration.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/config/IamJwtProperties.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/security/IamAccessTokenIssuer.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/security/IamIssuedAccessToken.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/security/IamBearerTokenAuthenticationFilter.java`

- [ ] 实现登录应用服务，完成密码校验、用户状态校验、登录成功/失败状态更新
- [ ] 实现 RSA/JWK access token 签发与公钥发布
- [ ] 实现登录接口与当前用户接口 DTO、装配器、控制器
- [ ] 实现服务内 `SecurityFilterChain`、`EnableMethodSecurity` 与 Bearer 过滤器接线

### Task 3: 验证与回归

**Files:**
- Verify: `backend/xuan-iam`

- [ ] 运行新增定向测试，确认红绿闭环
- [ ] 运行 `mvn -pl backend/xuan-iam -am test`
- [ ] 检查中文 Javadoc / DTO `@Schema` / DDD 结构测试是否全部通过
