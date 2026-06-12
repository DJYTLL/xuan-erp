---
title: "Nacos 配置"
---

本文记录 Nacos 服务注册与配置中心的使用方式。

## Maven 依赖

服务如果需要接入 Nacos，按使用场景引入依赖。

### 服务注册发现

服务需要注册到 Nacos，或需要通过 Nacos 发现其它服务时，引入 Nacos Discovery 依赖：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

### 配置中心

服务需要从 Nacos 配置中心读取配置时，引入 Nacos Config 依赖：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

这些依赖版本由 Spring Cloud Alibaba BOM 统一管理，业务服务不单独指定版本。

## 注册中心

### 1. 配置服务名和 Nacos 地址

每个服务必须配置稳定的 `spring.application.name`。服务名使用 `xuan-领域名`，并作为 Nacos 注册名、配置 dataId、日志查询和服务调用的统一标识。

```yaml
server:
  port: 8104

spring:
  application:
    name: xuan-product
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: dev
        group: XUAN_ERP_GROUP
        enabled: true
```

启动后，`xuan-product` 会注册到 Nacos 的 `dev` namespace 和 `XUAN_ERP_GROUP` group 下。

### 2. 启动类开启注册发现

Spring Cloud Alibaba 会根据依赖和配置自动完成服务注册。启动类保持标准 Spring Boot 写法即可。

```java
package com.xuan.erp.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class XuanProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(XuanProductApplication.class, args);
    }
}
```

如果后续需要显式标注服务发现能力，可以加 `@EnableDiscoveryClient`，但在当前 Spring Cloud 版本下通常不是必须项。

```java
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class XuanProductApplication {
}
```

### 3. 服务间调用服务名

服务注册到 Nacos 后，内部调用不要写固定 IP 和端口，应使用 Nacos 中的服务名。

```java
package com.xuan.erp.sales.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "xuan-product")
public interface ProductClient {

    @GetMapping("/internal/products/{id}")
    ProductSnapshotResponse getProductSnapshot(@PathVariable("id") Long id);
}
```

`@FeignClient(name = "xuan-product")` 会通过 Nacos 找到 `xuan-product` 的可用实例，并配合 `spring-cloud-starter-loadbalancer` 做客户端负载均衡。

### 4. 注册验证

服务启动后，在 Nacos 控制台检查：

| 项 | 预期 |
| --- | --- |
| namespace | `dev` / `test` / `prod` |
| group | `XUAN_ERP_GROUP` |
| service name | `xuan-product` |
| 实例数 | 至少 1 个健康实例 |

如果服务没有出现在 Nacos 中，优先检查：

- 是否引入 `spring-cloud-starter-alibaba-nacos-discovery`。
- `server-addr` 是否能访问。
- `namespace` 和 `group` 是否与控制台查看位置一致。
- `spring.application.name` 是否为空或写错。
- 本地网络、防火墙或 Nacos Server 是否正常。

### 5. 使用 `DiscoveryClient` 获取服务实例

需要查看某个服务当前有哪些实例时，可以使用 Spring Cloud 的 `DiscoveryClient`。

```java
package com.xuan.erp.gateway.infrastructure.discovery;

import java.util.List;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
public class ServiceInstanceQuery {

    private final DiscoveryClient discoveryClient;

    public ServiceInstanceQuery(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public List<ServiceInstance> getProductInstances() {
        return discoveryClient.getInstances("xuan-product");
    }
}
```

`DiscoveryClient` 适合做服务实例查看、诊断和少量基础设施能力，不建议业务代码频繁手工挑选实例。

### 6. 使用 `LoadBalancerClient` 选择实例

如果确实需要手动选择一个服务实例，可以使用 `LoadBalancerClient`。

```java
package com.xuan.erp.gateway.infrastructure.discovery;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;

@Component
public class ServiceInstanceChooser {

    private final LoadBalancerClient loadBalancerClient;

    public ServiceInstanceChooser(LoadBalancerClient loadBalancerClient) {
        this.loadBalancerClient = loadBalancerClient;
    }

    public ServiceInstance chooseProductInstance() {
        return loadBalancerClient.choose("xuan-product");
    }
}
```

使用 `LoadBalancerClient` 需要引入：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

