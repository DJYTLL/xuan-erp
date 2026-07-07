---
title: "Seata 准备"
---

本文记录 Xuan ERP 接入 Seata 的标准方式。Seata 用于管理微服务之间的同步分布式事务，适合确实需要跨服务强一致、链路短、参与服务少的业务场景。

Seata 不是所有跨服务一致性问题的默认答案。销售审核、库存扣减、应收生成等高频核心链路，仍要优先评估领域事件、Outbox、RocketMQ、幂等消费、补偿和对账。只有当业务明确要求同步强一致，且链路复杂度可控时，才使用 Seata。

## 服务地址约定

Xuan ERP 中，Seata 相关组件与 Nacos 使用同一台主机或同一服务域名。

Seata 2.4 之后，控制台入口放在 Namingserver 上，不再直接挂在 `seata-server` 上。

| 项 | 推荐值 | 说明 |
| --- | --- | --- |
| Nacos 地址 | `duaoyunxuan.com:9041` | 服务注册与配置中心 |
| Seata Namingserver / Console 地址 | `duaoyunxuan.com:9021` | 浏览器访问入口，主机与 Nacos 相同，端口为 `9021` |
| Seata Server 事务地址 | `xuan-seata:8091` | TC 服务端口，通常给业务服务在内网或 Docker 网络中访问 |
| Seata Server 对外事务地址 | `duaoyunxuan.com:9045` | 服务器外部访问 TC 时使用 |
| Seata 注册名 | `seata-server` | Seata Server 注册到 Nacos 的服务名 |
| namespace | `dev` / `prod` | 与当前环境一致 |
| group | `XUAN_ERP_GROUP` | 与 Xuan ERP 服务保持一致 |
| tx-service-group | `xuan-erp-tx-group` | Xuan ERP 默认事务分组 |

如果 Nacos 使用域名或 IP，例如 `duaoyunxuan.com:9041`，则 Seata Namingserver / Console 地址为同一主机的 `duaoyunxuan.com:9021`。

## 使用场景

推荐使用 Seata 的场景：

- 短链路、低频、确实需要同步强一致的跨服务写操作。
- 管理后台配置类操作，涉及 2 到 3 个服务的同步写入。
- 无法接受最终一致延迟，且补偿成本高于同步事务成本的流程。

不推荐使用 Seata 的场景：

- 高频核心交易链路默认全部套分布式事务。
- 长链路、多服务、多外部系统参与的流程。
- 需要人工审核、异步处理、消息重试、对账的业务。
- 查询接口、报表接口、打印预览接口。

## Maven 依赖

需要参与 Seata 全局事务的业务服务引入：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
```

依赖版本由 Spring Cloud Alibaba BOM 统一管理，业务服务不单独指定版本。

## 客户端配置

业务服务接入 Seata 时，建议和 Nacos 使用同一套 namespace、group。

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: xuan-erp-tx-group
  registry:
    type: nacos
    nacos:
      server-addr: ${spring.cloud.nacos.discovery.server-addr}
      namespace: ${spring.cloud.nacos.discovery.namespace}
      group: XUAN_ERP_GROUP
      application: seata-server
  config:
    type: nacos
    nacos:
      server-addr: ${spring.cloud.nacos.config.server-addr}
      namespace: ${spring.cloud.nacos.config.namespace}
      group: XUAN_ERP_GROUP
      data-id: seataServer.properties
  service:
    vgroup-mapping:
      xuan-erp-tx-group: default
```

说明：

| 配置 | 说明 |
| --- | --- |
| `application-id` | 当前业务服务名，建议与 `spring.application.name` 一致 |
| `tx-service-group` | 当前服务所属事务分组 |
| `registry.nacos.application` | Seata Server 在 Nacos 中的服务名 |
| `vgroup-mapping` | 事务分组到 Seata 集群的映射 |

`9021` 是 Seata Namingserver / Console 的对外入口。业务服务真正参与全局事务时，使用的是 Seata Server 的事务服务端口，默认是 `8091`。客户端通常通过注册发现或内网服务名访问 Seata Server，不在业务代码中硬编码控制台地址。

