package com.xuan.erp.tenant.infrastructure.rpc;

import java.util.List;

/**
 * 调用 IAM 初始化接口时使用的同步请求体。
 */
public record IamTenantBootstrapSyncRequest(
        String iamInitTemplateCode,
        List<String> columnPermissionTemplateCodes,
        String defaultColumnPermissionTemplateCode,
        String permissionHash
) {

    public IamTenantBootstrapSyncRequest(String iamInitTemplateCode) {
        this(iamInitTemplateCode, null, null, null);
    }
}
