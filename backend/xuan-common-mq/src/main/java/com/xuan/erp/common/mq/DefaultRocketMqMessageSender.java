package com.xuan.erp.common.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于原生 {@link DefaultMQProducer} 的默认统一消息发送实现。
 */
public class DefaultRocketMqMessageSender implements RocketMqMessageSender {

    private final DefaultMQProducer producer;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public DefaultRocketMqMessageSender(DefaultMQProducer producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> RocketMqSendResult send(RocketMqMessage<T> message) {
        Assert.notNull(message, "RocketMQ message 不能为空");
        ensureStarted();
        try {
            SendResult sendResult = producer.send(toRocketMqMessage(message));
            return new RocketMqSendResult(
                    sendResult.getSendStatus().name(),
                    sendResult.getMsgId(),
                    sendResult.getMessageQueue() == null ? -1 : sendResult.getMessageQueue().getQueueId(),
                    sendResult.getQueueOffset());
        } catch (MQClientException | MQBrokerException | RemotingException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("发送 RocketMQ 消息失败", ex);
        }
    }

    private <T> Message toRocketMqMessage(RocketMqMessage<T> message) {
        Message rocketMqMessage = new Message();
        rocketMqMessage.setTopic(message.topic());
        if (hasText(message.tag())) {
            rocketMqMessage.setTags(message.tag());
        }
        if (hasText(message.key())) {
            rocketMqMessage.setKeys(message.key());
        }
        rocketMqMessage.setBody(writePayload(message.payload()));
        for (Map.Entry<String, String> headerEntry : message.headers().entrySet()) {
            if (hasText(headerEntry.getKey()) && headerEntry.getValue() != null) {
                rocketMqMessage.putUserProperty(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        return rocketMqMessage;
    }

    private byte[] writePayload(Object payload) {
        if (payload instanceof byte[] bytes) {
            return bytes;
        }
        if (payload instanceof String text) {
            return text.getBytes(StandardCharsets.UTF_8);
        }
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化 RocketMQ 消息负载失败", ex);
        }
    }

    private void ensureStarted() {
        if (started.compareAndSet(false, true)) {
            try {
                producer.start();
            } catch (MQClientException ex) {
                started.set(false);
                throw new IllegalStateException("启动 RocketMQ Producer 失败", ex);
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
