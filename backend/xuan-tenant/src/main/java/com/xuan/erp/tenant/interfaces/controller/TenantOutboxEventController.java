package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantProvisionApplicationService;
import com.xuan.erp.tenant.interfaces.dto.RetryTenantOutboxEventRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "租户 Outbox 事件", description = "租户初始化 Outbox 事件人工回放接口")
@RestController
@RequestMapping("/api")
public class TenantOutboxEventController {

    private final TenantProvisionApplicationService service;

    public TenantOutboxEventController(TenantProvisionApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "重试 Outbox 事件", description = "按事件 ID 触发 Outbox 事件人工回放入口")
    @PreAuthorize("@xuanPermission.has('tenant-provision:manage')")
    @PostMapping("/tenant-outbox-events/{eventId}/retry")
    public ApiResponse<Void> retryEvent(
            @PathVariable("eventId") @Min(value = 1, message = "事件 ID 必须大于 0") Long eventId,
            @Valid @RequestBody RetryTenantOutboxEventRequest request) {
        service.retryOutboxEvent(eventId, request.operator(), request.reason());
        return ApiResponse.success(null);
    }
}
