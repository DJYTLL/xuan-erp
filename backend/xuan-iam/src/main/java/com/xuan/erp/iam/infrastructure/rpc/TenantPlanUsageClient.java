package com.xuan.erp.iam.infrastructure.rpc;

import com.xuan.erp.common.api.ApiResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * IAM 调用 xuan-tenant 查询套餐与 IAM 初始化模板使用关系的 Feign 客户端。
 */
@FeignClient(name = "xuan-tenant", contextId = "iamTenantPlanUsageClient")
public interface TenantPlanUsageClient {

    @GetMapping("/internal/tenant-plan-usages/iam-init-templates/{iamInitTemplateCode}/tenant-ids")
    ApiResponse<List<Long>> findTenantIdsByIamInitTemplateCode(
            @PathVariable("iamInitTemplateCode") String iamInitTemplateCode);
}
