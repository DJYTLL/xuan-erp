# Xuan Common MQ Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为后端工程补齐 `xuan-common-mq` 公共模块、父 POM 注册、RocketMQ 统一依赖与 `xuan.rocketmq` 基础配置绑定。

**Architecture:** 本次只交付最小公共骨架，不实现发送器、消费者或自动装配。依赖统一收口在父 POM 与 `xuan-common-mq` 模块，配置统一落到 `XuanRocketMqProperties`，后续业务模块只需要依赖该模块并覆盖配置即可。

**Tech Stack:** Java 21, Maven, Spring Boot 4, Spring Cloud Alibaba BOM, JUnit 5, AssertJ

---

### Task 1: 建立模块骨架并先写失败测试

**Files:**
- Create: `backend/xuan-common-mq/pom.xml`
- Create: `backend/xuan-common-mq/src/test/java/com/xuan/erp/common/mq/config/XuanRocketMqPropertiesTest.java`
- Modify: `backend/pom.xml`

- [ ] **Step 1: 新增模块目录与测试文件，先让测试引用尚未实现的配置类**

```java
package com.xuan.erp.common.mq.config;

import org.junit.jupiter.api.Test;

class XuanRocketMqPropertiesTest {

    @Test
    void bindsBasicRocketMqProperties() {
        new XuanRocketMqProperties();
    }
}
```

- [ ] **Step 2: 运行测试确认当前失败**

Run: `mvn -pl xuan-common-mq test`
Expected: 编译失败，提示 `XuanRocketMqProperties` 不存在或模块尚未完整实现。

- [ ] **Step 3: 在父 POM 注册新模块，并给模块 POM 补齐最小依赖**

```xml
<module>xuan-common-mq</module>
```

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
</dependency>
```

- [ ] **Step 4: 再次运行测试，确认失败点仍然集中在配置类未实现**

Run: `mvn -pl xuan-common-mq test`
Expected: 继续失败，但失败原因应聚焦到 `XuanRocketMqProperties` 缺失。

### Task 2: 实现 `xuan.rocketmq` 配置绑定

**Files:**
- Create: `backend/xuan-common-mq/src/main/java/com/xuan/erp/common/mq/config/XuanRocketMqProperties.java`
- Modify: `backend/xuan-common-mq/src/test/java/com/xuan/erp/common/mq/config/XuanRocketMqPropertiesTest.java`

- [ ] **Step 1: 写出完整的失败测试，覆盖基础配置绑定**

```java
assertThat(properties.enabled()).isTrue();
assertThat(properties.nameServer()).isEqualTo("duaoyunxuan.com:9047");
assertThat(properties.producer().group()).isEqualTo("xuan-test-producer");
```

- [ ] **Step 2: 运行测试确认因为配置类行为缺失而失败**

Run: `mvn -pl xuan-common-mq test`
Expected: FAIL，断言或绑定行为不满足预期。

- [ ] **Step 3: 用最小实现补齐配置类与嵌套配置**

```java
@ConfigurationProperties(prefix = "xuan.rocketmq")
public record XuanRocketMqProperties(
        boolean enabled,
        String nameServer,
        Producer producer
) {
    public XuanRocketMqProperties {
        producer = producer == null ? new Producer(null) : producer;
    }

    public record Producer(String group) {
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl xuan-common-mq test`
Expected: PASS

### Task 3: 回归验证四条范围

**Files:**
- Verify: `backend/pom.xml`
- Verify: `backend/xuan-common-mq/pom.xml`
- Verify: `backend/xuan-common-mq/src/main/java/com/xuan/erp/common/mq/config/XuanRocketMqProperties.java`

- [ ] **Step 1: 运行模块级 Maven 验证**

Run: `mvn -pl xuan-common-mq test`
Expected: `BUILD SUCCESS`

- [ ] **Step 2: 运行聚合校验，确认父 POM 已正确纳入模块**

Run: `mvn -pl xuan-common-mq -am test`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: 手工核对范围未越界**

Checklist:
- 已新增 `xuan-common-mq`
- 父 POM 已注册 `xuan-common-mq`
- 已引入 RocketMQ 统一依赖
- 已新增 `xuan.rocketmq` 公共配置
- 未额外实现发送器、自动装配、业务模块接入
