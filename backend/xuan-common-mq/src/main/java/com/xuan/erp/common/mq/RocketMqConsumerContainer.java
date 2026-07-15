package com.xuan.erp.common.mq;

import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * 根据业务声明的 {@link RocketMqMessageHandler} 启动 RocketMQ PushConsumer。
 */
public class RocketMqConsumerContainer implements SmartLifecycle {

    private final List<RocketMqMessageHandler> handlers;
    private final XuanRocketMqProperties properties;
    private final List<DefaultMQPushConsumer> consumers = new CopyOnWriteArrayList<>();
    private volatile boolean running;

    public RocketMqConsumerContainer(List<RocketMqMessageHandler> handlers, XuanRocketMqProperties properties) {
        this.handlers = new ArrayList<>(handlers);
        this.properties = properties;
    }

    public int handlerCount() {
        return handlers.size();
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        Assert.hasText(properties.getNameServer(), "xuan.rocketmq.name-server 不能为空");
        try {
            for (RocketMqMessageHandler handler : handlers) {
                consumers.add(startConsumer(handler));
            }
            running = true;
        } catch (MQClientException error) {
            stop();
            throw new IllegalStateException("启动 RocketMQ Consumer 失败", error);
        }
    }

    private DefaultMQPushConsumer startConsumer(RocketMqMessageHandler handler) throws MQClientException {
        Assert.hasText(handler.consumerGroup(), "RocketMQ consumer group 不能为空");
        Assert.hasText(handler.topic(), "RocketMQ consumer topic 不能为空");
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(handler.consumerGroup());
        consumer.setNamesrvAddr(properties.getNameServer());
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.subscribe(handler.topic(), tagExpression(handler));
        MessageListenerConcurrently listener = (messages, context) -> {
            try {
                for (MessageExt message : messages) {
                    handler.handle(readPayload(message), headers(message));
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (RuntimeException error) {
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        };
        consumer.registerMessageListener(listener);
        consumer.start();
        return consumer;
    }

    @Override
    public void stop() {
        for (DefaultMQPushConsumer consumer : consumers) {
            consumer.shutdown();
        }
        consumers.clear();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.getConsumer().isAutoStartup();
    }

    private String tagExpression(RocketMqMessageHandler handler) {
        String expression = handler.tagExpression();
        return expression == null || expression.isBlank() ? "*" : expression.trim();
    }

    private String readPayload(MessageExt message) {
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }

    private Map<String, String> headers(MessageExt message) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (message.getKeys() != null) {
            headers.put("keys", message.getKeys());
        }
        if (message.getTags() != null) {
            headers.put("tags", message.getTags());
        }
        for (Map.Entry<String, String> property : message.getProperties().entrySet()) {
            if (property.getKey() != null && property.getValue() != null) {
                headers.put(property.getKey(), property.getValue());
            }
        }
        return headers;
    }
}
