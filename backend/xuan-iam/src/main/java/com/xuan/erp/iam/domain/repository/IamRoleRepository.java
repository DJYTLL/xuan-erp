package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamRole;
import java.util.List;
import java.util.Optional;

/**
 * IAM 角色仓储端口，隔离租户内角色的读取细节。
 */
public interface IamRoleRepository {

    Optional<IamRole> findById(Long id);

    List<IamRole> findActiveRoles(Long tenantId);
}
