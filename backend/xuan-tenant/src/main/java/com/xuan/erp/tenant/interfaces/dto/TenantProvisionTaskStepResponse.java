package com.xuan.erp.tenant.interfaces.dto;

import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStepStatus;

public record TenantProvisionTaskStepResponse(
        Long id,
        Long provisionTaskId,
        String stepKey,
        String stepName,
        ProvisionTaskStepStatus status,
        int sequenceNo,
        String lastErrorCode,
        String lastErrorMessage
) {
}
