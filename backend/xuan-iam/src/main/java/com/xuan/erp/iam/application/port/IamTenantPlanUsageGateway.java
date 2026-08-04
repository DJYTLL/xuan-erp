package com.xuan.erp.iam.application.port;

import java.util.List;

/**
 * IAM 查询 xuan-tenant 中套餐模板使用关系的网关端口。
 */
public interface IamTenantPlanUsageGateway {

    /**
     * 查询当前使用指定 IAM 初始化模板编码套餐的租户 ID。
     */
    List<Long> findActiveTenantIdsByIamInitTemplateCode(String iamInitTemplateCode);
}
