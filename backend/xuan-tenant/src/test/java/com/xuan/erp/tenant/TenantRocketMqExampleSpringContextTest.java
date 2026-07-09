package com.xuan.erp.tenant;

import com.xuan.erp.common.mq.RocketMqMessageSender;
import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import com.xuan.erp.tenant.infrastructure.mq.TenantEventConsumer;
import com.xuan.erp.tenant.infrastructure.mq.TenantEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRocketMqExampleSpringContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "xuan.rocketmq.enabled=true",
                    "xuan.rocketmq.name-server=duaoyunxuan.com:9047",
                    "xuan.rocketmq.producer.group=xuan-tenant-producer",
                    "xuan.rocketmq.topics.tenant-events=xuan-tenant-event",
                    "xuan.rocketmq.topics.iam-events=xuan-iam-event")
            .withUserConfiguration(TenantRocketMqExampleConfiguration.class);

    // 测试租户服务的示例 Producer 和 Consumer 都能被 Spring 上下文装配。
    @Test
    void createsTenantRocketMqExampleBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantEventProducer.class);
            assertThat(context).hasSingleBean(TenantEventConsumer.class);

            TenantEventProducer producer = context.getBean(TenantEventProducer.class);
            TenantEventConsumer consumer = context.getBean(TenantEventConsumer.class);
            RecordingRocketMqMessageSender sender = context.getBean(RecordingRocketMqMessageSender.class);

            producer.publishTenantCreatedEvent(1L);

            assertThat(sender.lastMessage).isNotNull();
            assertThat(sender.lastMessage.topic()).isEqualTo("xuan-tenant-event");
            assertThat(sender.lastMessage.tag()).isEqualTo("tenant.created");
            assertThat(sender.lastMessage.key()).isEqualTo("tenant:1");
            assertThat(sender.lastMessage.payload().toString()).contains("tenantId=1");

            assertThat(consumer.handleTenantEvent("{\"tenantId\":1}"))
                    .isEqualTo("{\"tenantId\":1}");
        });
    }

    // 测试未启用 RocketMQ 时，租户服务上下文不会因为示例 Producer 缺少发送器而启动失败。
    @Test
    void skipsTenantEventProducerWhenRocketMqSenderIsMissing() {
        new ApplicationContextRunner()
                .withUserConfiguration(TenantRocketMqExampleWithoutSenderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(TenantEventConsumer.class);
                    assertThat(context).doesNotHaveBean(TenantEventProducer.class);
                    assertThat(context).doesNotHaveBean(RocketMqMessageSender.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(XuanRocketMqProperties.class)
    @Import({TenantEventProducer.class, TenantEventConsumer.class})
    static class TenantRocketMqExampleConfiguration {

        @Bean
        RecordingRocketMqMessageSender rocketMqMessageSender() {
            return new RecordingRocketMqMessageSender();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({TenantEventProducer.class, TenantEventConsumer.class})
    static class TenantRocketMqExampleWithoutSenderConfiguration {
    }

    static class RecordingRocketMqMessageSender implements RocketMqMessageSender {

        private com.xuan.erp.common.mq.RocketMqMessage<?> lastMessage;

        @Override
        public <T> com.xuan.erp.common.mq.RocketMqSendResult send(com.xuan.erp.common.mq.RocketMqMessage<T> message) {
            this.lastMessage = message;
            return new com.xuan.erp.common.mq.RocketMqSendResult("SEND_OK", "msg-tenant-001", 0, 0L);
        }
    }
}