业务服务间调用优先使用 OpenFeign。`LoadBalancerClient` 更适合基础设施、诊断或需要自定义调用流程的场景。

### 7. 使用 `RestTemplate` 按服务名调用

如果使用 `RestTemplate` 发起服务间调用，需要给 `RestTemplate` 加 `@LoadBalanced`，这样才能通过 Nacos 服务名调用，而不是写固定 IP。

```java
package com.xuan.erp.sales.infrastructure.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

调用示例：

```java
String url = "http://xuan-product/internal/products/" + productId;
ProductSnapshotResponse product = restTemplate.getForObject(url, ProductSnapshotResponse.class);
```

`http://xuan-product` 中的 `xuan-product` 是 Nacos 服务名，由 LoadBalancer 选择具体实例。

## 配置中心

## Nacos 层级通用规范

Nacos 没有强制的行业统一标准，但企业微服务项目中最常见、最稳妥的分层模型是：

```text
配置中心：Namespace -> Group -> DataId -> Key
注册中心：Namespace -> Group -> Service -> Cluster -> Instance
```

简单理解：

| 层级 | 配置中心职责 | 注册中心职责 |
| --- | --- | --- |
| Namespace | 环境、租户或强隔离边界 | 环境、租户或强隔离边界 |
| Group | 项目、系统、业务线或应用集合 | 项目、系统、业务线或应用集合 |
| DataId / Service | 具体配置文件 | 具体服务名 |
| Key / Cluster / Instance | 具体配置项 | 集群和服务实例 |

市场上最常见的划分方式：

```text
namespace = 环境
group     = 项目 / 系统 / 业务域 / 应用集合
dataId    = 公共配置 + 单服务配置
key       = 具体配置项
```

示例：

```text
dev
  ERP_GROUP
    common.yaml
    gateway.yaml
    iam-service.yaml
    product-service.yaml
    order-service.yaml

test
  ERP_GROUP
    common.yaml
    gateway.yaml
    iam-service.yaml
    product-service.yaml
    order-service.yaml

pre
  ERP_GROUP
    common.yaml
    gateway.yaml
    iam-service.yaml
    product-service.yaml
    order-service.yaml

prod
  ERP_GROUP
    common.yaml
    gateway.yaml
    iam-service.yaml
    product-service.yaml
    order-service.yaml
```

### Namespace 负责什么

Namespace 是强隔离边界，最常用于环境隔离。

常见值：

- `dev`
- `test`
- `pre` / `staging`
- `prod`

建议：

- 不同环境使用不同 namespace。
- 不要把 dev、test、prod 放在同一个 namespace 中。
- 不要用 dataId 代替环境隔离，例如 `order-dev.yaml`、`order-prod.yaml`。
- 不要用 group 代替环境隔离，例如 `DEV_GROUP`、`PROD_GROUP`。

### Group 负责什么

Group 通常用于区分项目、系统、业务线或应用集合，不建议默认按每个微服务拆 group。

适合一个 group 的场景：

- 一个 ERP 系统内部有多个微服务。
- 多个服务属于同一个项目、同一套权限、同一套发布治理。
- 服务之间需要频繁调用，服务发现 group 保持一致可以减少跨 group 调用复杂度。

适合拆多个 group 的场景：

- 多个独立系统共用同一个 Nacos。
- 不同业务域拥有独立团队、独立权限、独立发布节奏。
- 某个业务域下面已经有多个子服务，例如 `ORDER_GROUP` 下有 `order-api`、`order-worker`、`order-billing`。

不推荐的做法：

- 每个微服务一个 group。
- 用 group 区分 dev、test、prod。
- 多个无关系统全部放入同一个 group。

### DataId 负责什么

DataId 表示具体配置文件，通常按公共配置和服务私有配置拆分。

常见拆法：

```text
common.yaml
gateway.yaml
product-service.yaml
order-service.yaml
```

或者在多个系统共用 Nacos 时保留项目前缀：

```text
xuan-common.yaml
xuan-gateway.yaml
xuan-product.yaml
xuan-sales.yaml
```

建议：

- 公共配置放公共 dataId。
- 单服务配置放单服务 dataId。
- 不要把所有服务配置塞进一个 `application.yaml`。
- 不要用 dataId 区分环境。

