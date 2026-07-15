package com.xuan.erp.common.security.audit;

import com.xuan.erp.common.security.CurrentUser;

/**
 * 构造安全审计事件所需的请求上下文。
 *
 * @param currentUser 已认证用户，未登录或 token 异常时为空
 * @param requestedTenantId 请求显式声明的租户 ID，未声明时为空
 * @param requestId 请求 ID，可来自 X-Request-Id 或追踪头
 * @param clientIp 客户端 IP
 * @param userAgent 浏览器或客户端 User-Agent
 * @param method HTTP 方法
 * @param path HTTP 路径
 * @param errorMessage 安全异常摘要，不包含 token 明文
 */
public record SecurityAuditRequest(
        CurrentUser currentUser,
        Long requestedTenantId,
        String requestId,
        String clientIp,
        String userAgent,
        String method,
        String path,
        String errorMessage
) {
}
