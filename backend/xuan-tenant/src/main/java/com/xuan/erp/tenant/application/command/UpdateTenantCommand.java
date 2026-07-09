package com.xuan.erp.tenant.application.command;

public record UpdateTenantCommand(
        String name,
        String contactName,
        String contactPhone,
        String remark,
        String idempotencyKey
) {

    public UpdateTenantCommand(String name, String contactName, String contactPhone, String remark) {
        this(name, contactName, contactPhone, remark, null);
    }
}
