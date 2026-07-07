package com.xuan.erp.tenant.application.command;

public record UpdateTenantCommand(
        String name,
        String contactName,
        String contactPhone,
        String remark
) {
}
