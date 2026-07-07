package com.xuan.erp.tenant.interfaces.dto;

public record DeleteRequest(
        String reason,
        String operator
) {
}
