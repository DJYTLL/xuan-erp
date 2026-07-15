package com.xuan.erp.common.mq;

import java.util.Map;

/**
 * 业务模块声明 RocketMQ 消费入口的统一接口。
 */
public interface RocketMqMessageHandler {

    /**
     * 当前消费者组。
     */
    String consumerGroup();

    /**
     * 订阅 topic。
     */
    String topic();

    /**
     * 订阅 tag 表达式，未配置时使用 *。
     */
    String tagExpression();

    /**
     * 处理消息正文和头信息。
     */
    void handle(String payload, Map<String, String> headers);
}
