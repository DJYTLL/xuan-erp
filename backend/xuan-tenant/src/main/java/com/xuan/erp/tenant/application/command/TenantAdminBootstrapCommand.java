package com.xuan.erp.tenant.application.command;

/**
 * 传递给 IAM 租户初始化步骤的管理员账号数据。
 *
 * @param adminUsername 管理员用户名
 * @param adminPasswordHash 管理员密码哈希，避免异步编排载荷保存明文密码
 * @param adminDisplayName 管理员显示名
 * @param adminEmail 管理员邮箱
 * @param adminPhone 管理员手机号
 */
public record TenantAdminBootstrapCommand(
        String adminUsername,
        String adminPasswordHash,
        String adminDisplayName,
        String adminEmail,
        String adminPhone
) {
}
