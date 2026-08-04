package com.xuan.erp.tenant;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.exception.BusinessException;
import org.springframework.security.access.AccessDeniedException;
import com.xuan.erp.tenant.interfaces.advice.TenantApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantApiExceptionHandlerTest {

    @Test
    void returnsBusinessExceptionMessageInApiEnvelope() {
        TenantApiExceptionHandler handler = new TenantApiExceptionHandler();

        ApiResponse<Void> response = handler.handleBusinessException(
                new BusinessException("TENANT_INVALID_ARGUMENT", "租户名称不能为空"));

        assertEquals("TENANT_INVALID_ARGUMENT", response.code());
        assertEquals("租户名称不能为空", response.message());
    }

    @Test
    void returnsDataIntegrityRootCauseMessageInApiEnvelope() {
        TenantApiExceptionHandler handler = new TenantApiExceptionHandler();

        ApiResponse<Void> response = handler.handleDataIntegrityViolation(new DataIntegrityViolationException(
                "外层 JDBC 异常",
                new IllegalStateException("effective_at 不能为空")));

        assertEquals("TENANT_DATA_INTEGRITY_VIOLATION", response.code());
        assertEquals("effective_at 不能为空", response.message());
    }

    @Test
    void returnsForbiddenApiEnvelopeForAccessDenied() {
        TenantApiExceptionHandler handler = new TenantApiExceptionHandler();

        ApiResponse<Void> response = handler.handleAccessDenied(new AccessDeniedException("tenant:view denied"));

        assertEquals("SECURITY_PERMISSION_DENIED", response.code());
        assertEquals("没有访问权限", response.message());
    }
}
