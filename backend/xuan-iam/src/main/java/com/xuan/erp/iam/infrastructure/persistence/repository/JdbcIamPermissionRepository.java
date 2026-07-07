package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
/**
 * IAM 权限 JDBC 仓储适配器，承接全局权限定义的数据库访问。
 */
public class JdbcIamPermissionRepository implements IamPermissionRepository {

    @Override
    public Optional<IamPermission> findByCode(String code) {
        return Optional.empty();
    }

    @Override
    public List<IamPermission> findActivePermissions() {
        return List.of();
    }
}
