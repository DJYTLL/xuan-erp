package com.xuan.erp.tenant.interfaces.controller;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.tenant.application.service.TenantPlanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户套餐内部使用关系查询接口，供 IAM 模板权限变更时定位受影响租户。
 */
@Tag(name = "租户套餐内部使用关系", description = "服务内部使用的套餐与 IAM 初始化模板绑定查询接口")
@RestController
@RequestMapping("/internal/tenant-plan-usages")
public class TenantInternalPlanUsageController {

    private final TenantPlanApplicationService tenantPlanApplicationService;

    public TenantInternalPlanUsageController(TenantPlanApplicationService tenantPlanApplicationService) {
        this.tenantPlanApplicationService = tenantPlanApplicationService;
    }

    @Operation(summary = "按 IAM 初始化模板查询租户", description = "供 IAM 修改初始化模板权限后查询使用该模板套餐的租户")
    @GetMapping("/iam-init-templates/{iamInitTemplateCode}/tenant-ids")
    public ApiResponse<List<Long>> findTenantIdsByIamInitTemplateCode(
            @PathVariable("iamInitTemplateCode") String iamInitTemplateCode) {
        return ApiResponse.success(tenantPlanApplicationService.findActiveTenantIdsByIamInitTemplateCode(iamInitTemplateCode));
    }
}
