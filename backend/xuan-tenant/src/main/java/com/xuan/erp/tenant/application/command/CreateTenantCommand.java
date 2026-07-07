package com.xuan.erp.tenant.application.command;

public record CreateTenantCommand(
        String code,
        String name,
        String contactName,
        String contactPhone,
        String remark
) {
}
