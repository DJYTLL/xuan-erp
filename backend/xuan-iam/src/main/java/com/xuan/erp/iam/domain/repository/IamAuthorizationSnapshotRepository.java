package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import java.util.Optional;

/**
 * IAM 授权快照仓储端口，隔离授权快照的读取和保存细节。
 */
public interface IamAuthorizationSnapshotRepository {

    Optional<IamAuthorizationSnapshot> findByTenantIdAndUserId(Long tenantId, Long userId);

    IamAuthorizationSnapshot save(IamAuthorizationSnapshot snapshot);
}
