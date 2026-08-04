package com.xuan.erp.tenant.infrastructure.rpc;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.domain.repository.TenantIamBootstrapGateway;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 基于 Feign 的 IAM 初始化模板同步网关。
 */
@Component
public class FeignTenantIamBootstrapGateway implements TenantIamBootstrapGateway {

    private final IamTenantBootstrapClient client;

    public FeignTenantIamBootstrapGateway(IamTenantBootstrapClient client) {
        this.client = client;
    }

    @Override
    public void bootstrapTenant(Long tenantId, String iamInitTemplateCode, String operator) {
        bootstrapTenant(tenantId, iamInitTemplateCode, null, null, operator);
    }

    @Override
    public void bootstrapTenant(
            Long tenantId,
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode,
            String operator) {
        bootstrapTenant(tenantId, iamInitTemplateCode, columnPermissionTemplateCodes, defaultColumnPermissionTemplateCode, null, operator);
    }

    @Override
    public void bootstrapTenant(
            Long tenantId,
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode,
            String permissionHash,
            String operator) {
        ApiResponse<Integer> response = client.bootstrapTenant(
                tenantId,
                new IamTenantBootstrapSyncRequest(
                        iamInitTemplateCode,
                        columnPermissionTemplateCodes,
                        defaultColumnPermissionTemplateCode,
                        permissionHash),
                operator == null || operator.isBlank() ? "xuan-tenant" : operator.trim());
        if (response == null) {
            throw new BusinessException("TENANT_IAM_TEMPLATE_SYNC_FAILED", "IAM 初始化接口未返回响应");
        }
        if (!"SUCCESS".equals(response.code())) {
            throw new BusinessException(
                    "TENANT_IAM_TEMPLATE_SYNC_FAILED",
                    response.message() == null || response.message().isBlank()
                            ? "IAM 初始化接口返回失败: " + response.code()
                            : response.message());
        }
    }

    @Override
    public Optional<String> findLastSyncedPermissionHash(Long tenantId) {
        try {
            ApiResponse<IamTenantPermissionSyncStateResponse> response = client.getPermissionSyncState(tenantId);
            IamTenantPermissionSyncStateResponse data = response == null ? null : response.data();
            return Optional.ofNullable(data == null ? null : data.lastSyncedPermissionHash())
                    .map(String::trim)
                    .filter(value -> !value.isBlank());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
