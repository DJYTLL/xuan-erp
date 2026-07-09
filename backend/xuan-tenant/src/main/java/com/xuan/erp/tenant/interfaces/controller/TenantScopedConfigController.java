package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.api.PageResult;
import com.xuan.erp.tenant.application.command.UpdateTenantConfigCommand;
import com.xuan.erp.tenant.application.query.TenantConfigDetailView;
import com.xuan.erp.tenant.application.service.TenantConfigApplicationService;
import com.xuan.erp.tenant.interfaces.assembler.TenantConfigAssembler;
import com.xuan.erp.tenant.interfaces.dto.TenantConfigResponse;
import com.xuan.erp.tenant.interfaces.dto.UpdateTenantScopedConfigRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "租户配置子资源", description = "租户详情页下的配置分页查询与按键更新接口")
@RestController
@RequestMapping("/api/tenants/{tenantId}/configs")
public class TenantScopedConfigController {

    private final TenantConfigApplicationService service;

    public TenantScopedConfigController(TenantConfigApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "查询租户配置列表", description = "按租户 ID 分页查询租户配置")
    @PreAuthorize("hasAuthority('tenant-config:view')")
    @GetMapping
    public ApiResponse<PageResult<TenantConfigResponse>> listConfigs(
            @PathVariable("tenantId") @Min(value = 1, message = "租户 ID 必须大于 0") Long tenantId,
            @RequestParam(value = "pageNum", defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") @Min(value = 1, message = "分页大小必须大于 0") long pageSize) {
        PageResult<TenantConfigDetailView> page = service.listConfigs(tenantId, pageNum, pageSize);
        return ApiResponse.success(new PageResult<>(
                page.records().stream().map(TenantConfigAssembler::toResponse).toList(),
                page.total(),
                page.pageNum(),
                page.pageSize()
        ));
    }

    @Operation(summary = "修改租户配置", description = "按租户 ID 和配置键修改租户配置")
    @PreAuthorize("hasAuthority('tenant-config:manage')")
    @PutMapping("/{configKey}")
    public ApiResponse<TenantConfigResponse> updateConfig(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("configKey") String configKey,
            @Valid @RequestBody UpdateTenantScopedConfigRequest request) {
        return ApiResponse.success(TenantConfigAssembler.toResponse(service.updateConfig(
                tenantId,
                configKey,
                new UpdateTenantConfigCommand(
                        request.configValue(),
                        request.valueType(),
                        request.description(),
                        Boolean.TRUE.equals(request.publicConfig()),
                        Boolean.TRUE.equals(request.sensitive()),
                        Boolean.TRUE.equals(request.encrypted())
                ))));
    }
}
