package com.xuan.erp.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RocketMqAvailabilityProbeTest {

    private static final String ENABLED_PROPERTY = "xuan.mq.probe.enabled";
    private static final String ENABLED_ENV = "XUAN_MQ_PROBE_ENABLED";

    // 真实连 RocketMQ 的探针测试。默认跳过，需要显式开启，避免普通单元测试依赖外部 MQ。
    @Test
    void sendsProbeMessageToConfiguredRocketMq() {
        assumeTrue(booleanValue(ENABLED_PROPERTY, ENABLED_ENV),
                "设置 -Dxuan.mq.probe.enabled=true 或 XUAN_MQ_PROBE_ENABLED=true 后才会真实探测 RocketMQ");

        String nameServer = requiredValue("xuan.rocketmq.name-server", "XUAN_ROCKETMQ_NAME_SERVER");
        String topic = value("xuan.mq.probe.topic", "XUAN_MQ_PROBE_TOPIC", "xuan-tenant-event");
        String tag = value("xuan.mq.probe.tag", "XUAN_MQ_PROBE_TAG", "MqAvailabilityProbe");
        int timeout = Integer.parseInt(value("xuan.mq.probe.timeout-ms", "XUAN_MQ_PROBE_TIMEOUT_MS", "5000"));
        String key = "mq-probe-" + UUID.randomUUID();

        DefaultMQProducer producer = new DefaultMQProducer("xuan-mq-probe-producer-" + UUID.randomUUID());
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(timeout);
        try {
            RocketMqSendResult result = new DefaultRocketMqMessageSender(producer, new ObjectMapper())
                    .send(new RocketMqMessage<>(
                            topic,
                            tag,
                            key,
                            Map.of(
                                    "probeKey", key,
                                    "occurredAt", OffsetDateTime.now().toString(),
                                    "source", "RocketMqAvailabilityProbeTest"),
                            Map.of("probe", "true")));

            assertThat(result.sendStatus())
                    .as("RocketMQ 发送状态，nameServer=%s, topic=%s, key=%s", nameServer, topic, key)
                    .isEqualTo("SEND_OK");
            assertThat(result.messageId()).isNotBlank();
        } finally {
            producer.shutdown();
        }
    }

    // 真实消费闭环探针：Producer 发送后，必须由 PushConsumer 实际收到同一条 key 的消息。
    @Test
    void sendsAndConsumesProbeMessageThroughConfiguredRocketMq() throws Exception {
        assumeTrue(booleanValue(ENABLED_PROPERTY, ENABLED_ENV),
                "设置 -Dxuan.mq.probe.enabled=true 或 XUAN_MQ_PROBE_ENABLED=true 后才会真实探测 RocketMQ");

        String nameServer = requiredValue("xuan.rocketmq.name-server", "XUAN_ROCKETMQ_NAME_SERVER");
        String topic = value("xuan.mq.probe.topic", "XUAN_MQ_PROBE_TOPIC", "xuan-tenant-event");
        String token = randomToken();
        String tag = value("xuan.mq.probe.tag", "XUAN_MQ_PROBE_TAG", "MqAvailabilityProbe") + "_" + token;
        String key = "mq-probe-consume-" + token;
        int timeout = Integer.parseInt(value("xuan.mq.probe.timeout-ms", "XUAN_MQ_PROBE_TIMEOUT_MS", "5000"));
        long waitSeconds = Long.parseLong(value(
                "xuan.mq.probe.consume-wait-seconds",
                "XUAN_MQ_PROBE_CONSUME_WAIT_SECONDS",
                "30"));
        ObjectMapper objectMapper = new ObjectMapper();
        CountDownLatch consumed = new CountDownLatch(1);
        AtomicReference<MessageExt> consumedMessage = new AtomicReference<>();

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("xuan-mq-probe-consumer-" + token);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe(topic, tag);
        MessageListenerConcurrently listener = (messages, context) -> {
            for (MessageExt message : messages) {
                if (key.equals(message.getKeys())) {
                    consumedMessage.set(message);
                    consumed.countDown();
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        };
        consumer.registerMessageListener(listener);

        DefaultMQProducer producer = new DefaultMQProducer("xuan-mq-probe-producer-" + token);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(timeout);
        try {
            consumer.start();

            RocketMqSendResult result = new DefaultRocketMqMessageSender(producer, objectMapper)
                    .send(new RocketMqMessage<>(
                            topic,
                            tag,
                            key,
                            Map.of(
                                    "probeKey", key,
                                    "occurredAt", OffsetDateTime.now().toString(),
                                    "source", "RocketMqAvailabilityProbeTest"),
                            Map.of("probe", "true")));

            assertThat(result.sendStatus())
                    .as("RocketMQ 发送状态，nameServer=%s, topic=%s, tag=%s, key=%s", nameServer, topic, tag, key)
                    .isEqualTo("SEND_OK");
            assertThat(consumed.await(waitSeconds, TimeUnit.SECONDS))
                    .as("RocketMQ 消费闭环，nameServer=%s, topic=%s, tag=%s, key=%s, waitSeconds=%s",
                            nameServer, topic, tag, key, waitSeconds)
                    .isTrue();

            MessageExt message = consumedMessage.get();
            assertThat(message).isNotNull();
            assertThat(message.getTopic()).isEqualTo(topic);
            assertThat(message.getTags()).isEqualTo(tag);
            assertThat(message.getKeys()).isEqualTo(key);
            assertThat(objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8)).path("probeKey").asText())
                    .isEqualTo(key);
        } finally {
            consumer.shutdown();
            producer.shutdown();
        }
    }

    private static boolean booleanValue(String propertyName, String envName) {
        return Boolean.parseBoolean(value(propertyName, envName, "false"));
    }

    private static String requiredValue(String propertyName, String envName) {
        String value = value(propertyName, envName, null);
        assertThat(value)
                .as("必须通过 -D%s 或环境变量 %s 配置 RocketMQ NameServer", propertyName, envName)
                .isNotBlank();
        return value.trim();
    }

    private static String value(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        return defaultValue;
    }

    private static String randomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
