package com.xuan.erp.tenant.interfaces.assembler;

import com.xuan.erp.tenant.interfaces.dto.TenantConfigRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantConfigResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TenantConfigAssembler {

    private TenantConfigAssembler() {
    }

    public static Map<String, Object> toValues(TenantConfigRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("tenantId", request.tenantId());
        values.put("configKey", request.configKey());
        values.put("configValue", request.configValue());
        values.put("valueType", request.valueType());
        values.put("description", request.description());
        values.put("isPublic", request.publicConfig());
        values.put("isSensitive", request.sensitive());
        values.put("isEncrypted", request.encrypted());
        return values;
    }

    public static TenantConfigResponse toResponse(Map<String, Object> values) {
        return new TenantConfigResponse(values);
    }
}
