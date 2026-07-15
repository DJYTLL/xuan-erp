package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamUserPreference;
import java.util.Optional;

/**
 * IAM 用户通用偏好仓储，负责按当前用户维度读取和保存偏好配置。
 */
public interface IamUserPreferenceRepository {

    /**
     * 按租户、用户和偏好键查询偏好。
     */
    Optional<IamUserPreference> findByTenantIdAndUserIdAndPreferenceKey(Long tenantId, Long userId, String preferenceKey);

    /**
     * 保存偏好配置。
     */
    IamUserPreference save(IamUserPreference preference);
}
