package com.xuan.erp.iam.application.port;

/**
 * IAM 租户初始化网关端口，隔离应用服务与具体数据库调用方式。
 */
public interface IamTenantBootstrapGateway {

    /**
     * 执行租户 IAM 基础数据初始化。
     *
     * @return 本次实际新增的租户菜单授权数量，重复调用通常返回 0
     */
    Integer bootstrapTenant(
            Long tenantId,
            String adminUsername,
            String adminPasswordHash,
            String adminDisplayName,
            String adminEmail,
            String adminPhone,
            String iamInitTemplateCode,
            String requestedBy);
}
