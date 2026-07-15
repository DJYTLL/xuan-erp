package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamRole;
import java.util.List;
import java.util.Optional;

/**
 * IAM 角色仓储端口，隔离租户内角色的读取细节。
 */
public interface IamRoleRepository {

    Optional<IamRole> findById(Long id);

    /**
     * 按租户和角色编码查询有效角色。
     */
    default Optional<IamRole> findActiveByTenantIdAndCode(Long tenantId, String code) {
        return Optional.empty();
    }

    List<IamRole> findActiveRoles(Long tenantId);

    /**
     * 保存角色，新增和修改都通过领域对象表达最终状态。
     */
    default IamRole save(IamRole role) {
        throw new UnsupportedOperationException("当前角色仓储不支持写入");
    }
}
