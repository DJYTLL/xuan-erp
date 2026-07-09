package com.xuan.erp.common.mq.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.mq.DefaultRocketMqMessageSender;
import com.xuan.erp.common.mq.RocketMqMessageSender;
import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

/**
 * Xuan ERP RocketMQ 公共自动装配。
 */
@AutoConfiguration
@EnableConfigurationProperties(XuanRocketMqProperties.class)
@ConditionalOnClass({DefaultMQProducer.class, ObjectMapper.class})
public class XuanRocketMqAutoConfiguration {

    /**
     * 提供默认 ObjectMapper，业务方自定义时自动让位。
     */
    @Bean
    @ConditionalOnMissingBean
    ObjectMapper xuanRocketMqObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * 在显式启用 RocketMQ 时创建默认 Producer。
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "xuan.rocketmq", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(DefaultMQProducer.class)
    DefaultMQProducer xuanRocketMqProducer(XuanRocketMqProperties properties) {
        Assert.hasText(properties.getNameServer(), "xuan.rocketmq.name-server 不能为空");
        Assert.hasText(properties.getProducer().getGroup(), "xuan.rocketmq.producer.group 不能为空");

        DefaultMQProducer producer = new DefaultMQProducer(properties.getProducer().getGroup());
        producer.setNamesrvAddr(properties.getNameServer());
        if (properties.getProducer().getSendTimeout() != null) {
            producer.setSendMsgTimeout(properties.getProducer().getSendTimeout());
        }
        if (properties.getProducer().getMaxMessageSize() != null) {
            producer.setMaxMessageSize(properties.getProducer().getMaxMessageSize());
        }
        return producer;
    }

    /**
     * 注册默认统一发送器，业务自定义发送器时自动让位。
     */
    @Bean
    @ConditionalOnProperty(prefix = "xuan.rocketmq", name = "enabled", havingValue = "true")
    @ConditionalOnBean(DefaultMQProducer.class)
    @ConditionalOnMissingBean(RocketMqMessageSender.class)
    RocketMqMessageSender xuanRocketMqMessageSender(DefaultMQProducer producer, ObjectMapper objectMapper) {
        return new DefaultRocketMqMessageSender(producer, objectMapper);
    }
}
