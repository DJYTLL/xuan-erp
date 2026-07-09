package com.xuan.erp.tenant.infrastructure.mq;

import com.xuan.erp.common.mq.RocketMqMessage;
import com.xuan.erp.common.mq.RocketMqMessageSender;
import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 租户服务示例 Producer，演示业务模块如何通过公共发送器发布消息。
 */
@Component
@ConditionalOnProperty(prefix = "xuan.rocketmq", name = "enabled", havingValue = "true")
public class TenantEventProducer {

    private static final String TENANT_CREATED_TAG = "tenant.created";

    private final RocketMqMessageSender rocketMqMessageSender;
    private final XuanRocketMqProperties rocketMqProperties;

    public TenantEventProducer(RocketMqMessageSender rocketMqMessageSender,
                               XuanRocketMqProperties rocketMqProperties) {
        this.rocketMqMessageSender = rocketMqMessageSender;
        this.rocketMqProperties = rocketMqProperties;
    }

    /**
     * 发布“租户已创建”示例事件。
     *
     * @param tenantId 租户 ID
     */
    public void publishTenantCreatedEvent(Long tenantId) {
        rocketMqMessageSender.send(new RocketMqMessage<>(
                rocketMqProperties.getTopics().getTenantEvents(),
                TENANT_CREATED_TAG,
                "tenant:" + tenantId,
                Map.of("tenantId", tenantId, "eventType", TENANT_CREATED_TAG),
                Map.of("source", "xuan-tenant")));
    }
}
