package com.xuan.erp.tenant.interfaces.dto;

import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStatus;
import java.util.List;

public record TenantProvisionTaskResponse(
        Long id,
        Long tenantId,
        String taskKey,
        String taskType,
        ProvisionTaskStatus status,
        List<TenantProvisionTaskStepResponse> steps
) {
}
