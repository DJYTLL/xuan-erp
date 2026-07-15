package com.xuan.erp.tenant.infrastructure.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.mq.RocketMqMessageHandler;
import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import com.xuan.erp.tenant.application.command.TenantProvisionCallbackCommand;
import com.xuan.erp.tenant.application.service.TenantProvisionApplicationService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * 租户服务 Consumer 入口，负责把 IAM 初始化回执交给租户初始化编排服务处理。
 */
@Component
public class TenantEventConsumer implements RocketMqMessageHandler {

    private static final String EVENT_IAM_STEP_COMPLETED = "TenantIamProvisionStepCompleted";
    private static final String EVENT_IAM_STEP_FAILED = "TenantIamProvisionStepFailed";

    private final TenantProvisionApplicationService provisionApplicationService;
    private final XuanRocketMqProperties rocketMqProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TenantEventConsumer() {
        this(null, null);
    }

    @Autowired
    public TenantEventConsumer(@Nullable TenantProvisionApplicationService provisionApplicationService) {
        this(provisionApplicationService, null);
    }

    public TenantEventConsumer(
            @Nullable TenantProvisionApplicationService provisionApplicationService,
            @Nullable XuanRocketMqProperties rocketMqProperties) {
        this.provisionApplicationService = provisionApplicationService;
        this.rocketMqProperties = rocketMqProperties;
    }

    @Override
    public String consumerGroup() {
        return "xuan-tenant-iam-provision-result-consumer";
    }

    @Override
    public String topic() {
        if (rocketMqProperties != null && rocketMqProperties.getTopics().getTenantEvents() != null) {
            return rocketMqProperties.getTopics().getTenantEvents();
        }
        return "xuan-tenant-event";
    }

    @Override
    public String tagExpression() {
        return EVENT_IAM_STEP_COMPLETED + " || " + EVENT_IAM_STEP_FAILED;
    }

    @Override
    public void handle(String payload, Map<String, String> headers) {
        handleTenantEvent(payload);
    }

    /**
     * 处理收到的租户事件原始消息。非 IAM 初始化回执保持原样返回，便于后续扩展其他事件。
     *
     * @param payload 原始消息内容
     * @return 已处理的事件类型，或原始消息内容
     */
    public String handleTenantEvent(String payload) {
        Map<String, Object> event = parsePayload(payload);
        String eventType = text(event.get("eventType"));
        if (!EVENT_IAM_STEP_COMPLETED.equals(eventType) && !EVENT_IAM_STEP_FAILED.equals(eventType)) {
            return payload;
        }
        if (provisionApplicationService == null) {
            return payload;
        }

        Long tenantId = numberAsLong(event.get("tenantId"));
        if (tenantId == null) {
            return payload;
        }

        provisionApplicationService.handleProvisionCallback(
                tenantId,
                new TenantProvisionCallbackCommand(
                        text(event.get("eventId")),
                        eventType,
                        text(event.get("taskKey")),
                        text(event.get("idempotencyKey")),
                        text(event.get("provisionStep")),
                        EVENT_IAM_STEP_COMPLETED.equals(eventType),
                        text(event.get("errorCode")),
                        text(event.get("errorMessage")),
                        mapValue(event.get("resultPayload")),
                        textOrDefault(event.get("sourceService"), "xuan-iam")
                ));
        return eventType;
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

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String textOrDefault(Object value, String defaultValue) {
        String text = text(value);
        return text == null || text.isBlank() ? defaultValue : text;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
