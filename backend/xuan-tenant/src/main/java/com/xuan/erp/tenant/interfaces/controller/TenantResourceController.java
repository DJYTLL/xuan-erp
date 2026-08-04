package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantResourceApplicationService;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
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
 * 租户资源通用接口控制器，负责按资源名称提供租户相关表的基础增删改查能力。
 */
@Tag(name = "租户资源", description = "按资源名称提供租户相关资源的通用查询、创建、修改和软删除接口")
@RestController
@RequestMapping("/api/tenant-resources/{resourceName}")
public class TenantResourceController {

    private final TenantResourceApplicationService service;

    public TenantResourceController(TenantResourceApplicationService service) {
        this.service = service;
    }

    /**
     * 查询指定租户资源的列表数据。
     */
    @Operation(summary = "查询租户资源列表", description = "根据资源名称查询对应租户资源的列表数据")
    @PreAuthorize("@tenantResourcePermissionGuard.canRead(#resourceName)")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable("resourceName") String resourceName) {
        return ApiResponse.success(service.list(resourceName));
    }

    /**
     * 根据资源名称和数据 ID 查询单条租户资源数据。
     */
    @Operation(summary = "查询租户资源详情", description = "根据资源名称和数据 ID 查询单条租户资源数据")
    @PreAuthorize("@tenantResourcePermissionGuard.canRead(#resourceName)")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable("resourceName") String resourceName, @PathVariable("id") Long id) {
        return ApiResponse.success(service.get(resourceName, id));
    }

    /**
     * 为指定租户资源创建一条新数据。
     */
    @Operation(summary = "创建租户资源", description = "根据资源名称为对应租户资源创建一条新数据")
    @PreAuthorize("@tenantResourcePermissionGuard.canCreate(#resourceName)")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@PathVariable("resourceName") String resourceName, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.create(resourceName, body));
    }

    /**
     * 根据资源名称和数据 ID 修改一条租户资源数据。
     */
    @Operation(summary = "修改租户资源", description = "根据资源名称和数据 ID 修改一条租户资源数据")
    @PreAuthorize("@tenantResourcePermissionGuard.canUpdate(#resourceName)")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("resourceName") String resourceName, @PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.update(resourceName, id, body));
    }

    /**
     * 根据资源名称和数据 ID 软删除一条租户资源数据，并记录删除原因。
     */
    @Operation(summary = "删除租户资源", description = "根据资源名称和数据 ID 软删除一条租户资源数据，并记录删除原因")
    @PreAuthorize("@tenantResourcePermissionGuard.canDelete(#resourceName)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("resourceName") String resourceName, @PathVariable("id") Long id, @RequestBody DeleteRequest request) {
        service.delete(resourceName, id, request.reason(), request.operator());
        return ApiResponse.success(null);
    }
}
