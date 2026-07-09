package com.xuan.erp.iam.infrastructure.mq;

import com.xuan.erp.common.mq.RocketMqMessage;
import com.xuan.erp.common.mq.RocketMqMessageSender;
import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * IAM 服务示例 Producer，演示业务模块如何通过公共发送器发布消息。
 */
@Component
@ConditionalOnProperty(prefix = "xuan.rocketmq", name = "enabled", havingValue = "true")
public class IamEventProducer {

    private static final String USER_CREATED_TAG = "iam.user-created";

    private final RocketMqMessageSender rocketMqMessageSender;
    private final XuanRocketMqProperties rocketMqProperties;

    public IamEventProducer(RocketMqMessageSender rocketMqMessageSender,
                            XuanRocketMqProperties rocketMqProperties) {
        this.rocketMqMessageSender = rocketMqMessageSender;
        this.rocketMqProperties = rocketMqProperties;
    }

    /**
     * 发布“IAM 用户已创建”示例事件。
     *
     * @param userId 用户 ID
     */
    public void publishUserCreatedEvent(Long userId) {
        rocketMqMessageSender.send(new RocketMqMessage<>(
                rocketMqProperties.getTopics().getIamEvents(),
                USER_CREATED_TAG,
                "iam-user:" + userId,
                Map.of("userId", userId, "eventType", USER_CREATED_TAG),
                Map.of("source", "xuan-iam")));
    }
}
