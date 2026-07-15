package com.xuan.erp.tenant.application.query;

import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStepStatus;

public record TenantProvisionTaskStepView(
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
