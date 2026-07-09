package com.xuan.erp.common.mq;

import java.util.Map;
import java.util.Objects;

/**
 * Xuan ERP 统一 RocketMQ 消息模型。
 *
 * @param topic   目标 topic
 * @param tag     业务 tag
 * @param key     业务唯一键
 * @param payload 消息负载
 * @param headers 自定义消息头
 * @param <T>     负载类型
 */
public record RocketMqMessage<T>(
        String topic,
        String tag,
        String key,
        T payload,
        Map<String, String> headers) {

    public RocketMqMessage {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("RocketMQ topic 不能为空");
        }
        Objects.requireNonNull(payload, "RocketMQ payload 不能为空");
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
