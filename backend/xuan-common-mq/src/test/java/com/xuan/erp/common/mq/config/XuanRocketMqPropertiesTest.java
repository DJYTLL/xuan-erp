package com.xuan.erp.common.mq.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class XuanRocketMqPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    // 测试 xuan.rocketmq 前缀下的基础连接参数和生产者分组可以被统一绑定。
    @Test
    void bindsBasicRocketMqProperties() {
        contextRunner
                .withPropertyValues(
                        "xuan.rocketmq.enabled=true",
                        "xuan.rocketmq.name-server=duaoyunxuan.com:9047",
                        "xuan.rocketmq.access-key=test-access-key",
                        "xuan.rocketmq.secret-key=test-secret-key",
                        "xuan.rocketmq.producer.group=xuan-test-producer",
                        "xuan.rocketmq.producer.send-timeout=3000",
                        "xuan.rocketmq.producer.max-message-size=4194304",
                        "xuan.rocketmq.topics.tenant-events=xuan-tenant-event",
                        "xuan.rocketmq.topics.iam-events=xuan-iam-event")
                .run(context -> {
                    XuanRocketMqProperties properties = context.getBean(XuanRocketMqProperties.class);

                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getNameServer()).isEqualTo("duaoyunxuan.com:9047");
                    assertThat(properties.getAccessKey()).isEqualTo("test-access-key");
                    assertThat(properties.getSecretKey()).isEqualTo("test-secret-key");
                    assertThat(properties.getProducer().getGroup()).isEqualTo("xuan-test-producer");
                    assertThat(properties.getProducer().getSendTimeout()).isEqualTo(3000);
                    assertThat(properties.getProducer().getMaxMessageSize()).isEqualTo(4194304);
                    assertThat(properties.getTopics().getTenantEvents()).isEqualTo("xuan-tenant-event");
                    assertThat(properties.getTopics().getIamEvents()).isEqualTo("xuan-iam-event");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(XuanRocketMqProperties.class)
    static class TestConfiguration {
    }
}
