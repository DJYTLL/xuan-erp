package com.xuan.erp.tenant.interfaces.assembler;

import com.xuan.erp.tenant.interfaces.dto.TenantContactRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantContactResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TenantContactAssembler {

    private TenantContactAssembler() {
    }

    public static Map<String, Object> toValues(TenantContactRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("tenantId", request.tenantId());
        values.put("contactType", request.contactType());
        values.put("name", request.name());
        values.put("phone", request.phone());
        values.put("email", request.email());
        values.put("isPrimary", request.primary());
        values.put("remark", request.remark());
        return values;
    }

    public static TenantContactResponse toResponse(Map<String, Object> values) {
        return new TenantContactResponse(values);
    }
}
