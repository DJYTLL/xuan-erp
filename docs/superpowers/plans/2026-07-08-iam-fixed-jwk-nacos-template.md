# IAM Fixed JWK Nacos Template Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `xuan-iam` 增加“未配置固定 JWK 时的启动告警”，并整理 `xuan-iam.yaml` 的 Nacos 固定 JWK 配置模板。

**Architecture:** 保持当前 `xuan.iam.jwt` 属性模型不变，在 `IamSecurityConfiguration` 中对缺失 `signing-jwk-json` 的情况打印明确日志；同时在 Nacos 配置文档中新增 `xuan-iam.yaml` 模板，说明固定 JWK 应如何配置、哪些字段可以入 Nacos、哪些字段必须走加密或密钥管理。

**Tech Stack:** Spring Boot, SLF4J, ApplicationContextRunner, Maven, Markdown

---

### Task 1: 为固定 JWK 缺失场景补失败测试

**Files:**
- Create: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamJwtConfigurationTest.java`

- [ ] 写“未配置 `signing-jwk-json` 时仍能创建 RSAKey 但会输出告警”的测试
- [ ] 写“配置了固定 `signing-jwk-json` 时不输出临时 JWK 告警”的测试
- [ ] 运行定向测试确认先失败

### Task 2: 实现启动告警

**Files:**
- Modify: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/config/IamSecurityConfiguration.java`

- [ ] 在动态生成 JWK 的分支打印清晰告警，说明 token 重启后失效风险
- [ ] 保持现有属性结构和 fallback 行为不变
- [ ] 运行定向测试确认通过

### Task 3: 整理 Nacos 模板文档

**Files:**
- Modify: `docs-site/src/content/docs/guide/nacos-setup.md`
- Modify: `docs-site/src/content/docs/security/iam.md`

- [ ] 在 Nacos 文档中增加 `xuan-iam.yaml` 固定 JWK 配置模板
- [ ] 在 IAM 文档中补充固定 JWK、JWK 发布地址、轮换注意事项
- [ ] 明确 `signing-jwk-json` 属于敏感配置，仓库仅保留模板，不保留真实私钥

### Task 4: 回归验证

**Files:**
- Verify: `backend/xuan-iam`

- [ ] 运行 `mvn -pl backend/xuan-iam -am \"-Dtest=IamJwtConfigurationTest\" \"-Dsurefire.failIfNoSpecifiedTests=false\" test`
- [ ] 运行 `mvn -pl backend/xuan-iam -am test`
