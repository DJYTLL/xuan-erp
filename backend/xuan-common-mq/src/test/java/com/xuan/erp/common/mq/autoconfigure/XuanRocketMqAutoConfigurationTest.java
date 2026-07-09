package com.xuan.erp.common.mq.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.mq.RocketMqMessageSender;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class XuanRocketMqAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(XuanRocketMqAutoConfiguration.class));

    // 测试启用 xuan.rocketmq 后会自动注册 Producer、ObjectMapper 和统一发送器。
    @Test
    void registersDefaultProducerAndSenderWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "xuan.rocketmq.enabled=true",
                        "xuan.rocketmq.name-server=duaoyunxuan.com:9047",
                        "xuan.rocketmq.producer.group=xuan-test-producer",
                        "xuan.rocketmq.producer.send-timeout=3000",
                        "xuan.rocketmq.producer.max-message-size=4194304")
                .run(context -> {
                    assertThat(context).hasSingleBean(DefaultMQProducer.class);
                    assertThat(context).hasSingleBean(RocketMqMessageSender.class);
                    assertThat(context).hasSingleBean(ObjectMapper.class);

                    DefaultMQProducer producer = context.getBean(DefaultMQProducer.class);
                    assertThat(producer.getProducerGroup()).isEqualTo("xuan-test-producer");
                    assertThat(producer.getNamesrvAddr()).isEqualTo("duaoyunxuan.com:9047");
                    assertThat(producer.getSendMsgTimeout()).isEqualTo(3000);
                    assertThat(producer.getMaxMessageSize()).isEqualTo(4194304);
                });
    }

    // 测试未启用时不会误注册默认 RocketMQ 发送相关 Bean。
    @Test
    void doesNotRegisterProducerOrSenderWhenDisabled() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(DefaultMQProducer.class);
            assertThat(context).doesNotHaveBean(RocketMqMessageSender.class);
        });
    }

    // 测试业务方自定义发送器时，公共自动配置会自动让位。
    @Test
    void backsOffWhenCustomSenderExists() {
        contextRunner
                .withPropertyValues(
                        "xuan.rocketmq.enabled=true",
                        "xuan.rocketmq.name-server=duaoyunxuan.com:9047",
                        "xuan.rocketmq.producer.group=xuan-test-producer")
                .withUserConfiguration(CustomSenderConfiguration.class)
                .run(context -> assertThat(context).getBean(RocketMqMessageSender.class)
                        .isSameAs(context.getBean("customRocketMqMessageSender")));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSenderConfiguration {

        @Bean
        RocketMqMessageSender customRocketMqMessageSender() {
            return new RocketMqMessageSender() {
                @Override
                public <T> com.xuan.erp.common.mq.RocketMqSendResult send(com.xuan.erp.common.mq.RocketMqMessage<T> message) {
                    return null;
                }
            };
        }
    }
}
