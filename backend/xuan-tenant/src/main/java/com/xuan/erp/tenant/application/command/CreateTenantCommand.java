package com.xuan.erp.tenant.application.command;

import java.time.OffsetDateTime;

public record CreateTenantCommand(
        String code,
        String name,
        String contactName,
        String contactPhone,
        String remark,
        String idempotencyKey,
        String adminUsername,
        String adminPassword,
        String adminDisplayName,
        String adminEmail,
        String adminPhone,
        Long planId,
        OffsetDateTime planExpiresAt
) {

    public CreateTenantCommand(
            String code,
            String name,
            String contactName,
            String contactPhone,
            String remark,
            String idempotencyKey,
            String adminUsername,
            String adminPassword,
            String adminDisplayName,
            String adminEmail,
            String adminPhone) {
        this(code, name, contactName, contactPhone, remark, idempotencyKey,
                adminUsername, adminPassword, adminDisplayName, adminEmail, adminPhone, null, null);
    }

    public CreateTenantCommand(
            String code,
            String name,
            String contactName,
            String contactPhone,
            String remark,
            String idempotencyKey,
            String adminUsername,
            String adminPassword,
            String adminDisplayName,
            String adminEmail,
            String adminPhone,
            Long planId) {
        this(code, name, contactName, contactPhone, remark, idempotencyKey,
                adminUsername, adminPassword, adminDisplayName, adminEmail, adminPhone, planId, null);
    }

    public CreateTenantCommand(String code, String name, String contactName, String contactPhone, String remark, String idempotencyKey) {
        this(code, name, contactName, contactPhone, remark, idempotencyKey, null, null, null, null, null, null, null);
    }

    public CreateTenantCommand(String code, String name, String contactName, String contactPhone, String remark) {
        this(code, name, contactName, contactPhone, remark, null, null, null, null, null, null, null, null);
    }
}
