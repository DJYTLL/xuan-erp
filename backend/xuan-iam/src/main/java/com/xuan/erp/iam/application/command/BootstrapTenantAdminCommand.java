package com.xuan.erp.iam.application.command;

/**
 * 租户 IAM 初始化时要创建的管理员账号信息。
 *
 * @param username 管理员用户名，未传入时使用 admin
 * @param password 管理员密码或密码哈希
 * @param displayName 管理员显示名，未传入时使用租户管理员
 * @param email 管理员邮箱
 * @param phone 管理员手机号
 * @param passwordAlreadyEncoded true 表示 password 已经是可直接入库的密码哈希
 */
public record BootstrapTenantAdminCommand(
        String username,
        String password,
        String displayName,
        String email,
        String phone,
        boolean passwordAlreadyEncoded
) {
}
