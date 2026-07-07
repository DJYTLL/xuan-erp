package com.xuan.erp.tenant.interfaces.assembler;

import com.xuan.erp.tenant.domain.model.TenantDomain;
import com.xuan.erp.tenant.interfaces.dto.TenantDomainRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantDomainResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TenantDomainAssembler {

    private TenantDomainAssembler() {
    }

    public static Map<String, Object> toValues(TenantDomainRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("tenantId", request.tenantId());
        values.put("domain", request.domain());
        values.put("normalizedDomain", request.normalizedDomain() == null && request.domain() != null
                ? TenantDomain.normalizeDomain(request.domain())
                : request.normalizedDomain());
        values.put("status", request.status());
        values.put("isPrimary", request.primary());
        values.put("verificationToken", request.verificationToken());
        values.put("verifiedAt", request.verifiedAt());
        values.put("lastCheckedAt", request.lastCheckedAt());
        values.put("remark", request.remark());
        return values;
    }

    public static TenantDomainResponse toResponse(Map<String, Object> values) {
        return new TenantDomainResponse(values);
    }
}
