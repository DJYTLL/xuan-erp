package com.xuan.erp.common.security.audit;

/**
 * 安全审计失败类型。
 *
 * <p>这里统一定义网关和后续业务服务都能复用的安全失败编码，避免不同入口对同一种
 * 认证、授权或租户上下文异常写出不一致的 action 和 errorCode。</p>
 */
public enum SecurityAuditFailure {

    TOKEN_INVALID("security:token:invalid", "SECURITY_TOKEN_INVALID", 401),
    AUTHENTICATION_MISSING("security:authentication:missing", "SECURITY_AUTHENTICATION_MISSING", 401),
    PERMISSION_DENIED("security:permission:denied", "SECURITY_PERMISSION_DENIED", 403),
    TENANT_CONTEXT_INVALID("security:tenant-context:invalid", "SECURITY_TENANT_CONTEXT_INVALID", 403),
    JWKS_UNAVAILABLE("security:jwks:unavailable", "SECURITY_JWKS_UNAVAILABLE", 503);

    private final String action;
    private final String errorCode;
    private final int httpStatus;

    SecurityAuditFailure(String action, String errorCode, int httpStatus) {
        this.action = action;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String action() {
        return action;
    }

    public String errorCode() {
        return errorCode;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
