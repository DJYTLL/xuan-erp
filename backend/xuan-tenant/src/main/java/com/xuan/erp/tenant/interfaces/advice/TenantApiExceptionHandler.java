package com.xuan.erp.tenant.interfaces.advice;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将租户模块接口异常转换为统一 API 响应，避免前端只能看到通用 HTTP 文案。
 */
@RestControllerAdvice(basePackages = "com.xuan.erp.tenant.interfaces.controller")
public class TenantApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException error) {
        return ApiResponse.failure(error.code(), error.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDataIntegrityViolation(DataIntegrityViolationException error) {
        return ApiResponse.failure("TENANT_DATA_INTEGRITY_VIOLATION", mostSpecificMessage(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException error) {
        return ApiResponse.failure("SECURITY_PERMISSION_DENIED", "没有访问权限");
    }

    private String mostSpecificMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? "租户数据完整性校验失败" : message;
    }
}
