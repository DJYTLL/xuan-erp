package com.xuan.erp.common.security.audit;

import com.xuan.erp.common.audit.AuditWriteEvent;
import com.xuan.erp.common.audit.AuditWriteOutcome;
import com.xuan.erp.common.security.CurrentUser;

/**
 * 安全审计事件工厂。
 *
 * <p>公共安全模块只负责把安全异常标准化为审计事件，不负责选择写入方式。
 * 网关、Servlet 服务或异步消费者可以按自己的运行模型把事件提交给 xuan-audit。</p>
 */
public class SecurityAuditEventFactory {

    private static final String ENTITY_TYPE = "GatewaySecurity";
    private static final long DEFAULT_SYSTEM_TENANT_ID = 0L;

    private final Long systemTenantId;

    public SecurityAuditEventFactory() {
        this(DEFAULT_SYSTEM_TENANT_ID);
    }

    public SecurityAuditEventFactory(Long systemTenantId) {
        this.systemTenantId = systemTenantId == null ? DEFAULT_SYSTEM_TENANT_ID : systemTenantId;
    }

    public AuditWriteEvent failure(SecurityAuditFailure failure, SecurityAuditRequest request) {
        CurrentUser currentUser = request.currentUser();
        Long authTenantId = currentUser == null ? null : currentUser.tenantId();
        Long requestedTenantId = request.requestedTenantId();
        boolean crossTenant = requestedTenantId != null && authTenantId != null && !requestedTenantId.equals(authTenantId);
        Long tenantId = requestedTenantId != null ? requestedTenantId : authTenantId;
        if (tenantId == null) {
            tenantId = systemTenantId;
        }

        return new AuditWriteEvent(
                tenantId,
                currentUser == null ? null : currentUser.username(),
                currentUser == null ? null : currentUser.userId(),
                failure.action(),
                ENTITY_TYPE,
                request.path(),
                detail(failure),
                AuditWriteOutcome.FAILED,
                blankToNull(request.requestId()),
                blankToNull(request.clientIp()),
                blankToNull(request.userAgent()),
                null,
                blankToNull(request.method()),
                blankToNull(request.path()),
                failure.httpStatus(),
                failure.errorCode(),
                blankToNull(request.errorMessage()),
                authTenantId,
                null,
                crossTenant
        );
    }

    private String detail(SecurityAuditFailure failure) {
        return "{\"failureType\":\"" + failure.name() + "\"}";
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
