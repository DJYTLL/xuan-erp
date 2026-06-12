---
title: "服务通信"
---

本文记录微服务之间的同步调用、异步事件、服务间鉴权和超时重试策略。

## 同步调用基线

服务间 HTTP 同步调用优先使用 OpenFeign，并显式引入 `spring-cloud-starter-loadbalancer` 负责客户端负载均衡。

简单理解：

| 组件 | 负责什么 |
| --- | --- |
| `spring-cloud-starter-openfeign` | 声明式 HTTP 调用 |
| `spring-cloud-starter-loadbalancer` | 根据服务名选择具体服务实例 |

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

`spring-cloud-starter-loadbalancer` 的版本由 Spring Cloud BOM 管理，业务服务不单独覆盖。

## OpenFeign 示例

`Spring Cloud OpenFeign` 负责把 Java 接口声明转换成 HTTP 调用。比如 `xuan-sales` 需要查询 `xuan-product` 的商品快照：

```java
@FeignClient(name = "xuan-product", path = "/internal/products")
public interface ProductClient {

    @GetMapping("/{id}")
    ProductDTO getProduct(@PathVariable("id") Long id);
}
```

销售服务中可以像调用本地接口一样调用商品服务：

```java
@Service
public class SalesOrderApplicationService {

    private final ProductClient productClient;

    public SalesOrderApplicationService(ProductClient productClient) {
        this.productClient = productClient;
    }

    public void createOrder(Long productId) {
        ProductDTO product = productClient.getProduct(productId);
        // 使用商品快照创建销售单
    }
}
```

调用链路：

```text
xuan-sales -> OpenFeign -> Spring Cloud LoadBalancer -> xuan-product 实例
```

## 批量调用与 N+1 禁止规则

服务间调用必须优先设计成批量查询。凡是一次业务请求中已经拿到一组 ID 的场景，调用方应一次性把 ID 集合传给被调用服务，由被调用服务批量返回快照数据，禁止在循环中逐条 Feign 调用。

典型反例：

```java
for (SalesOrderItem item : items) {
    ProductDTO product = productClient.getProduct(item.getProductId());
    item.fillProductName(product.getName());
}
```

这种写法会让一张销售单的明细行数直接变成远程请求次数。明细越多，耗时越长，也更容易放大下游抖动。

推荐写法：

```java
List<Long> productIds = items.stream()
    .map(SalesOrderItem::getProductId)
    .distinct()
    .toList();

Map<Long, ProductDTO> productMap = productClient.listProductSnapshots(productIds);

for (SalesOrderItem item : items) {
    ProductDTO product = productMap.get(item.getProductId());
    item.fillProductSnapshot(product);
}
```

对应 Feign 接口：

```java
@FeignClient(name = "xuan-product", path = "/internal/products")
public interface ProductClient {

    @PostMapping("/snapshots:batch-get")
    Map<Long, ProductDTO> listProductSnapshots(@RequestBody List<Long> productIds);
}
```

Xuan ERP 约束：

- 当前页面首屏或当前业务动作必须展示的数据，应由后端一次性聚合返回；禁止因为明细行、关联 ID 或字典项数量增长，造成前端或后端 N+1 次请求。
- 后端一次服务调用能批量完成的数据补齐，不拆成循环 N 次 Feign 调用。
- 销售单、采购单、库存单据、生产工单等包含明细行的接口，必须优先提供批量快照查询能力。
- 商品名称、商品编码、仓库名称、单位、客户名称、供应商名称等展示字段，应由后端在 DTO 中一次性返回。
- 批量接口要对 ID 去重，并对单次请求数量设置上限，避免一次请求过大。
- 批量查询结果建议按 ID 返回 `Map` 或稳定顺序列表，调用方必须处理缺失 ID。

边界和例外：

- 历史轨迹、审计日志、附件列表等较重数据，可以由前端在用户点开时再懒加载。
- 库存可用量等实时敏感数据，可以单独刷新，但后端仍要提供批量查询能力。
- 超大明细不能无限一次性返回，必须使用分页、分段加载或单次数量上限。
- 多个不相关写操作不能为了减少请求强行合并，应保持命令语义、事务边界和幂等规则清晰。

