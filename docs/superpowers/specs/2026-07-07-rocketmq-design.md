# RocketMQ 基础接入骨架设计

## 背景

当前 Xuan ERP 仓库已经明确把 RocketMQ 作为目标消息中间件，并且 `xuan-tenant`、`xuan-iam` 已经具备 Outbox 表、事件契约和 `infrastructure/mq` 包位。但代码层还没有统一的 RocketMQ 依赖管理、公共配置、统一发送封装，也没有可复用的示例 Producer / Consumer 骨架。

如果直接在每个业务服务里各自接一套 RocketMQ 客户端，后续 `xuan-product`、`xuan-sales`、`xuan-audit` 等服务继续接入时会产生重复依赖、重复配置和重复发送代码，不利于公共模块边界稳定。

因此本次先完成“基础接入骨架”，把消息能力沉到公共模块，让业务服务只保留最薄的一层接入代码。

## 目标

- 新增统一的 RocketMQ 公共模块，承载依赖、配置、发送封装和自动装配。
- 让业务服务按统一方式发送消息，而不是直接散落使用底层客户端。
- 在 `xuan-tenant`、`xuan-iam` 中分别提供一个最小示例 Producer / Consumer，作为后续服务复制模板。
- 保持和当前 `xuan-common-security`、`xuan-common-web` 一致的公共模块组织方式。
- 为后续接 Outbox 发布器、消费重试、死信处理预留稳定落点。

## 非目标

- 本次不实现 Outbox 扫描发布器。
- 本次不实现消费失败后的数据库回写、重试调度或死信处理。
- 本次不改造现有业务事件模型，不统一替换现有领域事件类。
- 本次不接真实 RocketMQ 服务端做端到端环境联调。
- 本次不新增、修改任何 Flyway migration。

## 方案选择

## 方案 A：新增 `xuan-common-mq` 公共模块

优点：

- 公共职责边界清晰，消息能力不会污染 `xuan-common-web`。
- 后续任何业务服务都可以直接依赖该模块复用能力。
- 便于后续继续增加 Outbox 发布器、消费幂等、统一消息头等能力。

代价：

- 需要改父 POM 和相关业务模块 POM。
- 需要维护一个新的公共模块。

## 方案 B：把 RocketMQ 能力放入 `xuan-common-web`

优点：

- 少一个模块，接入路径更短。

问题：

- Web 与 MQ 职责混在一起，公共边界会变糊。
- 后续演进消息能力时会把 `xuan-common-web` 越堆越重。

## 方案 C：在业务模块各自直连 RocketMQ

优点：

- 最快写出效果。

问题：

- 不满足“统一依赖、统一配置、统一发送封装”的目标。
- 后续服务接入只能复制粘贴。

结论：本次采用方案 A，新增 `xuan-common-mq`。

## 模块设计

### 1. `xuan-common-mq`

职责：

- 统一引入 RocketMQ 相关依赖。
- 提供 `xuan.rocketmq` 配置绑定。
- 提供统一消息对象与消息发送接口。
- 提供默认发送实现和自动装配。

该模块只负责“消息基础能力”，不直接承担业务事件定义和业务编排逻辑。

### 2. `xuan-tenant`

职责：

- 依赖 `xuan-common-mq`。
- 在 `infrastructure/mq` 中提供一个示例 Producer。
- 在 `infrastructure/mq` 中提供一个示例 Consumer。

示例类只演示如何调用公共发送器、如何接收消息，不引入 Outbox 发布和数据库状态回写。

### 3. `xuan-iam`

职责与 `xuan-tenant` 相同：

- 依赖 `xuan-common-mq`
- 提供示例 Producer
- 提供示例 Consumer

## 配置设计

统一采用 `xuan.rocketmq` 前缀，而不是直接把业务方暴露到零散的 RocketMQ 原生配置键上。

建议保留以下配置项：

- `xuan.rocketmq.enabled`
- `xuan.rocketmq.name-server`
- `xuan.rocketmq.producer.group`
- `xuan.rocketmq.producer.send-timeout`
- `xuan.rocketmq.producer.max-message-size`
- `xuan.rocketmq.consumer.group-prefix`
- `xuan.rocketmq.topics.tenant-events`
- `xuan.rocketmq.topics.iam-events`

设计原则：

