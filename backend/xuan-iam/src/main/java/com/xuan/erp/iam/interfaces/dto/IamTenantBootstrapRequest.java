package com.xuan.erp.iam.interfaces.dto;

import com.xuan.erp.iam.application.command.BootstrapTenantAdminCommand;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IAM 租户初始化请求体。
 */
public record IamTenantBootstrapRequest(
        @Schema(description = "租户管理员用户名，未传入时默认 admin", example = "admin")
        String adminUsername,
        @Schema(description = "租户管理员明文密码，未传入时默认 123456；IAM 会立即转为密码哈希入库", example = "123456")
        String adminPassword,
        @Schema(description = "租户管理员密码哈希。由异步租户开通流程传入时优先使用，避免明文密码落库到编排载荷")
        String adminPasswordHash,
        @Schema(description = "租户管理员显示名，未传入时默认租户管理员", example = "租户管理员")
        String adminDisplayName,
        @Schema(description = "租户管理员邮箱", example = "admin@example.com")
        String adminEmail,
        @Schema(description = "租户管理员手机号", example = "13800000000")
        String adminPhone
) {

    public BootstrapTenantAdminCommand toCommand() {
        if (adminPasswordHash != null && !adminPasswordHash.isBlank()) {
            return new BootstrapTenantAdminCommand(
                    adminUsername,
                    adminPasswordHash,
                    adminDisplayName,
                    adminEmail,
                    adminPhone,
                    true);
        }
        return new BootstrapTenantAdminCommand(
                adminUsername,
                adminPassword,
                adminDisplayName,
                adminEmail,
                adminPhone,
                false);
    }
}