## LoadBalancer 示例

`Spring Cloud LoadBalancer` 负责在多个服务实例中选择一个实际实例。使用 `RestTemplate`、`WebClient.Builder` 或 `RestClient.Builder` 按服务名调用时，需要通过 `@LoadBalanced` 标记客户端。

```java
@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

调用时 URL 中的 `xuan-product` 是服务名，不是固定域名：

```java
ProductDTO product = restTemplate.getForObject(
    "http://xuan-product/internal/products/{id}",
    ProductDTO.class,
    productId
);
```

如果 `xuan-product` 有多个实例，LoadBalancer 会根据服务发现结果选择其中一个实例。

## 常见问题

### `ProductClient` 需要手动创建吗？

不需要。下面这种写法是 Spring 构造器注入，不是手动创建对象：

```java
public SalesOrderApplicationService(ProductClient productClient) {
    this.productClient = productClient;
}
```

Spring 创建 `SalesOrderApplicationService` Bean 时，会自动从容器中找到 `ProductClient` Bean 并传入构造方法。`ProductClient` 是由 `@FeignClient` 生成的代理对象。

如果项目使用 Lombok，也可以写成：

```java
@Service
@RequiredArgsConstructor
public class SalesOrderApplicationService {

    private final ProductClient productClient;
}
```

两种写法本质都是构造器注入。字段注入虽然也能工作，但不推荐作为默认写法：

```java
@Autowired
private ProductClient productClient;
```

### OpenFeign 是否等同于封装了 LoadBalancer？

不完全是。OpenFeign 封装的是 HTTP 调用写法，LoadBalancer 负责按服务名选择具体服务实例。

不用 OpenFeign 时，可能需要手写：

```java
ProductDTO product = restTemplate.getForObject(
    "http://xuan-product/internal/products/{id}",
    ProductDTO.class,
    productId
);
```

使用 OpenFeign 后，业务代码变成：

```java
ProductDTO product = productClient.getProduct(productId);
```

底层链路可以理解为：

```text
productClient.getProduct(productId)
  -> OpenFeign 生成 HTTP 请求
  -> Spring Cloud LoadBalancer 根据 xuan-product 选择实例
  -> 调用具体 xuan-product 实例
```

所以更准确的表述是：

```text
OpenFeign 是服务间 HTTP 调用的声明式封装；
Spring Cloud LoadBalancer 是 OpenFeign 按服务名调用时使用的客户端负载均衡能力。
```

### 使用 OpenFeign 时还需要 `@LoadBalanced` 吗？

一般不需要。OpenFeign 通过 `@FeignClient(name = "xuan-product")` 使用服务名调用，若项目中存在 `spring-cloud-starter-loadbalancer`，Feign 会集成 Spring Cloud LoadBalancer 完成实例选择。

`@LoadBalanced` 主要用于手写 HTTP 客户端 Bean，例如：

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

对比：

| 调用方式 | 是否需要 `@LoadBalanced` |
| --- | --- |
| OpenFeign：`@FeignClient(name = "xuan-product")` | 不需要 |
| `RestTemplate` 按服务名调用 | 需要 |
| `WebClient.Builder` 按服务名调用 | 需要 |
| `RestClient.Builder` 按服务名调用 | 需要 |

### 只引入 OpenFeign，不引入 LoadBalancer，还有负载均衡吗？

没有 Spring Cloud 的服务发现负载均衡能力。

如果只引入：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

但没有引入：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

Feign 仍然可以作为普通 HTTP 客户端使用，但不会通过 Spring Cloud LoadBalancer 按服务名从注册中心选择实例。`@FeignClient(name = "xuan-product")` 想要按服务名调用并负载均衡，应确保 `spring-cloud-starter-loadbalancer` 在 classpath 中。

## 待补充

- OpenFeign 调用规范。
- LoadBalancer 负载均衡策略。
- 服务 Token。
- 用户上下文透传。
- 内部接口路径规范。



