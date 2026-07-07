package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantConfigApplicationService;
import com.xuan.erp.tenant.interfaces.assembler.TenantConfigAssembler;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantConfigRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户配置接口控制器，负责租户级系统配置的查询、创建、修改和软删除。
 */
@Tag(name = "租户配置", description = "租户级系统配置查询、创建、修改和软删除接口")
@RestController
@RequestMapping("/api/tenant-configs")
public class TenantConfigController {

    private final TenantConfigApplicationService service;

    public TenantConfigController(TenantConfigApplicationService service) {
        this.service = service;
    }

    /**
     * 查询所有未删除的租户配置列表。
     */
    @Operation(summary = "查询租户配置列表", description = "查询所有未逻辑删除的租户级系统配置")
    @GetMapping
    public ApiResponse<List<TenantConfigResponse>> listConfigs() {
        return ApiResponse.success(service.listConfigs().stream()
                .map(TenantConfigAssembler::toResponse)
                .toList());
    }

    /**
     * 根据配置 ID 查询租户配置详情。
     */
    @Operation(summary = "查询租户配置详情", description = "根据配置 ID 查询租户级系统配置详情")
    @GetMapping("/{configId}")
    public ApiResponse<TenantConfigResponse> getConfig(@PathVariable("configId") Long configId) {
        return ApiResponse.success(TenantConfigAssembler.toResponse(service.getConfig(configId)));
    }

    /**
     * 创建租户级系统配置。
     */
    @Operation(summary = "创建租户配置", description = "创建租户级系统配置")
    @PostMapping
    public ApiResponse<TenantConfigResponse> createConfig(@RequestBody TenantConfigRequest request) {
        return ApiResponse.success(TenantConfigAssembler.toResponse(service.createConfig(TenantConfigAssembler.toValues(request))));
    }

    /**
     * 修改租户级系统配置，例如配置值、值类型、公开标记和敏感标记。
     */
    @Operation(summary = "修改租户配置", description = "修改租户级系统配置值、值类型、公开标记和敏感标记")
    @PutMapping("/{configId}")
    public ApiResponse<TenantConfigResponse> updateConfig(@PathVariable("configId") Long configId, @RequestBody TenantConfigRequest request) {
        return ApiResponse.success(TenantConfigAssembler.toResponse(service.updateConfig(configId, TenantConfigAssembler.toValues(request))));
    }

    /**
     * 软删除租户配置，并要求记录删除原因。
     */
    @Operation(summary = "删除租户配置", description = "软删除租户配置，并要求记录删除原因")
    @DeleteMapping("/{configId}")
    public ApiResponse<Void> deleteConfig(@PathVariable("configId") Long configId, @RequestBody DeleteRequest request) {
        service.deleteConfig(configId, request.reason(), request.operator());
        return ApiResponse.success(null);
    }
}
