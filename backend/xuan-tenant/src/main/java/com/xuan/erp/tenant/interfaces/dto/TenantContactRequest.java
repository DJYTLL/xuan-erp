package com.xuan.erp.tenant.interfaces.dto;

public record TenantContactRequest(
        Long tenantId,
        String contactType,
        String name,
        String phone,
        String email,
        Boolean primary,
        String remark
) {
}
