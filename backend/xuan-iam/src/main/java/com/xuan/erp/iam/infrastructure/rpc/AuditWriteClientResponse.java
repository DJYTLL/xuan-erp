package com.xuan.erp.iam.infrastructure.rpc;

/**
 * 审计服务写入响应的轻量视图。
 */
public record AuditWriteClientResponse(
        Long id,
        String status
) {
}
