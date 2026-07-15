package com.xuan.erp.tenant.application.command;

import java.util.Map;

public record TenantProvisionCallbackCommand(
        String eventId,
        String eventType,
        String taskKey,
        String idempotencyKey,
        String provisionStep,
        boolean success,
        String errorCode,
        String errorMessage,
        Map<String, Object> resultPayload,
        String operator
) {
}
