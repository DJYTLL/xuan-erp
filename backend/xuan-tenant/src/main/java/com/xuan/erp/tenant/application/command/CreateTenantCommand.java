package com.xuan.erp.tenant.application.command;

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
        String adminPhone
) {

    public CreateTenantCommand(String code, String name, String contactName, String contactPhone, String remark, String idempotencyKey) {
        this(code, name, contactName, contactPhone, remark, idempotencyKey, null, null, null, null, null);
    }

    public CreateTenantCommand(String code, String name, String contactName, String contactPhone, String remark) {
        this(code, name, contactName, contactPhone, remark, null, null, null, null, null, null);
    }
}
