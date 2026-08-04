package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantContactApplicationService;
import com.xuan.erp.tenant.interfaces.assembler.TenantContactAssembler;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantContactRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantContactResponse;
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
 * 租户联系人接口控制器，负责租户管理员、商务、技术、财务等联系人信息的维护。
 */
@Tag(name = "租户联系人", description = "租户管理员、商务、技术、财务等联系人信息维护接口")
@RestController
@RequestMapping("/api/tenant-contacts")
public class TenantContactController {

    private final TenantContactApplicationService service;

    public TenantContactController(TenantContactApplicationService service) {
        this.service = service;
    }

    /**
     * 查询所有未删除的租户联系人列表。
     */
    @Operation(summary = "查询租户联系人列表", description = "查询所有未逻辑删除的租户联系人")
    @PreAuthorize("@xuanPermission.has('tenant:view')")
    @GetMapping
    public ApiResponse<List<TenantContactResponse>> listContacts() {
        return ApiResponse.success(service.listContacts().stream()
                .map(TenantContactAssembler::toResponse)
                .toList());
    }

    /**
     * 根据联系人 ID 查询租户联系人详情。
     */
    @Operation(summary = "查询租户联系人详情", description = "根据联系人 ID 查询租户联系人详情")
    @PreAuthorize("@xuanPermission.has('tenant:view')")
    @GetMapping("/{contactId}")
    public ApiResponse<TenantContactResponse> getContact(@PathVariable("contactId") Long contactId) {
        return ApiResponse.success(TenantContactAssembler.toResponse(service.getContact(contactId)));
    }

    /**
     * 创建租户联系人记录。
     */
    @Operation(summary = "创建租户联系人", description = "创建租户管理员、商务、技术或财务联系人记录")
    @PreAuthorize("@xuanPermission.has('tenant:update')")
    @PostMapping
    public ApiResponse<TenantContactResponse> createContact(@RequestBody TenantContactRequest request) {
        return ApiResponse.success(TenantContactAssembler.toResponse(service.createContact(TenantContactAssembler.toValues(request))));
    }

    /**
     * 修改租户联系人记录，例如联系人类型、姓名、电话、邮箱和主联系人标记。
     */
    @Operation(summary = "修改租户联系人", description = "修改租户联系人类型、姓名、电话、邮箱和主联系人标记")
    @PreAuthorize("@xuanPermission.has('tenant:update')")
    @PutMapping("/{contactId}")
    public ApiResponse<TenantContactResponse> updateContact(@PathVariable("contactId") Long contactId, @RequestBody TenantContactRequest request) {
        return ApiResponse.success(TenantContactAssembler.toResponse(service.updateContact(contactId, TenantContactAssembler.toValues(request))));
    }

    /**
     * 软删除租户联系人记录，并要求记录删除原因。
     */
    @Operation(summary = "删除租户联系人", description = "软删除租户联系人记录，并要求记录删除原因")
    @PreAuthorize("@xuanPermission.has('tenant:update')")
    @DeleteMapping("/{contactId}")
    public ApiResponse<Void> deleteContact(@PathVariable("contactId") Long contactId, @RequestBody DeleteRequest request) {
        service.deleteContact(contactId, request.reason(), request.operator());
        return ApiResponse.success(null);
    }
}
