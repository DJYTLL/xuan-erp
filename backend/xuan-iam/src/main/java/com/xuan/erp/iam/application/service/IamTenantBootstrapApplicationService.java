package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.port.IamTenantBootstrapGateway;
import org.springframework.stereotype.Service;

/**
 * IAM 租户初始化应用服务。
 *
 * 负责租户开通后的 IAM 初始化用例编排：参数校验、操作者默认值处理，
 * 以及把真正的初始化动作委托给下层网关。
 */
@Service
public class IamTenantBootstrapApplicationService {

    private final IamTenantBootstrapGateway bootstrapGateway;

    public IamTenantBootstrapApplicationService(IamTenantBootstrapGateway bootstrapGateway) {
        this.bootstrapGateway = bootstrapGateway;
    }

    /**
     * 初始化指定租户的 IAM 基础授权。
     *
     * 首次调用会写入租户菜单授权、初始化任务和 outbox 事件；
     * 重复调用由下层数据库函数按幂等键处理，通常返回 0。
     */
    public Integer bootstrapTenant(Long tenantId, String requestedBy) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", "租户 ID 不能为空");
        }

        return bootstrapGateway.bootstrapTenant(tenantId, operator(requestedBy));
    }

    /**
     * 规范化触发来源。
     *
     * 未传入时默认认为由租户开通流程触发。
     */
    private String operator(String requestedBy) {
        return requestedBy == null || requestedBy.isBlank() ? "tenant-provision" : requestedBy.trim();
    }
}
