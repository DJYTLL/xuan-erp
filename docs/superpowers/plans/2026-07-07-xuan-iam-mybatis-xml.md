# Xuan IAM MyBatis XML Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `xuan-iam` 的持久化层从 JDBC 直写 SQL 统一重构为 MyBatis XML + Mapper + Adapter 结构，并补齐中文注释。

**Architecture:** 持久化层按 `entity / assembler / mapper / repository` 拆分，SQL 和 `resultMap` 全部下沉到 `resources/mapper/iam/`。应用层与领域层端口保持不变，Adapter 只负责编排 Mapper 调用与对象转换。

**Tech Stack:** Java 21, Spring Boot, MyBatis, JUnit 5, Mockito, Maven

---

### Task 1: 固化结构契约测试

**Files:**
- Modify: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamDddSkeletonTest.java`
- Modify: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/JdbcIamTenantBootstrapGatewayTest.java`
- Create: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamPersistenceMapperContractTest.java`

- [ ] 将测试改为面向 `Mapper + Adapter` 结构
- [ ] 运行定向测试，确认因新类型和 XML 资源缺失而失败

### Task 2: 迁移 Tenant Bootstrap

**Files:**
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/persistence/entity/IamTenantBootstrapResultRecord.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/persistence/mapper/IamTenantBootstrapMapper.java`
- Create: `backend/xuan-iam/src/main/resources/mapper/iam/IamTenantBootstrapMapper.xml`
- Modify: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/persistence/repository/IamTenantBootstrapGatewayAdapter.java`

- [ ] 让 gateway adapter 通过 mapper 调用数据库函数
- [ ] 运行 bootstrap 定向测试并转绿

### Task 3: 迁移 IamUser

**Files:**
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/persistence/assembler/IamUserPersistenceAssembler.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/persistence/mapper/IamUserPersistenceMapper.java`
- Create: `backend/xuan-iam/src/main/resources/mapper/iam/IamUserPersistenceMapper.xml`
- Modify: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/persistence/repository/IamUserRepositoryAdapter.java`

- [ ] 将用户 CRUD 改为 XML Mapper
- [ ] 跑 `IamUser` 相关测试

### Task 4: 补齐 Role / Permission / Menu / AuthorizationSnapshot 骨架

**Files:**
- Create: 对应 record / assembler / mapper / xml / adapter 文件
- Modify: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/persistence/*`

- [ ] 统一命名、注释和目录风格
- [ ] 让空壳仓储切换到 Mapper 结构

### Task 5: 收尾与验证

**Files:**
- Modify: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/persistence/*/package-info.java`
- Modify: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/*.java`

- [ ] 补齐中文 `package-info`
- [ ] 跑 `mvn -pl xuan-iam -am test`
- [ ] 检查关键文件 diff
