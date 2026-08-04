package com.xuan.erp.iam.infrastructure.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.mq.RocketMqMessageHandler;
import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import com.xuan.erp.iam.application.command.BootstrapTenantAdminCommand;
import com.xuan.erp.iam.application.service.IamTenantBootstrapApplicationService;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * IAM 租户初始化请求 Consumer，消费 xuan-tenant 发出的 IAM_BOOTSTRAP 请求。
 */
@Component
@ConditionalOnProperty(prefix = "xuan.rocketmq", name = "enabled", havingValue = "true")
public class IamTenantProvisionConsumer implements RocketMqMessageHandler {

    private static final String EVENT_IAM_BOOTSTRAP_REQUESTED = "TenantIamBootstrapRequested";
    private static final String IAM_BOOTSTRAP_STEP = "IAM_BOOTSTRAP";

    private final IamTenantBootstrapApplicationService bootstrapApplicationService;
    private final IamEventProducer eventProducer;
    private final XuanRocketMqProperties rocketMqProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IamTenantProvisionConsumer(
            IamTenantBootstrapApplicationService bootstrapApplicationService,
            IamEventProducer eventProducer,
            XuanRocketMqProperties rocketMqProperties) {
        this.bootstrapApplicationService = bootstrapApplicationService;
        this.eventProducer = eventProducer;
        this.rocketMqProperties = rocketMqProperties;
    }

    @Override
    public String consumerGroup() {
        return "xuan-iam-tenant-provision-consumer";
    }

    @Override
    public String topic() {
        return rocketMqProperties.getTopics().getTenantEvents();
    }

    @Override
    public String tagExpression() {
        return EVENT_IAM_BOOTSTRAP_REQUESTED;
    }

    @Override
    public void handle(String payload, Map<String, String> headers) {
        handleTenantProvisionRequest(payload);
    }

    public String handleTenantProvisionRequest(String payload) {
        Map<String, Object> event = parsePayload(payload);
        if (!supports(event)) {
            return payload;
        }
        Long tenantId = numberAsLong(event.get("tenantId"));
        String taskKey = text(event.get("taskKey"));
        String idempotencyKey = text(event.get("idempotencyKey"));
        String provisionStep = text(event.get("provisionStep"));
        boolean callbackRequired = booleanValue(event.get("callbackRequired"), true);
        try {
            Integer menuGrantCount = bootstrapApplicationService.bootstrapTenant(
                    tenantId,
                    new BootstrapTenantAdminCommand(
                            text(event.get("adminUsername")),
                            text(event.get("adminPasswordHash")),
                            text(event.get("adminDisplayName")),
                            text(event.get("adminEmail")),
                            text(event.get("adminPhone")),
                            text(event.get("adminPasswordHash")) != null),
                    text(event.get("iamInitTemplateCode")),
                    stringList(event.get("columnPermissionTemplateCodes")),
                    text(event.get("defaultColumnPermissionTemplateCode")),
                    text(event.get("permissionHash")),
                    "xuan-tenant");
            if (!callbackRequired) {
                return EVENT_IAM_BOOTSTRAP_REQUESTED;
            }
            eventProducer.publishTenantProvisionStepCompleted(
                    tenantId,
                    taskKey,
                    idempotencyKey,
                    provisionStep,
                    menuGrantCount);
            return "TenantIamProvisionStepCompleted";
        } catch (RuntimeException error) {
            if (!callbackRequired) {
                throw error;
            }
            eventProducer.publishTenantProvisionStepFailed(
                    tenantId,
                    taskKey,
                    idempotencyKey,
                    provisionStep,
                    "IAM_BOOTSTRAP_FAILED",
                    error.getMessage());
            return "TenantIamProvisionStepFailed";
        }
    }

    private boolean supports(Map<String, Object> event) {
        String eventType = text(event.get("eventType"));
        String provisionStep = text(event.get("provisionStep"));
        return (eventType == null || EVENT_IAM_BOOTSTRAP_REQUESTED.equals(eventType))
                && IAM_BOOTSTRAP_STEP.equals(provisionStep)
                && numberAsLong(event.get("tenantId")) != null;
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Long numberAsLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return defaultValue;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> items)) {
            return null;
        }
        return items.stream()
                .map(this::text)
                .filter(item -> item != null && !item.isBlank())
                .toList();
    }
}
