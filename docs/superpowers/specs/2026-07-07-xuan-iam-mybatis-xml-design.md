# Xuan IAM MyBatis XML Design

## 背景

`xuan-iam` 当前的持久化层同时存在目录层面的 MyBatis 预期和实现层面的 JDBC 直写 SQL。为了让模块结构与后端分层约定一致，本次将 IAM 持久化层统一为传统 MyBatis XML 风格。

## 目标

- 移除 `xuan-iam` 中直接使用 `NamedParameterJdbcTemplate` 的持久化实现。
- 统一 `entity / assembler / mapper / repository` 四类持久化资产。
- SQL 与 `resultMap` 全部下沉到 `src/main/resources/mapper/iam/*.xml`。
- 为新增公开类型补齐中文 Javadoc 和中文 `package-info`。
- 保持应用层与领域层端口稳定，不改 Flyway、不改表结构、不扩业务语义。

## 设计

### 持久化结构

- `infrastructure/persistence/entity/`：定义表结构 record。
- `infrastructure/persistence/assembler/`：负责领域对象与 record 的双向转换。
- `infrastructure/persistence/mapper/`：定义 MyBatis Mapper 接口，仅保留方法签名。
- `src/main/resources/mapper/iam/`：定义 XML Mapper、`resultMap` 和 SQL。
- `infrastructure/persistence/repository/`：保留 Adapter/GatewayAdapter，负责实现端口和调用 Mapper。

### 命名统一

- `JdbcIamUserRepository` -> `IamUserRepositoryAdapter`
- `JdbcIamTenantBootstrapGateway` -> `IamTenantBootstrapGatewayAdapter`
- `JdbcIamRoleRepository` -> `IamRoleRepositoryAdapter`
- `JdbcIamPermissionRepository` -> `IamPermissionRepositoryAdapter`
- `JdbcIamMenuRepository` -> `IamMenuRepositoryAdapter`
- `JdbcIamAuthorizationSnapshotRepository` -> `IamAuthorizationSnapshotRepositoryAdapter`

### 需要补齐的持久化资产

- Entity：
  - `IamUserRecord`
  - `IamRoleRecord`
  - `IamPermissionRecord`
  - `IamMenuRecord`
  - `IamAuthorizationSnapshotRecord`
  - `IamTenantBootstrapResultRecord`
- Assembler：
  - `IamUserPersistenceAssembler`
  - `IamRolePersistenceAssembler`
  - `IamPermissionPersistenceAssembler`
  - `IamMenuPersistenceAssembler`
  - `IamAuthorizationSnapshotPersistenceAssembler`
- Mapper：
  - `IamUserPersistenceMapper`
  - `IamTenantBootstrapMapper`
  - `IamRolePersistenceMapper`
  - `IamPermissionPersistenceMapper`
  - `IamMenuPersistenceMapper`
  - `IamAuthorizationSnapshotPersistenceMapper`
- XML：
  - `IamUserPersistenceMapper.xml`
  - `IamTenantBootstrapMapper.xml`
  - `IamRolePersistenceMapper.xml`
  - `IamPermissionPersistenceMapper.xml`
  - `IamMenuPersistenceMapper.xml`
  - `IamAuthorizationSnapshotPersistenceMapper.xml`

### 行为边界

- `IamTenantBootstrapApplicationService` 继续只依赖 `IamTenantBootstrapGateway`。
- `IamUserApplicationService` 继续只依赖 `IamUserRepository`。
- `RepositoryAdapter / GatewayAdapter` 不再直接写 SQL。
- `Mapper` 面向 record 或基础类型，不直接承担领域对象组装。

## 测试策略

- 先把 `TenantBootstrap` 和 `IamUser` 的测试改成面向 Mapper/Adapter 的结构。
- 更新 `IamDddSkeletonTest`，让结构契约指向新的 Adapter、Mapper、Assembler、Entity。
- 新增 Mapper XML 契约测试，确认 XML 资源存在、命名空间正确并包含关键 SQL。
- 最终执行 `mvn -pl xuan-iam -am test`。

## 非目标

- 不修改 Flyway 历史。
- 不引入新的业务字段或新的对外接口。
- 不顺手改造其他服务模块。
