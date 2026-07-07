package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamPermission;
import java.util.List;
import java.util.Optional;

/**
 * IAM 权限仓储端口，隔离全局权限定义的读取细节。
 */
public interface IamPermissionRepository {

    Optional<IamPermission> findByCode(String code);

    List<IamPermission> findActivePermissions();
}
