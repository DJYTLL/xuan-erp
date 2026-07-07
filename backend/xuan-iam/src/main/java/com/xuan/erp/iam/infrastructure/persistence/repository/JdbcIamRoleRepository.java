package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
/**
 * IAM 角色 JDBC 仓储适配器，承接租户内角色的数据库访问。
 */
public class JdbcIamRoleRepository implements IamRoleRepository {

    @Override
    public Optional<IamRole> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public List<IamRole> findActiveRoles(Long tenantId) {
        return List.of();
    }
}