### Key 负责什么

Key 是配置文件内的具体配置项，例如：

```yaml
logging:
  level:
    com.xuan.erp: INFO

xuan:
  export:
    max-rows: 50000
```

Key 适合存放运行参数、功能开关、超时时间、日志级别、非敏感连接信息等；不适合承载复杂业务规则、大段模板、数据库结构、初始化数据、权限授权关系或明文密钥。

### 常见反模式

| 反模式 | 问题 |
| --- | --- |
| 所有配置都放 public namespace | 环境隔离弱，容易误改生产配置 |
| 用 `DEV_GROUP`、`PROD_GROUP` 区分环境 | group 职责被环境污染，后续服务发现和配置管理混乱 |
| 用 `order-dev.yaml`、`order-prod.yaml` 区分环境 | dataId 职责被环境污染，环境隔离不清晰 |
| 所有服务共用一个 `application.yaml` | 服务边界不清晰，变更影响面过大 |
| 每个微服务一个 group | 服务间调用和治理复杂度上升 |
| 生产密码、私钥、证书明文放 Nacos | 安全风险高，缺少密钥轮换和审计能力 |

### 1. 使用 `spring.config.import` 导入配置

Spring Boot 2.4 之后推荐使用 `spring.config.import` 导入外部配置。服务可以导入自己的 Nacos dataId，也可以导入公共配置。

```yaml
spring:
  application:
    name: xuan-product
  config:
    import:
      - optional:nacos:xuan-common.yaml
      - optional:nacos:xuan-product.yaml
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: dev
        group: XUAN_ERP_GROUP
        file-extension: yaml
```

说明：

| 配置 | 说明 |
| --- | --- |
| `optional:nacos:xuan-common.yaml` | 公共配置，不存在时不阻断启动 |
| `optional:nacos:xuan-product.yaml` | 当前服务配置 |
| `namespace` | 区分环境，例如 `dev`、`test`、`prod` |
| `group` | 区分项目或服务组，例如 `XUAN_ERP_GROUP` |
| `dataId` | 区分配置文件，例如 `xuan-product.yaml` |

## 配置动态刷新

### `@ConfigurationProperties` 批量绑定

推荐将同一组业务配置批量绑定到一个配置类。配置放在 Nacos 后，可以不用 `@RefreshScope`，配置变更后也能实现自动刷新。

```java
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "order")
@Data
public class OrderProperties {

    private String timeout;

    private String autoConfirm;
}
```

对应 Nacos 配置：

```yaml
order:
  timeout: 30m
  auto-confirm: true
```

这种写法适合一组配置项整体读取，例如订单超时、自动确认、导出限制、业务开关等。

### `@RefreshScope` 单 Bean 刷新

如果某个 Bean 需要在 Nacos 配置变更后动态读取新值，可以在该 Bean 上使用 `@RefreshScope`。

```java
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@RefreshScope
@Component
public class ExportProperties {
    // 读取 xuan.export.max-rows 等可动态调整配置
}
```

`@RefreshScope` 的作用是让被标注的 Bean 进入刷新作用域。当配置中心中的配置发生变化并触发刷新后，该 Bean 会重新创建，从而读取最新配置值。

适合使用 `@RefreshScope` 的场景：

- 功能开关。
- 导出最大行数。
- Feign 超时时间。
- 日志级别以外的自定义运行参数。
- 不影响核心数据一致性的运行阈值。

不建议滥用 `@RefreshScope`：

- 核心业务规则不要只靠动态配置绕过代码评审。
- 数据源、线程池、MQ Consumer 等基础设施 Bean 是否支持动态刷新要单独验证。
- 密码、私钥、证书等敏感配置即使能刷新，也不建议明文放入 Nacos。
- 配置变更必须经过发布流程，生产环境不允许随意手工修改。

### `@Value + @RefreshScope` 读取少量配置

少量配置项可以使用 `@Value` 读取，并配合 `@RefreshScope` 实现刷新。

```java
package com.xuan.erp.product.interfaces.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
public class ProductConfigController {

    @Value("${xuan.export.max-rows:50000}")
    private Integer maxExportRows;

    @GetMapping("/internal/config/export-max-rows")
    public Integer getMaxExportRows() {
        return maxExportRows;
    }
}
```

