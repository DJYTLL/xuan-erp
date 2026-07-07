package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.TenantContactType;
import java.time.OffsetDateTime;

public record TenantContact(
        Long id,
        Long tenantId,
        TenantContactType contactType,
        String name,
        String phone,
        String email,
        boolean primaryContact,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {
}
