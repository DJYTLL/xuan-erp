# Audit Observability Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build two System Settings pages, Interface Latency and SQL Rankings, backed by `xuan-audit`.

**Architecture:** Frontend pages call Xuan ERP APIs only. `xuan-audit` proxies SkyWalking OAP HTTP for interface trace data and queries PostgreSQL `pg_stat_statements` for SQL rankings. IAM owns menu and permission seed data through a new append-only migration.

**Tech Stack:** Spring Boot 4, Java 21, `JdbcTemplate`, `RestClient`, Vue 3, Element Plus, Vite.

---

### Task 1: Backend Query Contracts

**Files:**
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/application/query/InterfaceTraceQuery.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/application/query/InterfaceTraceSummary.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/application/query/InterfaceTraceDetail.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/application/query/InterfaceSpanDetail.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/application/query/SqlRankingQuery.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/application/query/SqlRankingEntry.java`
- Test: `backend/xuan-audit/src/test/java/com/xuan/erp/audit/AuditObservabilityQueryContractTest.java`

- [ ] Write contract tests for allowed SQL ranking sort values and default database selection.
- [ ] Run `mvn -pl xuan-audit -Dtest=AuditObservabilityQueryContractTest test` and verify RED.
- [ ] Add the query records and validation helpers.
- [ ] Re-run the same test and verify GREEN.

### Task 2: Backend Adapters and Controller

**Files:**
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/infrastructure/config/AuditObservabilityProperties.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/infrastructure/rpc/SkyWalkingOapClient.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/infrastructure/persistence/repository/PgStatStatementsRepository.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/application/service/ObservabilityQueryApplicationService.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/interfaces/controller/ObservabilityQueryController.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/interfaces/dto/InterfaceTraceQueryRequest.java`
- Create: `backend/xuan-audit/src/main/java/com/xuan/erp/audit/interfaces/dto/SqlRankingQueryRequest.java`
- Modify: `backend/xuan-audit/src/main/resources/application.yml`
- Test: `backend/xuan-audit/src/test/java/com/xuan/erp/audit/ObservabilityQueryApplicationServiceTest.java`

- [ ] Write application service tests with fake OAP and SQL repositories.
- [ ] Run `mvn -pl xuan-audit -Dtest=ObservabilityQueryApplicationServiceTest test` and verify RED.
- [ ] Implement OAP GraphQL client, SQL repository, properties, service, and controller.
- [ ] Re-run the same test and verify GREEN.

### Task 3: IAM Menu and Permission Seeds

**Files:**
- Create: `backend/xuan-iam/src/main/resources/db/migration/V9__add_audit_observability_menus.sql`
- Modify: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamMigrationContractTest.java`

- [ ] Extend the migration contract test to require V9 audit observability menus and permissions.
- [ ] Run `mvn -pl xuan-iam -Dtest=IamMigrationContractTest test` and verify RED.
- [ ] Add V9 with `audit:interface-cost:view`, `audit:sql-ranking:view`, `接口耗时`, and `SQL 排名`.
- [ ] Re-run the migration test and verify GREEN.

### Task 4: Frontend API, Routes, and Pages

**Files:**
- Create: `frontend/src/api/observability.ts`
- Create: `frontend/src/types/observability.ts`
- Create: `frontend/src/views/InterfaceCostView.vue`
- Create: `frontend/src/views/SqlRankingView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/config/navigation.ts`
- Modify: `frontend/src/i18n/messages.ts`
- Modify: `frontend/scripts/verify-navigation-routes.mjs`

- [ ] Add route verification expectations for both new system paths.
- [ ] Run `npm run verify:navigation-routes` from `frontend` and verify RED.
- [ ] Add frontend API/types, routes, navigation metadata, i18n, and the two pages.
- [ ] Re-run `npm run verify:navigation-routes` and verify GREEN.

### Task 5: Verification

**Files:**
- No new production files.

- [ ] Run `mvn -pl xuan-audit -am test`.
- [ ] Run `mvn -pl xuan-iam -Dtest=IamMigrationContractTest test`.
- [ ] Run `npm run verify:navigation-routes` from `frontend`.
- [ ] Run `npm run build` from `frontend` if dependencies are available.
