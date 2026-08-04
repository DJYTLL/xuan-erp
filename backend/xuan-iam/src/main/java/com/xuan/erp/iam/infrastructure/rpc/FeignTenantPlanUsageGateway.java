package com.xuan.erp.iam.infrastructure.rpc;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.port.IamTenantPlanUsageGateway;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 基于 Feign 的租户套餐模板使用关系查询适配器。
 */
@Component
public class FeignTenantPlanUsageGateway implements IamTenantPlanUsageGateway {

    private final TenantPlanUsageClient client;

    public FeignTenantPlanUsageGateway(TenantPlanUsageClient client) {
        this.client = client;
    }

    @Override
    public List<Long> findActiveTenantIdsByIamInitTemplateCode(String iamInitTemplateCode) {
        ApiResponse<List<Long>> response = client.findTenantIdsByIamInitTemplateCode(iamInitTemplateCode);
        if (response == null) {
            throw new BusinessException("IAM_TENANT_PLAN_USAGE_QUERY_FAILED", "租户套餐使用关系接口未返回响应");
        }
        if (!"SUCCESS".equals(response.code())) {
            throw new BusinessException(
                    "IAM_TENANT_PLAN_USAGE_QUERY_FAILED",
                    response.message() == null || response.message().isBlank()
                            ? "租户套餐使用关系接口返回失败: " + response.code()
                            : response.message());
        }
        return response.data() == null ? List.of() : response.data();
    }
}