- 先保留最小必要配置，不一次性暴露过多高级参数。
- 公共模块只提供默认能力，业务服务保留按需覆盖空间。
- topic 名在公共配置中集中声明，避免散落硬编码。

## 类设计

`xuan-common-mq` 内建议包含以下核心类：

- `XuanRocketMqProperties`
- `RocketMqMessage<T>`
- `RocketMqMessageSender`
- `DefaultRocketMqMessageSender`
- `RocketMqMessageConverter`
- `XuanRocketMqAutoConfiguration`

职责划分：

- `XuanRocketMqProperties`：配置绑定与默认值
- `RocketMqMessage<T>`：统一消息模型，至少包含 `topic`、`tag`、`key`、`payload`、`headers`
- `RocketMqMessageSender`：业务依赖的统一发送接口
- `DefaultRocketMqMessageSender`：调用底层模板完成同步发送
- `RocketMqMessageConverter`：统一对象转 JSON / 字节数组
- `XuanRocketMqAutoConfiguration`：自动注册发送器、转换器和默认 Bean

业务模块示例类：

- `xuan-tenant.infrastructure.mq.TenantEventProducer`
- `xuan-tenant.infrastructure.mq.TenantEventConsumer`
- `xuan-iam.infrastructure.mq.IamEventProducer`
- `xuan-iam.infrastructure.mq.IamEventConsumer`

## 自动装配原则

自动装配需要遵循现有公共模块风格：

- 使用 `@AutoConfiguration`
- 使用条件装配，避免业务未启用时误注册 Bean
- 业务方自定义同名或同类型 Bean 时，公共默认实现应自动让位

这意味着：

- `enabled=false` 时，不注册默认发送器
- 业务服务若自定义 `RocketMqMessageSender`，公共默认实现不覆盖

## 发送模型设计

当前阶段统一只封装最常用的同步发送路径。

消息模型最少包含：

- `topic`
- `tag`
- `key`
- `payload`
- `headers`

本次不额外引入延时消息、顺序消息、事务消息、批量消息的统一抽象，避免骨架一开始就过重。

## 示例 Producer / Consumer 设计

示例 Producer 的目标是告诉业务开发者：

- 如何从配置里取 topic
- 如何组装统一消息对象
- 如何通过 `RocketMqMessageSender` 发消息

示例 Consumer 的目标是告诉业务开发者：

- RocketMQ 消费者类放在哪一层
- 收到消息后如何做最小解析
- 业务逻辑应继续下沉到应用服务或领域服务，而不是堆在消费入口里

因此示例 Consumer 只做最小演示，不承载真实业务编排。

## 测试策略

测试遵循“先测试、后实现”，但只验证基础骨架，不依赖真实 RocketMQ 服务端。

### `xuan-common-mq`

需要覆盖：

- `XuanRocketMqProperties` 配置绑定
- `RocketMqMessage` 的基本构造行为
- `DefaultRocketMqMessageSender` 调用底层模板的行为
- `XuanRocketMqAutoConfiguration` 的启用与禁用场景

### `xuan-tenant` 与 `xuan-iam`

各自提供轻量测试，覆盖：

- 示例 Producer Bean 能被装配
- 示例 Consumer Bean 能被装配

本次不做：

- 真实 NameServer 收发验证
- 真实消息消费确认
- 真实重试与异常补偿链路测试

## 实现边界

本次实现包含：

- 父 POM 增加 `xuan-common-mq` 模块
- 新建 `xuan-common-mq`
- RocketMQ 统一依赖
- 公共配置类
- 公共发送封装
- 自动装配
- `xuan-tenant` 示例 Producer / Consumer
- `xuan-iam` 示例 Producer / Consumer
- 基础测试
- 示例 YAML 配置

本次实现不包含：

- Outbox 发布器
- 消费幂等落库
- 死信处理
- 消费失败回写表状态
- 业务事件模型重构
- 真实部署和联调脚本

## 验收标准

- 后端父 POM 已纳入 `xuan-common-mq`
- `xuan-common-mq` 可以独立被业务服务依赖
- 业务服务不需要直接依赖底层 RocketMQ API 即可发送消息
- `xuan-tenant`、`xuan-iam` 均有可运行的示例 Producer / Consumer 骨架
- 自动装配在启用和禁用场景下行为清晰
- 所有新增测试在本地 Maven 下通过

