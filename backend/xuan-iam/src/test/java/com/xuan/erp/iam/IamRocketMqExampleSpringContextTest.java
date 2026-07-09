package com.xuan.erp.iam;

import com.xuan.erp.common.mq.RocketMqMessageSender;
import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import com.xuan.erp.iam.infrastructure.mq.IamEventConsumer;
import com.xuan.erp.iam.infrastructure.mq.IamEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class IamRocketMqExampleSpringContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "xuan.rocketmq.enabled=true",
                    "xuan.rocketmq.name-server=duaoyunxuan.top:9047",
                    "xuan.rocketmq.producer.group=xuan-iam-producer",
                    "xuan.rocketmq.topics.tenant-events=xuan-tenant-event",
                    "xuan.rocketmq.topics.iam-events=xuan-iam-event")
            .withUserConfiguration(IamRocketMqExampleConfiguration.class);

    // 测试 IAM 服务的示例 Producer 和 Consumer 都能被 Spring 上下文装配。
    @Test
    void createsIamRocketMqExampleBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IamEventProducer.class);
            assertThat(context).hasSingleBean(IamEventConsumer.class);

            IamEventProducer producer = context.getBean(IamEventProducer.class);
            IamEventConsumer consumer = context.getBean(IamEventConsumer.class);
            RecordingRocketMqMessageSender sender = context.getBean(RecordingRocketMqMessageSender.class);

            producer.publishUserCreatedEvent(1001L);

            assertThat(sender.lastMessage).isNotNull();
            assertThat(sender.lastMessage.topic()).isEqualTo("xuan-iam-event");
            assertThat(sender.lastMessage.tag()).isEqualTo("iam.user-created");
            assertThat(sender.lastMessage.key()).isEqualTo("iam-user:1001");
            assertThat(sender.lastMessage.payload().toString()).contains("userId=1001");

            assertThat(consumer.handleIamEvent("{\"userId\":1001}"))
                    .isEqualTo("{\"userId\":1001}");
        });
    }

    // 测试未启用 RocketMQ 时，IAM 服务上下文不会因为示例 Producer 缺少发送器而启动失败。
    @Test
    void skipsIamEventProducerWhenRocketMqSenderIsMissing() {
        new ApplicationContextRunner()
                .withUserConfiguration(IamRocketMqExampleWithoutSenderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(IamEventConsumer.class);
                    assertThat(context).doesNotHaveBean(IamEventProducer.class);
                    assertThat(context).doesNotHaveBean(RocketMqMessageSender.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(XuanRocketMqProperties.class)
    @Import({IamEventProducer.class, IamEventConsumer.class})
    static class IamRocketMqExampleConfiguration {

        @Bean
        RecordingRocketMqMessageSender rocketMqMessageSender() {
            return new RecordingRocketMqMessageSender();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({IamEventProducer.class, IamEventConsumer.class})
    static class IamRocketMqExampleWithoutSenderConfiguration {
    }

    static class RecordingRocketMqMessageSender implements RocketMqMessageSender {

        private com.xuan.erp.common.mq.RocketMqMessage<?> lastMessage;

        @Override
        public <T> com.xuan.erp.common.mq.RocketMqSendResult send(com.xuan.erp.common.mq.RocketMqMessage<T> message) {
            this.lastMessage = message;
            return new com.xuan.erp.common.mq.RocketMqSendResult("SEND_OK", "msg-iam-001", 0, 0L);
        }
    }
}
