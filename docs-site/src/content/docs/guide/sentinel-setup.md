---
title: "Sentinel 准备"
---

本文记录 Xuan ERP 接入 Sentinel 的标准方式。Sentinel 用于微服务流量治理，主要负责限流、熔断降级、热点参数保护、系统自适应保护和网关入口保护。

Sentinel 不负责业务正确性。库存扣减、销售审核、应收生成、单据状态流转等一致性问题，必须通过事务、领域规则、幂等、事件、Outbox 和补偿机制解决，不能依赖 Sentinel 兜底。

## 处理流程

Sentinel 的核心处理流程是：请求进入资源，按规则检查是否违反；未违反则放行，违反则抛出限流或熔断异常；如果配置了兜底处理则执行 fallback，否则返回默认错误。

![Sentinel 资源规则检查处理流程](/images/sentinel-processing-flow.svg)

## 使用场景

| 场景 | 建议 |
| --- | --- |
| Gateway 入口限流 | 使用 Sentinel Gateway 规则保护入口流量 |
| 业务接口 QPS 限流 | 对列表、搜索、导出、批量操作等接口设置流控规则 |
| 热点参数限流 | 对商品搜索、客户搜索、仓库查询等热点参数接口设置热点规则 |
| 服务间调用熔断 | 对 OpenFeign 下游调用设置慢调用比例、异常比例或异常数熔断 |
| 系统保护 | 在高负载时保护服务实例，避免雪崩 |

不建议使用 Sentinel 的场景：

- 替代权限校验。
- 替代租户隔离。
- 替代业务幂等。
- 替代数据库事务。
- 替代消息补偿和对账。

## Maven 依赖

### 普通业务服务

普通业务服务需要接入 Sentinel 时，引入 Sentinel starter：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

如果 Sentinel 规则需要通过 Nacos 动态加载，再引入 Sentinel datasource 依赖：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-sentinel-datasource</artifactId>
</dependency>
```

这些依赖版本由 Spring Cloud Alibaba BOM 管理，业务服务不单独指定版本。

### Gateway 服务

`xuan-gateway` 使用 Spring Cloud Gateway 时，需要额外接入 Sentinel Gateway 适配：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>

<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
</dependency>
```

Gateway 入口限流规则使用 `gw-flow` 和 `gw-api-group` 类型。`spring-cloud-alibaba-sentinel-gateway` 是 Gateway 适配，仍需要配合 Sentinel starter 使用。

## 基础配置

业务服务连接 Sentinel Dashboard：

```yaml
spring:
  application:
    name: xuan-product
  cloud:
    sentinel:
      transport:
        dashboard: 127.0.0.1:8858
        port: 8719
      eager: true
```

说明：

| 配置 | 说明 |
| --- | --- |
| `dashboard` | Sentinel Dashboard 地址 |
| `port` | 当前服务与 Dashboard 通信的本地端口，多个服务不能冲突 |
| `eager` | 是否启动后立即初始化 Sentinel |

生产环境中，Dashboard 地址应按环境放入 Nacos 配置，不写死在代码仓库。

## OpenFeign 熔断

服务间 HTTP 调用优先使用 OpenFeign。需要让 Sentinel 接管 Feign 调用保护时，开启：

```yaml
feign:
  sentinel:
    enabled: true
```

推荐对这些调用配置熔断规则：

- `xuan-sales -> xuan-product` 查询商品快照。
- `xuan-sales -> xuan-warehouse` 查询仓库或库存信息。
- `xuan-inventory -> xuan-product` 查询商品基础信息。
- `xuan-document -> 业务服务` 查询打印快照。

Feign 熔断只能降低下游异常对当前服务的影响，不能吞掉业务错误。下游业务失败时，必须返回清晰错误或稳定降级结果。

## 规则类型

常用规则：

| 规则类型 | rule-type | 用途 |
| --- | --- | --- |
| 流控规则 | `flow` | QPS、并发线程数限流 |
| 熔断降级规则 | `degrade` | 慢调用比例、异常比例、异常数熔断 |
| 热点参数规则 | `param-flow` | 针对热点参数限流 |
| 系统规则 | `system` | 根据系统负载、CPU、RT 等做整体保护 |
| 授权规则 | `authority` | Sentinel 调用来源控制，不替代 IAM 权限 |
| 网关流控规则 | `gw-flow` | Gateway 路由或 API 分组限流 |
| 网关 API 分组 | `gw-api-group` | Gateway API 分组定义 |

Xuan ERP 默认优先使用 `flow`、`degrade`、`param-flow`、`gw-flow`、`gw-api-group`。

## Nacos 动态规则

Sentinel Dashboard 的临时规则不适合作为生产持久化来源。生产建议将 Sentinel 规则放入 Nacos，并通过 Sentinel datasource 动态加载。

推荐 dataId 命名：

| 服务 | dataId |
| --- | --- |
| 商品服务流控规则 | `xuan-product-sentinel-flow-rules.json` |
| 商品服务熔断规则 | `xuan-product-sentinel-degrade-rules.json` |
| 商品服务热点规则 | `xuan-product-sentinel-param-flow-rules.json` |
| 网关流控规则 | `xuan-gateway-sentinel-gw-flow-rules.json` |
| 网关 API 分组 | `xuan-gateway-sentinel-gw-api-group-rules.json` |

