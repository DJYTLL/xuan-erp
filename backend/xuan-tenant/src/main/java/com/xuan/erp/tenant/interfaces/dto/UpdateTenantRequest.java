package com.xuan.erp.tenant.interfaces.dto;

public record UpdateTenantRequest(
        String name,
        String contactName,
        String contactPhone,
        String remark
) {
}
