package com.xuan.erp.audit.interfaces.controller;

import com.xuan.erp.audit.application.query.AuditLogQuery;
import com.xuan.erp.audit.application.service.AuditLogQueryApplicationService;
import com.xuan.erp.audit.domain.model.AuditLog;
import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;
import com.xuan.erp.common.api.ApiResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/logs")
public class AuditLogQueryController {

    private final AuditLogQueryApplicationService service;

    public AuditLogQueryController(AuditLogQueryApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AuditLog>> search(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "actorUsername", required = false) String actorUsername,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success(service.search(new AuditLogQuery(
                tenantId,
                actorUsername,
                action,
                entityType,
                parseStatus(status),
                parseOffsetDateTime(startTime),
                parseOffsetDateTime(endTime),
                limit
        )));
    }

    private static AuditWriteStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return AuditWriteStatus.fromCode(status);
    }

    private static OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value.trim());
    }
}