`duaoyunxuan.com` 服务器当前对外端口约定中，`9021` 是 Seata 控制台，`9045` 是 Seata TC 事务服务端口。外部客户端需要连接 `duaoyunxuan.com` 这台服务器上的 Seata TC 时使用 `duaoyunxuan.com:9045`。NAS 或其它远程服务器按各自部署记录为准。

## 事务使用方式

全局事务入口放在 ApplicationService，不放在 Controller、Domain Entity、Mapper 或基础设施实现中。

```java
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

@Service
public class SalesAuditApplicationService {

    @GlobalTransactional(name = "sales-audit", rollbackFor = Exception.class)
    public void audit(SalesAuditCommand command) {
        // 1. 校验销售单状态
        // 2. 调用库存服务锁定或扣减库存
        // 3. 调用财务服务生成应收
        // 4. 更新销售单审核状态
    }
}
```

使用要求：

- `@GlobalTransactional` 只标在明确的用例入口上。
- 全局事务内避免调用不支持回滚的外部系统。
- 全局事务内避免执行耗时任务、人工流程、异步等待。
- 所有参与方本地事务必须清晰，异常必须向外抛出，不能吞异常。
- 关键写操作仍然要有幂等键，避免重试造成重复提交。

## undo_log 要求

使用 Seata AT 模式时，每个参与全局事务的业务库都需要 `undo_log` 表。

落地时必须遵守数据库迁移规范：

- 先扫描当前服务 `src/main/resources/db/migration` 目录。
- 确认当前最高版本号。
- 按顺序新增 migration，例如 `V{next}__add_seata_undo_log.sql`。
- 禁止复用版本号、跳号、修改历史 migration。

本次文档更新不新增任何 migration。后续具体服务接入 Seata AT 模式时，再按服务分别补充 `undo_log` migration。

## Xuan ERP 推荐边界

| 场景 | 推荐方式 |
| --- | --- |
| 销售审核带库存与财务联动 | 优先事件 + Outbox + 幂等 + 补偿；确需同步强一致时再评估 Seata |
| 管理后台低频配置同步 | 可以评估 Seata |
| 多服务短链路强一致写入 | 可以评估 Seata |
| 查询、报表、打印预览 | 不使用 Seata |
| 长流程、人工审核、外部三方系统 | 不使用 Seata，使用事件和补偿 |

## 和 Nacos 的关系

当前项目文档约定里，Nacos 继续承担服务注册、配置中心和环境隔离；Seata 的部署地址与 Nacos 保持同主机，便于统一记忆和排障。

如果后续项目采用 Nacos 作为 Seata 的注册中心和配置中心，则建议：

- Seata Server 注册到 Nacos。
- 业务服务通过 Nacos 发现 `seata-server`。
- Seata 配置放入 Nacos 的 `seataServer.properties`。
- namespace 与当前环境一致。
- group 使用 `XUAN_ERP_GROUP`。

当前 NAS 部署口径中，`9021` 是 Namingserver / Console 对外入口，`8091` 是 Seata Server 事务服务端口。

```text
Nacos:  duaoyunxuan.com:9041
Seata Console: duaoyunxuan.com:9021
Seata TC:      xuan-seata:8091
```

## 验证方式

接入 Seata 后至少验证：

- Seata Server 能注册到 Nacos。
- 业务服务能发现 `seata-server`。
- Seata Namingserver / Console 对外端口 `9021` 可访问。
- Seata Server 事务端口 `8091` 可在服务网络内访问。
- 全局事务提交成功时，各参与服务本地事务都提交。
- 全局事务回滚时，各参与服务本地事务都回滚。
- AT 模式参与库存在 `undo_log` 表。
- 异常不能被业务代码吞掉，否则全局事务无法正确回滚。

## 待补充

- Seata Server 部署参数。
- `undo_log` migration 模板。
- 统一全局事务命名规范。
