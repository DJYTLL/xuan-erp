package com.xuan.erp.iam.interfaces.advice;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts IAM business exceptions to the API envelope before the request falls through to /error.
 */
@RestControllerAdvice(basePackages = "com.xuan.erp.iam.interfaces.controller")
public class IamApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException error) {
        return ApiResponse.failure(error.code(), error.getMessage());
    }
}
