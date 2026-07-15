package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamPermission;
import java.util.List;
import java.util.Optional;

/**
 * IAM 权限仓储端口，隔离全局权限定义的读取细节。
 */
public interface IamPermissionRepository {

    /**
     * 按权限主键查询有效权限。
     */
    default Optional<IamPermission> findById(Long id) {
        return Optional.empty();
    }

    Optional<IamPermission> findByCode(String code);

    List<IamPermission> findActivePermissions();

    /**
     * 保存权限定义，新增和修改都通过领域对象表达最终状态。
     */
    default IamPermission save(IamPermission permission) {
        throw new UnsupportedOperationException("当前权限仓储不支持写入");
    }
}
