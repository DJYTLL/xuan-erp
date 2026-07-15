package com.xuan.erp.iam.infrastructure.rpc;

import com.xuan.erp.common.api.ApiResponse;
import com.xuan.erp.iam.application.port.IamTenantStatusGateway;
import com.xuan.erp.iam.application.query.IamTenantStatusView;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 基于 Feign 的租户状态查询适配器。
 */
@Component
public class FeignTenantStatusGateway implements IamTenantStatusGateway {

    private final TenantStatusClient client;

    public FeignTenantStatusGateway(TenantStatusClient client) {
        this.client = client;
    }

    @Override
    public Optional<IamTenantStatusView> findTenantStatus(Long tenantId) {
        ApiResponse<TenantStatusClientResponse> response = client.getTenantStatus(tenantId);
        TenantStatusClientResponse data = response == null ? null : response.data();
        return Optional.ofNullable(data).map(this::toView);
    }

    private IamTenantStatusView toView(TenantStatusClientResponse response) {
        return new IamTenantStatusView(
                response.tenantId(),
                response.code(),
                response.status(),
                response.loginAllowed(),
                response.loginDeniedReason(),
                response.currentPlanExpiresAt());
    }
}
