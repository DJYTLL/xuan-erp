package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.api.PageResult;
import com.xuan.erp.tenant.application.service.TenantConfigApplicationService;
import com.xuan.erp.tenant.interfaces.assembler.TenantConfigAssembler;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantConfigRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户配置接口控制器，负责租户级系统配置的分页查询、公开配置查询、创建、修改和软删除。
 */
@Validated
@Tag(name = "租户配置", description = "租户级系统配置分页查询、公开配置查询、创建、修改和软删除接口")
@RestController
@RequestMapping("/api/tenant-configs")
public class TenantConfigController {

    private final TenantConfigApplicationService service;

    public TenantConfigController(TenantConfigApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "分页查询租户配置", description = "按租户 ID 分页查询未逻辑删除的租户级系统配置")
    @PreAuthorize("hasAuthority('tenant-config:view')")
    @GetMapping
    public ApiResponse<PageResult<TenantConfigResponse>> listConfigs(
            @RequestParam("tenantId") @Min(value = 1, message = "租户 ID 必须大于 0") Long tenantId,
            @RequestParam(value = "pageNum", defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") @Min(value = 1, message = "分页大小必须大于 0") long pageSize) {
        PageResult<com.xuan.erp.tenant.application.query.TenantConfigDetailView> result = service.listConfigs(tenantId, pageNum, pageSize);
        PageResult<TenantConfigResponse> page = new PageResult<>(
                result.records().stream()
                        .map(TenantConfigAssembler::toResponse)
                        .toList(),
                result.total(),
                result.pageNum(),
                result.pageSize()
        );
        return ApiResponse.success(page);
    }

    @Operation(summary = "查询公开租户配置", description = "按租户 ID 查询允许前端公开读取的租户配置")
    @PreAuthorize("hasAuthority('tenant-config:view')")
    @GetMapping("/public")
    public ApiResponse<List<TenantConfigResponse>> listPublicConfigs(
            @RequestParam("tenantId") @Min(value = 1, message = "租户 ID 必须大于 0") Long tenantId) {
        return ApiResponse.success(service.listPublicConfigs(tenantId).stream()
                .map(TenantConfigAssembler::toResponse)
                .toList());
    }

    @Operation(summary = "查询租户配置详情", description = "根据配置 ID 查询租户级系统配置详情")
    @PreAuthorize("hasAuthority('tenant-config:view')")
    @GetMapping("/{configId}")
    public ApiResponse<TenantConfigResponse> getConfig(@PathVariable("configId") Long configId) {
        return ApiResponse.success(TenantConfigAssembler.toResponse(service.getConfig(configId)));
    }

    @Operation(summary = "创建租户配置", description = "创建租户级系统配置")
    @PreAuthorize("hasAuthority('tenant-config:manage')")
    @PostMapping
    public ApiResponse<TenantConfigResponse> createConfig(@Valid @RequestBody TenantConfigRequest request) {
        return ApiResponse.success(TenantConfigAssembler.toResponse(service.createConfig(TenantConfigAssembler.toCreateCommand(request))));
    }

    @Operation(summary = "修改租户配置", description = "修改租户级系统配置值、值类型、公开标记和敏感标记")
    @PreAuthorize("hasAuthority('tenant-config:manage')")
    @PutMapping("/{configId}")
    public ApiResponse<TenantConfigResponse> updateConfig(@PathVariable("configId") Long configId, @Valid @RequestBody TenantConfigRequest request) {
        return ApiResponse.success(TenantConfigAssembler.toResponse(service.updateConfig(configId, TenantConfigAssembler.toUpdateCommand(request))));
    }

    @Operation(summary = "删除租户配置", description = "软删除租户配置，并要求记录删除原因")
    @PreAuthorize("hasAuthority('tenant-config:manage')")
    @DeleteMapping("/{configId}")
    public ApiResponse<Void> deleteConfig(@PathVariable("configId") Long configId, @Valid @RequestBody DeleteRequest request) {
        service.deleteConfig(configId, request.reason(), request.operator());
        return ApiResponse.success(null);
    }
}
