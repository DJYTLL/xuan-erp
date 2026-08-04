package com.xuan.erp.tenant.domain.model;

import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.time.OffsetDateTime;
import java.util.Locale;

public record Tenant(
        Long id,
        String code,
        String normalizedCode,
        String name,
        TenantStatus status,
        String contactName,
        String contactPhone,
        OffsetDateTime provisionedAt,
        OffsetDateTime enabledAt,
        OffsetDateTime disabledAt,
        String disabledReason,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt,
        String deletedBy,
        String deleteReason,
        OffsetDateTime deletedAt
) {

    public static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("tenant code must not be blank");
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    public boolean active() {
        return deletedAt == null;
    }

    public TenantLoginEligibility loginEligibility(OffsetDateTime currentPlanExpiresAt, OffsetDateTime now) {
        if (!active()) {
            return TenantLoginEligibility.denied("租户已删除");
        }
        if (status != TenantStatus.PROVISIONED && status != TenantStatus.ENABLED) {
            return TenantLoginEligibility.denied("租户状态不允许登录: " + status.code());
        }
        if (currentPlanExpiresAt != null && !currentPlanExpiresAt.isAfter(now)) {
            return TenantLoginEligibility.denied("租户套餐已到期");
        }
        return TenantLoginEligibility.allow();
    }

    public Tenant updateProfile(String newName, String newContactName, String newContactPhone, String newRemark, String operator, OffsetDateTime now) {
        ensureActive();
        return new Tenant(
                id,
                code,
                normalizedCode,
                requiredText(newName, "tenant name must not be blank"),
                status,
                newContactName,
                newContactPhone,
                provisionedAt,
                enabledAt,
                disabledAt,
                disabledReason,
                newRemark,
                createdBy,
                createdAt,
                operator,
                now,
                deletedBy,
                deleteReason,
                deletedAt
        );
    }

    public Tenant markProvisioned(String reason, String operator, OffsetDateTime now) {
        ensureActive();
        if (status != TenantStatus.PROVISIONING) {
            throw new IllegalStateException("tenant is not provisioning");
        }
        return new Tenant(
                id,
                code,
                normalizedCode,
                name,
                TenantStatus.PROVISIONED,
                contactName,
                contactPhone,
                now,
                enabledAt,
                disabledAt,
                disabledReason,
                remark,
                createdBy,
                createdAt,
                operator,
                now,
                deletedBy,
                deleteReason,
                deletedAt
        );
    }

    public Tenant enable(String reason, String operator, OffsetDateTime now) {
        ensureActive();
        if (status == TenantStatus.ENABLED) {
            throw new IllegalStateException("tenant already enabled");
        }
        if (status != TenantStatus.PROVISIONED && provisionedAt == null) {
            throw new IllegalStateException("tenant must be provisioned before enabling");
        }
        return new Tenant(
                id,
                code,
                normalizedCode,
                name,
                TenantStatus.ENABLED,
                contactName,
                contactPhone,
                provisionedAt,
                now,
                null,
                null,
                remark,
                createdBy,
                createdAt,
                operator,
                now,
                deletedBy,
                deleteReason,
                deletedAt
        );
    }

    public Tenant disable(String reason, String operator, OffsetDateTime now) {
        ensureActive();
        if (status == TenantStatus.DISABLED) {
            throw new IllegalStateException("tenant already disabled");
        }
        if (status != TenantStatus.ENABLED) {
            throw new IllegalStateException("tenant must be enabled before disabling");
        }
        return new Tenant(
                id,
                code,
                normalizedCode,
                name,
                TenantStatus.DISABLED,
                contactName,
                contactPhone,
                provisionedAt,
                enabledAt,
                now,
                requiredText(reason, "disable reason must not be blank"),
                remark,
                createdBy,
                createdAt,
                operator,
                now,
                deletedBy,
                deleteReason,
                deletedAt
        );
    }

    public Tenant markDeleted(String reason, String operator, OffsetDateTime now) {
        ensureActive();
        if (status == TenantStatus.ENABLED) {
            throw new IllegalStateException("enabled tenant cannot be deleted");
        }
        return new Tenant(
                id,
                code,
                normalizedCode,
                name,
                status,
                contactName,
                contactPhone,
                provisionedAt,
                enabledAt,
                disabledAt,
                disabledReason,
                remark,
                createdBy,
                createdAt,
                operator,
                now,
                operator,
                requiredText(reason, "delete reason must not be blank"),
                now
        );
    }

    private void ensureActive() {
        if (!active()) {
            throw new IllegalStateException("tenant already deleted");
        }
    }

    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
