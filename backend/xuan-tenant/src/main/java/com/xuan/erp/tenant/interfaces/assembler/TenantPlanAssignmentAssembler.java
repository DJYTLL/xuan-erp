package com.xuan.erp.tenant.interfaces.assembler;

import com.xuan.erp.tenant.interfaces.dto.TenantPlanAssignmentRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantPlanAssignmentResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TenantPlanAssignmentAssembler {

    private TenantPlanAssignmentAssembler() {
    }

    public static Map<String, Object> toValues(TenantPlanAssignmentRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("tenantId", request.tenantId());
        values.put("previousPlanId", request.previousPlanId());
        values.put("planId", request.planId());
        values.put("status", request.status());
        values.put("effectiveAt", request.effectiveAt());
        values.put("expiresAt", request.expiresAt());
        values.put("assignedAt", request.assignedAt());
        values.put("assignedBy", request.assignedBy());
        values.put("changeReason", request.changeReason());
        values.put("source", request.source());
        values.put("remark", request.remark());
        return values;
    }

    public static TenantPlanAssignmentResponse toResponse(Map<String, Object> values) {
        return new TenantPlanAssignmentResponse(values);
    }
}