当前 `duaoyunxuan.com` Nacos 的 `dev`、`prod` namespace 已初始化 Sentinel 规则 dataId。普通业务服务初始化 `flow`、`degrade`、`param-flow` 三类规则；`xuan-gateway` 初始化 `gw-flow`、`gw-api-group` 两类规则。初始内容统一为 `[]`，表示没有下发实际限流或熔断规则。

注意：创建规则 dataId 只是准备持久化存储。要让服务真正加载这些规则，还需要在对应服务配置 dataId 中加入 `spring.cloud.sentinel.datasource` 配置。批量修改 `dev` / `prod` 服务配置会影响运行态，必须作为单独发布动作执行。

推荐配置：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: duaoyunxuan.com:9022
      eager: true
      datasource:
        flow:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            namespace: ${spring.cloud.nacos.config.namespace}
            username: ${spring.cloud.nacos.username:nacos}
            password: ${spring.cloud.nacos.password:nacos}
            group-id: XUAN_ERP_GROUP
            data-id: xuan-product-sentinel-flow-rules.json
            data-type: json
            rule-type: flow
        degrade:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            namespace: ${spring.cloud.nacos.config.namespace}
            username: ${spring.cloud.nacos.username:nacos}
            password: ${spring.cloud.nacos.password:nacos}
            group-id: XUAN_ERP_GROUP
            data-id: xuan-product-sentinel-degrade-rules.json
            data-type: json
            rule-type: degrade
        param-flow:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            namespace: ${spring.cloud.nacos.config.namespace}
            username: ${spring.cloud.nacos.username:nacos}
            password: ${spring.cloud.nacos.password:nacos}
            group-id: XUAN_ERP_GROUP
            data-id: xuan-product-sentinel-param-flow-rules.json
            data-type: json
            rule-type: param-flow
```

规则和服务配置使用相同 namespace 和 group，便于按环境和项目统一治理。规则 dataId 独立于服务配置 dataId，避免把运行配置和治理规则混在一个文件中。

Gateway 规则示例：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: duaoyunxuan.com:9022
      eager: true
      datasource:
        gw-flow:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            namespace: ${spring.cloud.nacos.config.namespace}
            username: ${spring.cloud.nacos.username:nacos}
            password: ${spring.cloud.nacos.password:nacos}
            group-id: XUAN_ERP_GROUP
            data-id: xuan-gateway-sentinel-gw-flow-rules.json
            data-type: json
            rule-type: gw-flow
        gw-api-group:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            namespace: ${spring.cloud.nacos.config.namespace}
            username: ${spring.cloud.nacos.username:nacos}
            password: ${spring.cloud.nacos.password:nacos}
            group-id: XUAN_ERP_GROUP
            data-id: xuan-gateway-sentinel-gw-api-group-rules.json
            data-type: json
            rule-type: gw-api-group
```

## 规则示例

### 流控规则

```json
[
  {
    "resource": "/api/products",
    "grade": 1,
    "count": 100,
    "controlBehavior": 0
  }
]
```

### 熔断规则

```json
[
  {
    "resource": "GET:http://xuan-product/internal/products/{id}",
    "grade": 0,
    "count": 1000,
    "timeWindow": 10,
    "minRequestAmount": 20,
    "statIntervalMs": 10000,
    "slowRatioThreshold": 0.5
  }
]
```

资源名需要以实际运行时 Sentinel 识别到的资源名为准。上线前必须通过 Dashboard 或埋点确认资源名，不要凭感觉写规则。

## 资源命名

默认 Web 接口、Gateway 路由、Feign 调用会由框架接入 Sentinel。对于非 Web 的核心资源，可以使用 `@SentinelResource` 明确命名。

```java
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.stereotype.Service;

@Service
public class ProductSearchApplicationService {

    @SentinelResource(
        value = "productSearch",
        blockHandler = "handleBlocked",
        fallback = "fallbackSearch"
    )
    public ProductSearchResult search(ProductSearchQuery query) {
        // 查询商品
        return ProductSearchResult.empty();
    }
}
```

建议：

- 资源名稳定、可读、能对应业务动作。
- blockHandler 处理限流或熔断。
- fallback 处理业务异常降级。
- 降级结果必须可解释，不返回错误的业务数据。

## Xuan ERP 推荐落地

| 服务 | Sentinel 重点 |
| --- | --- |
| `xuan-gateway` | 入口 QPS、路由限流、登录接口保护、导出入口保护 |
| `xuan-product` | 商品列表、商品搜索、商品快照批量查询热点保护 |
| `xuan-party` | 客户、供应商搜索热点保护 |
| `xuan-warehouse` | 仓库查询、库存可用量查询限流 |
| `xuan-sales` | 销售单列表、审核接口、下游商品和库存调用熔断 |
| `xuan-document` | 打印预览、打印提交、模板查询限流 |

默认策略：

- 入口限流先在 Gateway 做。
- 业务服务再做关键接口保护。
- 服务间调用必须设置超时，再配合 Sentinel 熔断。
- 规则变更通过 Nacos 发布，生产环境保留变更记录和操作人。

## 验证方式

接入 Sentinel 后，至少验证：

- 服务启动后能在 Sentinel Dashboard 看到应用。
- 调用接口后 Dashboard 有实时监控数据。
- Nacos 中的 Sentinel 规则能被服务加载。
- 超过 QPS 阈值时返回统一限流响应。
- 下游慢调用或异常达到阈值时触发熔断。
- Gateway 规则能按路由或 API 分组生效。

## 待补充

- 统一限流响应格式。
- Gateway API 分组规则样例。
- Sentinel Dashboard 部署方式。
