package com.xuan.erp.tenant.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record CreateTenantRequest(
        @NotBlank(message = "租户编码不能为空")
        @Size(max = 64, message = "租户编码长度不能超过 64")
        @Schema(description = "租户编码", example = "default")
        String code,
        @NotBlank(message = "租户名称不能为空")
        @Size(max = 120, message = "租户名称长度不能超过 120")
        @Schema(description = "租户名称", example = "默认租户")
        String name,
        @Schema(description = "联系人姓名", example = "系统管理员")
        String contactName,
        @Schema(description = "联系人电话", example = "13800000000")
        String contactPhone,
        @Schema(description = "备注", example = "本地联调租户")
        String remark,
        @Schema(description = "幂等键，客户端重复提交时用于去重", example = "tenant-create-20260707-001")
        String idempotencyKey,
        @Size(max = 100, message = "管理员用户名长度不能超过 100")
        @Schema(description = "租户管理员用户名，未传入时默认 admin", example = "admin")
        String adminUsername,
        @Size(max = 100, message = "管理员密码长度不能超过 100")
        @Schema(description = "租户管理员初始密码，未传入时默认 123456", example = "123456")
        String adminPassword,
        @Size(max = 200, message = "管理员显示名长度不能超过 200")
        @Schema(description = "租户管理员显示名，未传入时默认租户管理员", example = "租户管理员")
        String adminDisplayName,
        @Size(max = 200, message = "管理员邮箱长度不能超过 200")
        @Schema(description = "租户管理员邮箱", example = "admin@example.com")
        String adminEmail,
        @Size(max = 50, message = "管理员手机号长度不能超过 50")
        @Schema(description = "租户管理员手机号", example = "13800000000")
        String adminPhone,
        @Schema(description = "创建租户时绑定的套餐 ID", example = "1")
        Long planId,
        @Schema(description = "初始套餐到期时间，不传表示暂不设置到期时间", example = "2026-08-15T23:59:59+08:00")
        OffsetDateTime planExpiresAt
) {

    public CreateTenantRequest(
            String code,
            String name,
            String contactName,
            String contactPhone,
            String remark,
            String idempotencyKey) {
        this(code, name, contactName, contactPhone, remark, idempotencyKey, null, null, null, null, null, null, null);
    }

    public CreateTenantRequest(
            String code,
            String name,
            String contactName,
            String contactPhone,
            String remark,
            String idempotencyKey,
            String adminUsername,
            String adminPassword,
            String adminDisplayName,
            String adminEmail,
            String adminPhone,
            Long planId) {
        this(code, name, contactName, contactPhone, remark, idempotencyKey,
                adminUsername, adminPassword, adminDisplayName, adminEmail, adminPhone, planId, null);
    }
}
