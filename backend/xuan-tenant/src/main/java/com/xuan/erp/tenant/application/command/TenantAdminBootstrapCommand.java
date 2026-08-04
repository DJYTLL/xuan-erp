package com.xuan.erp.tenant.application.command;

import java.util.List;

/**
 * 传递给 IAM 租户初始化步骤的管理员账号数据。
 *
 * @param adminUsername 管理员用户名
 * @param adminPasswordHash 管理员密码哈希，避免异步编排载荷保存明文密码
 * @param adminDisplayName 管理员显示名
 * @param adminEmail 管理员邮箱
 * @param adminPhone 管理员手机号
 * @param iamInitTemplateCode IAM 租户初始化权限模板编码
 * @param columnPermissionTemplateCodes 套餐绑定的平台列权限模板编码列表
 * @param defaultColumnPermissionTemplateCode 套餐绑定的默认列权限模板编码
 */
public record TenantAdminBootstrapCommand(
        String adminUsername,
        String adminPasswordHash,
        String adminDisplayName,
        String adminEmail,
        String adminPhone,
        String iamInitTemplateCode,
        List<String> columnPermissionTemplateCodes,
        String defaultColumnPermissionTemplateCode
) {

    public TenantAdminBootstrapCommand(
            String adminUsername,
            String adminPasswordHash,
            String adminDisplayName,
            String adminEmail,
            String adminPhone,
            String iamInitTemplateCode) {
        this(adminUsername, adminPasswordHash, adminDisplayName, adminEmail, adminPhone, iamInitTemplateCode, null, null);
    }
}
