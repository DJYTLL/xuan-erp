# RocketMQ Example Producers And Consumers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `xuan-tenant` 和 `xuan-iam` 补齐最小示例 Producer / Consumer，并验证它们能基于 `xuan-common-mq` 被装配。

**Architecture:** 两个业务模块只增加最薄的一层 `infrastructure/mq` 示例类，不引入真实消费逻辑，也不直连底层 RocketMQ API 到业务层。示例 Producer 负责从公共配置里取 topic 并通过 `RocketMqMessageSender` 发送统一消息；示例 Consumer 只保留一个最小处理入口，演示消息落点。

**Tech Stack:** Java 21, Maven, Spring Boot 4, JUnit 6, AssertJ, ApplicationContextRunner, xuan-common-mq

---

### Task 1: 扩展公共配置以承载示例 topic

**Files:**
- Modify: `backend/xuan-common-mq/src/main/java/com/xuan/erp/common/mq/config/XuanRocketMqProperties.java`
- Modify: `backend/xuan-common-mq/src/test/java/com/xuan/erp/common/mq/config/XuanRocketMqPropertiesTest.java`

- [ ] **Step 1: 先写失败测试，断言 tenant/iam topic 可绑定**

Run: `mvn -pl xuan-common-mq -Dtest=XuanRocketMqPropertiesTest test`
Expected: FAIL，提示 topic 相关属性或 getter 缺失。

- [ ] **Step 2: 最小实现 topics 配置对象**

目标：
- `xuan.rocketmq.topics.tenant-events`
- `xuan.rocketmq.topics.iam-events`

- [ ] **Step 3: 再跑测试确认转绿**

Run: `mvn -pl xuan-common-mq -Dtest=XuanRocketMqPropertiesTest test`
Expected: PASS

### Task 2: 为 xuan-tenant 增加示例 Producer / Consumer 与装配测试

**Files:**
- Modify: `backend/xuan-tenant/pom.xml`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/infrastructure/mq/TenantEventProducer.java`
- Create: `backend/xuan-tenant/src/main/java/com/xuan/erp/tenant/infrastructure/mq/TenantEventConsumer.java`
- Create: `backend/xuan-tenant/src/test/java/com/xuan/erp/tenant/TenantRocketMqExampleSpringContextTest.java`

- [ ] **Step 1: 先写失败测试，要求 Producer / Consumer 可装配**

Run: `mvn -pl xuan-tenant -Dtest=TenantRocketMqExampleSpringContextTest test`
Expected: FAIL，提示 `xuan-common-mq` 依赖或示例类缺失。

- [ ] **Step 2: 最小实现依赖与示例类**

约束：
- `TenantEventProducer` 依赖 `RocketMqMessageSender` 与 `XuanRocketMqProperties`
- `TenantEventConsumer` 只提供最小消费入口，不接真实 RocketMQ listener
- 代码注释使用中文

- [ ] **Step 3: 再跑模块定向测试确认通过**

Run: `mvn -pl xuan-tenant -Dtest=TenantRocketMqExampleSpringContextTest test`
Expected: PASS

### Task 3: 为 xuan-iam 增加示例 Producer / Consumer 与装配测试

**Files:**
- Modify: `backend/xuan-iam/pom.xml`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/mq/IamEventProducer.java`
- Create: `backend/xuan-iam/src/main/java/com/xuan/erp/iam/infrastructure/mq/IamEventConsumer.java`
- Create: `backend/xuan-iam/src/test/java/com/xuan/erp/iam/IamRocketMqExampleSpringContextTest.java`

- [ ] **Step 1: 先写失败测试，要求 Producer / Consumer 可装配**

Run: `mvn -pl xuan-iam -Dtest=IamRocketMqExampleSpringContextTest test`
Expected: FAIL，提示 `xuan-common-mq` 依赖或示例类缺失。

- [ ] **Step 2: 最小实现依赖与示例类**

约束：
- `IamEventProducer` 依赖 `RocketMqMessageSender` 与 `XuanRocketMqProperties`
- `IamEventConsumer` 只提供最小消费入口，不接真实 RocketMQ listener
- 代码注释使用中文

- [ ] **Step 3: 再跑模块定向测试确认通过**

Run: `mvn -pl xuan-iam -Dtest=IamRocketMqExampleSpringContextTest test`
Expected: PASS

### Task 4: 补示例 YAML 并做聚合验证

**Files:**
- Modify: `backend/xuan-tenant/src/main/resources/application.yml`
- Modify: `backend/xuan-iam/src/main/resources/application.yml`

- [ ] **Step 1: 为两个服务补最小示例配置**

目标：
- `xuan.rocketmq.enabled`
- `xuan.rocketmq.name-server`
- `xuan.rocketmq.producer.group`
- `xuan.rocketmq.topics.tenant-events`
- `xuan.rocketmq.topics.iam-events`

- [ ] **Step 2: 跑 tenant 与 iam 的聚合验证**

Run: `mvn -pl xuan-tenant,xuan-iam -am test`
Expected: BUILD SUCCESS

- [ ] **Step 3: 核对范围**

Checklist:
- `xuan-tenant` 已有示例 Producer / Consumer
- `xuan-iam` 已有示例 Producer / Consumer
- 两个模块都依赖 `xuan-common-mq`
- 示例 YAML 已补齐
- 未引入真实 RocketMQ listener 和联调逻辑
