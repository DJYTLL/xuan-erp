---
title: "OpenAPI 与 springdoc-openapi"
---

本文记录 Xuan ERP 后端服务接入 OpenAPI / springdoc-openapi 的标准方式。

## 定位

OpenAPI 是机器可读的接口契约，主要解决：

- 前后端联调时确认接口路径、方法、参数、响应结构。
- 服务间 HTTP 调用时确认契约。
- 自动生成接口调试页面。
- 后续生成客户端 SDK、接口测试用例或契约校验。

Markdown 接口文档仍然保留，用于说明业务语义、权限、租户、幂等、审计、状态机和领域规则。

推荐分工：

| 文档类型 | 负责内容 |
| --- | --- |
| OpenAPI | 接口长什么样：路径、方法、参数、响应模型、状态码 |
| Markdown `api.md` | 接口为什么这样设计：业务规则、权限、租户、幂等、审计、边界 |

## 依赖

普通 Spring MVC 后端服务默认引入：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

如果服务是 WebFlux，再评估使用：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
</dependency>
```

版本优先由父 POM 或 dependency management 统一管理，业务服务不单独散落版本号。

## 默认访问地址

单服务本地默认地址：

```text
OpenAPI JSON: /v3/api-docs
Swagger UI:   /swagger-ui.html
```

示例：

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/swagger-ui.html
```

线上环境是否开放 Swagger UI 要按环境控制。生产环境可以保留 `/v3/api-docs` 给内网、测试或网关聚合使用，但不建议把 Swagger UI 直接暴露到公网。

## 推荐配置

服务内配置示例：

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
  packages-to-scan:
    - com.xuan.erp
```

生产环境可以通过 Nacos 或环境变量关闭 UI：

```yaml
springdoc:
  swagger-ui:
    enabled: false
```

## 注解规范

Controller 必须补充接口说明：

```java
@Tag(name = "商品管理", description = "商品主档、商品状态和商品查询接口")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Operation(summary = "查询商品列表", description = "按租户查询商品主档分页列表")
    @GetMapping
    public ApiResponse<PageResult<ProductListItemResponse>> page(ProductPageQuery query) {
        return ApiResponse.ok(productApplicationService.page(query));
    }
}
```

DTO 字段必须补充 `@Schema`：

```java
public class ProductListItemResponse {

    @Schema(description = "商品 ID", example = "1890000000000000001")
    private Long id;

    @Schema(description = "商品编码", example = "P-0001")
    private String code;

    @Schema(description = "商品名称", example = "空气滤芯")
    private String name;
}
```

常用注解：

| 注解 | 用途 |
| --- | --- |
| `@Tag` | Controller 分组 |
| `@Operation` | 接口摘要和描述 |
| `@Parameter` | Path、Query、Header 参数说明 |
| `@Schema` | DTO、字段、枚举说明 |
| `@ApiResponse` | 响应状态和错误结构说明 |

## 安全要求

- Swagger UI 不直接暴露到公网。
- 内部管理接口、运维接口和敏感接口必须在 OpenAPI 中明确权限和调用边界。
- OpenAPI 不能替代后端鉴权，Controller 仍必须使用 `@PreAuthorize`。
- DTO 中不要暴露密码、密钥、token、内部配置等敏感字段。
- 生产环境如需开放 `/v3/api-docs`，必须通过网关、内网、VPN 或白名单控制访问。

## 与 Gateway 的关系

第一阶段每个服务先提供自己的 `/v3/api-docs`。

后续可以在 Gateway 或独立文档服务聚合各服务 OpenAPI：

```text
xuan-gateway
  -> xuan-product /v3/api-docs
  -> xuan-sales /v3/api-docs
  -> xuan-inventory /v3/api-docs
```

聚合时必须避免把未授权的内部接口暴露给前端或公网。

## 交付要求

新增或修改接口时，必须同步完成：

- Controller OpenAPI 注解。
- Request / Response DTO `@Schema`。
- Markdown `api.md` 业务说明。
- 权限码、租户要求、幂等规则和错误码说明。
- 必要的接口测试或契约校验。

OpenAPI 负责“可执行契约”，Markdown 负责“业务解释”，两者缺一不可。
