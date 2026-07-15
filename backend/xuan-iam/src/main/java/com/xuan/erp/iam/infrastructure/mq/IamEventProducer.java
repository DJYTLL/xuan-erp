package com.xuan.erp.iam.infrastructure.mq;

import com.xuan.erp.common.mq.RocketMqMessage;
import com.xuan.erp.common.mq.RocketMqMessageSender;
import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import java.util.LinkedHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * IAM 服务示例 Producer，演示业务模块如何通过公共发送器发布消息。
 */
@Component
@ConditionalOnProperty(prefix = "xuan.rocketmq", name = "enabled", havingValue = "true")
public class IamEventProducer {

    private static final String USER_CREATED_TAG = "iam.user-created";
    private static final String EVENT_IAM_STEP_COMPLETED = "TenantIamProvisionStepCompleted";
    private static final String EVENT_IAM_STEP_FAILED = "TenantIamProvisionStepFailed";

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

    /**
     * 发布 IAM 初始化步骤成功回执，供 xuan-tenant 推进初始化任务。
     */
    public void publishTenantProvisionStepCompleted(
            Long tenantId,
            String taskKey,
            String idempotencyKey,
            String provisionStep,
            Integer menuGrantCount) {
        Map<String, Object> resultPayload = new LinkedHashMap<>();
        resultPayload.put("menuGrantCount", menuGrantCount == null ? 0 : menuGrantCount);
        publishTenantProvisionResult(
                EVENT_IAM_STEP_COMPLETED,
                tenantId,
                taskKey,
                idempotencyKey,
                provisionStep,
                null,
                null,
                resultPayload);
    }

    /**
     * 发布 IAM 初始化步骤失败回执，供 xuan-tenant 标记初始化失败。
     */
    public void publishTenantProvisionStepFailed(
            Long tenantId,
            String taskKey,
            String idempotencyKey,
            String provisionStep,
            String errorCode,
            String errorMessage) {
        publishTenantProvisionResult(
                EVENT_IAM_STEP_FAILED,
                tenantId,
                taskKey,
                idempotencyKey,
                provisionStep,
                errorCode,
                errorMessage,
                Map.of());
    }

    private void publishTenantProvisionResult(
            String eventType,
            Long tenantId,
            String taskKey,
            String idempotencyKey,
            String provisionStep,
            String errorCode,
            String errorMessage,
            Map<String, Object> resultPayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", eventType);
        payload.put("tenantId", tenantId);
        payload.put("taskKey", taskKey);
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("provisionStep", provisionStep);
        payload.put("sourceService", "xuan-iam");
        payload.put("resultPayload", resultPayload);
        if (errorCode != null) {
            payload.put("errorCode", errorCode);
        }
        if (errorMessage != null) {
            payload.put("errorMessage", errorMessage);
        }
        rocketMqMessageSender.send(new RocketMqMessage<>(
                rocketMqProperties.getTopics().getTenantEvents(),
                eventType,
                taskKey,
                payload,
                Map.of("source", "xuan-iam")));
    }
}