`@Value` 适合少量简单配置。配置项变多后，优先使用 `@ConfigurationProperties` 批量绑定。

### 使用 `NacosConfigManager` 监听配置变化

如果需要在配置变化时执行额外动作，例如记录日志、清理缓存、通知本地组件，可以使用 `NacosConfigManager` 监听配置。

```java
package com.xuan.erp.product.infrastructure.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import java.util.concurrent.Executor;
import org.springframework.stereotype.Component;

@Component
public class ProductConfigListener {

    public ProductConfigListener(NacosConfigManager nacosConfigManager) throws Exception {
        nacosConfigManager.getConfigService().addListener(
            "xuan-product.yaml",
            "XUAN_ERP_GROUP",
            new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    // 配置变更后可记录日志、刷新本地缓存或触发自定义处理
                }
            }
        );
    }
}
```

监听配置变化属于扩展能力，不建议把核心业务流程依赖在监听回调里。

## 配置优先级

配置优先级要保持简单、可解释，避免同一个配置在多个地方重复定义。

推荐顺序：

```text
启动参数 / 环境变量
  > Nacos 服务配置 dataId
  > Nacos 公共配置 dataId
  > 本地 application-local.yml
  > 本地 application.yml
```

Xuan ERP 推荐做法：

| 层级 | 示例 | 用途 |
| --- | --- | --- |
| 本地 `application.yml` | 服务基础默认值 | 放最低优先级默认配置 |
| 本地 `application-local.yml` | 本机开发差异 | 只用于本地，不提交敏感信息 |
| Nacos 公共配置 | `xuan-common.yaml` | 跨服务公共非敏感配置 |
| Nacos 服务配置 | `xuan-product.yaml` | 当前服务环境配置 |
| 启动参数 / 环境变量 | `--server.port=8104` | 临时覆盖或容器注入 |

namespace 用于区分环境，group 用于区分项目或服务组，dataId 用于区分具体配置文件。不要用 dataId 代替环境隔离，也不要把多个环境配置写在同一个 dataId 中。

## 适合放入 Nacos 的配置

Nacos 适合存放“不同环境不同、需要集中管理、可能需要灰度或动态调整”的运行时配置。

| 配置类型                    | 示例                                      | 说明                    |
| ----------------------- | --------------------------------------- | --------------------- |
| 服务基础配置                  | `server.port`、`spring.application.name` | 服务启动和注册发现所需配置         |
| 服务注册发现                  | Nacos `namespace`、`group`、服务注册开关        | 用于区分环境和服务分组           |
| 数据源连接信息                 | 数据库 host、port、database、连接池参数            | 可以放连接位置和池参数，密码不建议明文放入 |
| Redis 配置                | Redis host、port、database、timeout        | 运行环境相关的缓存连接配置         |
| RocketMQ 配置             | name server、consumer group、topic 前缀     | 消息中间件地址和消费组配置         |
| Sentinel 配置             | 限流阈值、熔断规则、热点参数规则                        | 可按环境和灰度策略调整           |
| Feign / LoadBalancer 配置 | 超时时间、重试策略、负载均衡策略                        | 服务间调用治理参数             |
| 日志级别                    | `logging.level.com.xuan.erp=INFO`       | 开发、测试、生产环境可以不同        |
| 功能开关                    | 是否启用导入、导出、打印、异步任务                       | 用于灰度发布或临时关闭某能力        |
| 外部服务地址                  | 短信、邮件、文件存储、打印服务地址                       | 不同环境的 endpoint 不同     |
| 业务可调参数                  | 导出最大行数、上传大小限制、任务批量大小                    | 不改变代码逻辑，只调整运行阈值       |

## 不适合放入 Nacos 的配置

Nacos 不应成为“所有东西都往里塞”的配置仓库。下面这些内容不建议直接放入 Nacos，或必须通过更严格的密钥管理和发布流程处理。

