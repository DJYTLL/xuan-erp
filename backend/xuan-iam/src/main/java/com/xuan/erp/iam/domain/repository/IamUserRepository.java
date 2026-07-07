package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamUser;
import java.util.List;
import java.util.Optional;

/**
 * IAM 用户仓储端口，隔离租户内账号的读取和保存细节。
 */
public interface IamUserRepository {

    Optional<IamUser> findById(Long id);

    Optional<IamUser> findActiveByTenantIdAndUsername(Long tenantId, String username);

    List<IamUser> findActiveUsers(Long tenantId);

    IamUser save(IamUser user);
}
