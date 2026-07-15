package com.xuan.erp.iam.application.port;

import com.xuan.erp.iam.application.query.IamTenantStatusView;
import java.util.Optional;

/**
 * IAM 查询租户状态的端口，隔离 xuan-tenant 的远程调用细节。
 */
public interface IamTenantStatusGateway {

    Optional<IamTenantStatusView> findTenantStatus(Long tenantId);
}
