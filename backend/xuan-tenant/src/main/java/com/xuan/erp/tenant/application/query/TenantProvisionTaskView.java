package com.xuan.erp.tenant.application.query;

import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStatus;
import java.util.List;

public record TenantProvisionTaskView(
        Long id,
        Long tenantId,
        String taskKey,
        String taskType,
        ProvisionTaskStatus status,
        List<TenantProvisionTaskStepView> steps
) {
}
