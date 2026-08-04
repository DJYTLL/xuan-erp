package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantDomainApplicationService;
import com.xuan.erp.tenant.interfaces.assembler.TenantDomainAssembler;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantDomainRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantDomainResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户域名接口控制器，负责租户自定义域名的查询、创建、修改和软删除。
 */
@Tag(name = "租户域名", description = "租户自定义域名查询、创建、修改和软删除接口")
@RestController
@RequestMapping("/api/tenant-domains")
public class TenantDomainController {

    private final TenantDomainApplicationService service;

    public TenantDomainController(TenantDomainApplicationService service) {
        this.service = service;
    }

    /**
     * 查询所有未删除的租户域名列表。
     */
    @Operation(summary = "查询租户域名列表", description = "查询所有未逻辑删除的租户域名")
    @PreAuthorize("@xuanPermission.has('tenant:view')")
    @GetMapping
    public ApiResponse<List<TenantDomainResponse>> listDomains() {
        return ApiResponse.success(service.listDomains().stream()
                .map(TenantDomainAssembler::toResponse)
                .toList());
    }

    /**
     * 根据域名记录 ID 查询租户域名详情。
     */
    @Operation(summary = "查询租户域名详情", description = "根据域名记录 ID 查询租户域名详情")
    @PreAuthorize("@xuanPermission.has('tenant:view')")
    @GetMapping("/{domainId}")
    public ApiResponse<TenantDomainResponse> getDomain(@PathVariable("domainId") Long domainId) {
        return ApiResponse.success(TenantDomainAssembler.toResponse(service.getDomain(domainId)));
    }

    /**
     * 创建租户域名记录，并自动补齐规范化域名。
     */
    @Operation(summary = "创建租户域名", description = "创建租户自定义域名记录，并自动补齐规范化域名")
    @PreAuthorize("@xuanPermission.has('tenant:update')")
    @PostMapping
    public ApiResponse<TenantDomainResponse> createDomain(@RequestBody TenantDomainRequest request) {
        return ApiResponse.success(TenantDomainAssembler.toResponse(service.createDomain(TenantDomainAssembler.toValues(request))));
    }

    /**
     * 修改租户域名记录，例如域名状态、主域名标记和验证信息。
     */
    @Operation(summary = "修改租户域名", description = "修改租户域名状态、主域名标记和验证信息")
    @PreAuthorize("@xuanPermission.has('tenant:update')")
    @PutMapping("/{domainId}")
    public ApiResponse<TenantDomainResponse> updateDomain(@PathVariable("domainId") Long domainId, @RequestBody TenantDomainRequest request) {
        return ApiResponse.success(TenantDomainAssembler.toResponse(service.updateDomain(domainId, TenantDomainAssembler.toValues(request))));
    }

    /**
     * 软删除租户域名记录，并要求记录删除原因。
     */
    @Operation(summary = "删除租户域名", description = "软删除租户域名记录，并要求记录删除原因")
    @PreAuthorize("@xuanPermission.has('tenant:update')")
    @DeleteMapping("/{domainId}")
    public ApiResponse<Void> deleteDomain(@PathVariable("domainId") Long domainId, @RequestBody DeleteRequest request) {
        service.deleteDomain(domainId, request.reason(), request.operator());
        return ApiResponse.success(null);
    }
}
