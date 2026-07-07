---
title: "事件驱动"
---

核心业务跨服务一致性优先使用领域事件、Outbox、RocketMQ、幂等消费和补偿任务。

Redis 缓存和 Elasticsearch 索引也通过事件驱动更新。业务服务写入主库后写 Outbox 事件，事件进入 RocketMQ，再由 `xuan-query` 消费并更新读模型表、Redis 缓存和 Elasticsearch 索引。

Elasticsearch 索引是搜索型读模型，不是事实来源。索引更新允许最终一致，但必须支持幂等、补偿重建和历史数据回填。

## 待补充

- 事件命名规范。
- 事件字段规范。
- Outbox 表结构。
- 消费幂等策略。
- Redis 缓存失效策略。
- Elasticsearch 索引重建和回填策略。



