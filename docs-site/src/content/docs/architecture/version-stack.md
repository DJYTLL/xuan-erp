---
title: "Xuan ERP Microservice Version Stack"
---

更新时间：2026-06-07 11:59:31 +08:00

本文记录当前 Xuan ERP 微服务化重构推荐使用的生产级技术栈版本。目标是围绕 Spring Boot、Spring Cloud、Spring Cloud Alibaba、Nacos、Sentinel、Seata、RocketMQ 等组件形成一套可落地、版本对应关系清晰的选型。

## 1. 首推版本组合

| 组件 | 推荐版本 | 说明 |
| --- | --- | --- |
| JDK | Java 21 LTS | 当前项目生产基线，所有后端微服务统一使用 Java 21 |
| Spring Boot | 4.0.6 或同分支最新 4.0.x | 对应 Spring Cloud 2025.1.x |
| Spring Cloud | 2025.1.1 | 对应 Spring Boot 4.0.x |
| Spring Cloud Alibaba | 2025.1.0.0 | 对应 Spring Boot 4.0.x / Spring Cloud 2025.1.x |
| Nacos Server | 3.2.2 | 服务注册、配置中心；要求 Java 17+ |
| Nacos Client | 由 Spring Cloud Alibaba BOM 管理，当前为 3.1.1 | 不建议手工覆盖 |
| Sentinel | 由 BOM 管理为 1.8.9 | 单独看 Sentinel 已有 1.8.10，但生产中优先跟随 BOM |
| Seata | 由 BOM 管理为 2.5.0 | 分布式事务；建议只用于确实需要强一致协调的场景 |
| RocketMQ Client | 由 BOM 管理为 5.3.1 | 事件驱动、异步消息 |
| RocketMQ Server | 5.5.0 | 若追求更保守兼容，可用 5.3.4 |
| Gateway | Spring Cloud Gateway | 统一入口、路由、认证、限流 |
| 服务调用 | OpenFeign + Spring Cloud LoadBalancer | 服务间 HTTP 调用 |
| 客户端负载均衡 | `spring-cloud-starter-loadbalancer` | 服务间调用负载均衡，由 Spring Cloud BOM 管理版本 |
| 熔断限流 | Sentinel | 接口限流、熔断、热点参数保护 |
| 链路追踪 | Micrometer Tracing + OpenTelemetry | TraceId 全链路传递 |
| 监控 | Prometheus + Grafana | 指标采集与可视化 |
| 日志 | Loki 或 ELK | 集中日志查询 |

## 2. 保守版本组合

如果后续遇到第三方组件暂不兼容 Spring Boot 4，可以临时评估下面这套降级组合；但这不是当前项目默认方案：

| 组件 | 保守版本 |
| --- | --- |
| JDK | Java 21 |
| Spring Boot | 最新 3.5.x |
| Spring Cloud | 2025.0.2 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| Nacos Server | 3.2.2 |
| Sentinel | 跟随 Spring Cloud Alibaba BOM |
| Seata | 跟随 Spring Cloud Alibaba BOM |

对当前准备新拆微服务的 Xuan ERP 项目，默认选择首推组合：Java 21 LTS + Spring Boot 4.0.x + Spring Cloud 2025.1.x + Spring Cloud Alibaba 2025.1.x + Nacos 3.2.x。

## 3. Maven BOM 配置

父 POM 建议统一管理版本，业务服务不要各自手写 Nacos、Sentinel、Seata 等子依赖版本。

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.6</version>
</parent>

<properties>
    <java.version>21</java.version>
    <spring-cloud.version>2025.1.1</spring-cloud.version>
    <spring-cloud-alibaba.version>2025.1.0.0</spring-cloud-alibaba.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 4. 业务服务常用依赖

每个普通业务服务通常需要：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>

<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>

<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

`spring-cloud-starter-loadbalancer` 负责服务间调用的客户端负载均衡，版本由 Spring Cloud BOM 统一管理，业务服务不单独指定版本。

## 5. Gateway 服务依赖

网关服务建议使用 Spring Cloud Gateway，并接入 Sentinel Gateway 适配：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
</dependency>

<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
</dependency>
```

## 6. 配置中心与服务注册建议

Nacos 建议同时承担：

- 服务注册与发现。
- 配置中心。
- 环境隔离，例如 `dev`、`test`、`prod` namespace。
- 服务分组，例如 `XUAN_ERP_GROUP`。

示例配置：

```yaml
spring:
  application:
    name: xuan-product
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848
        namespace: prod
        group: XUAN_ERP_GROUP
      config:
        server-addr: nacos:8848
        namespace: prod
        group: XUAN_ERP_GROUP
        file-extension: yaml
```

生产环境不要把密码、JWT 密钥、数据库账号直接写死在 Git 仓库。可以使用 Nacos 加密能力、Kubernetes Secret、云厂商 Secret Manager，或部署平台的密钥管理能力。

## 7. 熔断限流建议

Sentinel 建议用于：

- Gateway 入口限流。
- 业务接口 QPS 限流。
- 热点参数限流，例如商品搜索、客户搜索。
- 服务间调用熔断。
- 慢调用比例熔断。

不建议把 Sentinel 当成业务正确性的兜底工具。库存扣减、应收生成、销售审核这类业务一致性问题，应该通过事务、事件、Outbox、幂等和补偿机制解决。

## 8. 事件与分布式事务建议

当前 Xuan ERP 拆分后，销售、采购、库存、财务之间会出现跨服务一致性问题。建议优先使用：

- 领域事件。
- Outbox Pattern。
- RocketMQ。
- 幂等消费。
- 重试与死信队列。
- 对账任务。

Seata 只建议用于确实需要同步强一致、链路短、参与服务少的场景。对于销售审核、采购审核这类高频核心业务，更推荐事件驱动 + 最终一致。

## 9. 服务上线顺序建议

涉及权限、菜单、配置、数据库迁移时，推荐上线顺序：

1. 部署基础设施：Nacos、Sentinel Dashboard、RocketMQ、监控、日志。
2. 部署 `xuan-iam`，执行 IAM 权限和菜单 migration。
3. 部署 `xuan-gateway`。
4. 部署基础业务服务：product、party、warehouse。
5. 部署核心交易服务：sales、procurement、inventory、finance。
6. 部署 document、audit、reporting。
7. 部署前端。

权限和菜单必须先进入 IAM，再上线依赖这些权限的业务服务与前端页面。

## 10. 版本来源

版本对应关系主要参考：

- Spring Cloud supported versions: https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions
- Spring Cloud Alibaba: https://github.com/alibaba/spring-cloud-alibaba
- Spring Cloud Alibaba Maven Central BOM: https://central.sonatype.com/artifact/com.alibaba.cloud/spring-cloud-alibaba-dependencies
- Nacos release history: https://nacos-group.github.io/en/download/release-history/
- RocketMQ download: https://rocketmq.apache.org/download/








