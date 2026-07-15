package com.xuan.erp.audit.interfaces.controller;

import com.xuan.erp.audit.application.result.AuditWriteResult;
import com.xuan.erp.audit.application.service.AuditWriteApplicationService;
import com.xuan.erp.audit.interfaces.dto.AuditWriteRequest;
import com.xuan.erp.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/logs")
public class AuditWriteController {

    private final AuditWriteApplicationService service;

    public AuditWriteController(AuditWriteApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<AuditWriteResult> write(@RequestBody AuditWriteRequest request) {
        return ApiResponse.success(service.write(request.toCommand()));
    }
}
