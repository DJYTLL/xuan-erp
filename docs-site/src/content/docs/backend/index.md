---
title: "后端开发总览"
---

后端文档用于记录 Xuan ERP 微服务后端工程的统一规范，包括 DDD 分层、工程结构、服务边界、接口契约、权限接入、数据库迁移、配置管理、服务治理和测试要求。

## 后端技术基线

| 组件 | 项目基线 |
| --- | --- |
| JDK | Java 21 LTS |
| Spring Boot | 4.0.6 或同分支最新 4.0.x |
| Spring Cloud | 2025.1.1 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Nacos Server | 3.2.2 |
| LoadBalancer | `spring-cloud-starter-loadbalancer`，由 Spring Cloud BOM 管理 |
| Sentinel | 跟随 Spring Cloud Alibaba BOM |
| Seata | 跟随 Spring Cloud Alibaba BOM，Console 入口端口 `9021`，事务服务端口默认 `8091` |
| RocketMQ | Client 跟随 BOM，Server 使用 5.5.0 |
| Redis | Client 跟随 Spring Boot BOM，Server 使用 8.x |
| Elasticsearch | Client 跟随 Spring Boot BOM，Server 使用 9.x |
| OpenAPI | springdoc-openapi |

详细版本对应见：[版本栈推荐](/architecture/version-stack/)。Sentinel 接入方式见：[Sentinel 准备](/guide/sentinel-setup/)，Seata 接入方式见：[Seata 准备](/guide/seata-setup/)，Redis 与 Elasticsearch 接入方式见：[Redis 与 Elasticsearch 准备](/guide/cache-search-setup/)。

## 后端文档范围

- 服务工程结构：父工程、公共模块、业务服务模块、依赖管理。
- DDD 分层：限界上下文、聚合根、实体、值对象、领域服务、仓储接口、领域事件。
- 服务边界：每个服务的数据所有权、接口范围、事件发布与订阅。
- 接口文档位置：每个服务的 HTTP 接口文档放在服务说明目录下。
- OpenAPI：每个后端服务默认使用 springdoc-openapi 生成 `/v3/api-docs` 和 Swagger UI。
- 接口契约：Controller 入参、出参、错误码、分页、幂等、审计字段。
- 数据库结构文档位置：每个服务的表结构说明放在服务说明目录下，并和 Flyway migration 对应。
- 权限接入：菜单、权限码、列权限、super admin 旁路、IAM 同步。
- 数据库迁移：每个服务独立数据库，使用 Flyway 追加迁移。
- 配置管理：Nacos 配置、环境隔离、密钥管理、灰度配置。
- 服务治理：注册发现、限流熔断、分布式事务、消息事件、缓存、搜索、链路追踪、日志、监控。
- 测试要求：单元测试、集成测试、权限回归测试、契约测试。

## 推荐阅读顺序

1. 先读 [DDD 分层与领域建模](/backend/ddd/)，明确业务代码应该怎么分层。
2. 再读 [服务工程结构](/backend/service-structure/)，明确后端项目应该怎么建。
3. 然后读 [服务文档模板](/backend/service-doc-template/)，以后每个微服务都按这个格式补齐。
4. 接着读 [接口文档位置规范](/backend/api-doc-location/)，明确接口文档放在哪里。
5. 再读 [OpenAPI 与 springdoc-openapi](/backend/openapi-springdoc/)，明确机器可读接口契约。
6. 再读 [接口契约规范](/backend/api-contract/)，统一前后端和服务间调用口径。
7. 然后读 [数据库结构文档规范](/backend/database-doc-location/)，明确表结构文档放在哪里。
8. 接着读 [权限接入规范](/backend/permission-integration/)，确保后端权限不会散。
9. 再读 [Sentinel 准备](/guide/sentinel-setup/)，明确限流、熔断和热点参数保护边界。
10. 接着读 [Seata 准备](/guide/seata-setup/)，明确同步分布式事务的使用边界。
11. 再读 [Redis 与 Elasticsearch 准备](/guide/cache-search-setup/)，明确缓存和搜索索引边界。
12. 最后读 [数据库迁移规范](/backend/database-migration/)，明确 Flyway 和多服务数据归属。