| 内容             | 原因                 | 推荐做法                                                 |
| -------------- | ------------------ | ---------------------------------------------------- |
| 数据库密码、Redis 密码 | 明文泄露风险高            | 使用 Kubernetes Secret、云厂商 Secret Manager 或 Nacos 加密能力 |
| JWT 私钥、签名密钥    | 泄露后会影响认证安全         | 使用专门的密钥管理系统，支持轮换和审计                                  |
| TLS 证书、私钥      | 文件体积大且安全敏感         | 使用证书管理系统或部署平台挂载                                      |
| 固定代码常量         | 不应该运行时变化           | 写入代码、枚举或公共模块                                         |
| 数据库表结构         | 配置中心不能替代 migration | 使用 Flyway migration 管理                               |
| 初始化数据和权限授权关系   | 需要审计、版本和回滚         | 使用 Flyway、受控 seed 或 IAM 后台管理                         |
| 复杂业务规则         | 容易绕过测试和代码评审        | 写入领域模型或规则引擎，并配套测试                                    |
| 大段 JSON / 模板文件 | 难审阅、难 diff、容易误改    | 放入代码仓库、对象存储或专门模板服务                                   |
| 生产紧急手工改动       | 缺少评审会造成环境漂移        | 通过发布流水线变更并记录版本                                       |

## Xuan ERP 推荐配置示例

Xuan ERP 按市场常用模型落地：namespace 按环境分，group 按项目分，dataId 按公共配置和服务配置分。

| 项              | 推荐值                 | 说明                                       |
| -------------- | ------------------- | ---------------------------------------- |
| dev namespace  | `dev`               | 本地和开发环境                                  |
| test namespace | `test`              | 测试环境                                     |
| pre namespace  | `pre`               | 预发或 staging 环境                           |
| prod namespace | `prod`              | 生产环境                                     |
| group          | `XUAN_ERP_GROUP`    | Xuan ERP 后端统一分组                          |
| 公共配置 dataId    | `xuan-common.yaml`  | 跨服务共享的非敏感公共配置                            |
| 服务配置 dataId    | `xuan-product.yaml` | 单服务配置，名称与 `spring.application.name` 保持一致 |

推荐结构：

```text
dev
  XUAN_ERP_GROUP
    xuan-common.yaml
    xuan-gateway.yaml
    xuan-iam.yaml
    xuan-product.yaml
    xuan-sales.yaml

test
  XUAN_ERP_GROUP
    xuan-common.yaml
    xuan-gateway.yaml
    xuan-iam.yaml
    xuan-product.yaml
    xuan-sales.yaml

pre
  XUAN_ERP_GROUP
    xuan-common.yaml
    xuan-gateway.yaml
    xuan-iam.yaml
    xuan-product.yaml
    xuan-sales.yaml

prod
  XUAN_ERP_GROUP
    xuan-common.yaml
    xuan-gateway.yaml
    xuan-iam.yaml
    xuan-product.yaml
    xuan-sales.yaml
```

当前不按 `product`、`order`、`sales` 每个微服务拆 group。原因是 Xuan ERP 是一个 ERP 系统下的多个微服务，统一 group 更符合“项目 / 系统 / 应用集合”这个职责，也能减少服务发现和服务间调用时的跨 group 复杂度。

只有当某个业务域发展成独立系统，并拥有多个子服务、独立团队、独立权限和独立发布节奏时，才考虑拆成独立 group，例如：

```text
prod
  ORDER_GROUP
    order-api.yaml
    order-worker.yaml
    order-billing.yaml

  PRODUCT_GROUP
    product-api.yaml
    product-search.yaml
    product-sync.yaml
```

对于当前阶段，`XUAN_ERP_GROUP` 统一承载 `xuan-gateway`、`xuan-iam`、`xuan-product`、`xuan-sales`、`xuan-inventory` 等服务更合适。

示例：

```yaml
spring:
  application:
    name: xuan-product
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848
        namespace: dev
        group: XUAN_ERP_GROUP
      config:
        server-addr: nacos:8848
        namespace: dev
        group: XUAN_ERP_GROUP
        file-extension: yaml

server:
  port: 8104

logging:
  level:
    com.xuan.erp: INFO

xuan:
  export:
    max-rows: 50000
  feign:
    connect-timeout: 3000
    read-timeout: 10000
```

生产环境中，数据库密码、JWT 密钥、证书等敏感信息不直接明文写入 Nacos。确需由 Nacos 承载时，必须使用加密能力，并在发布流程中保留变更记录和操作人。

## 待补充

- 生产环境安全配置。
