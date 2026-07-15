package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamUserPreferenceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 用户通用偏好持久化 Mapper，负责 iam_user_preference 表的读取和写入。
 */
@Mapper
public interface IamUserPreferencePersistenceMapper {

    /**
     * 按租户、用户和偏好键查询用户偏好。
     */
    IamUserPreferenceRecord findByTenantIdAndUserIdAndPreferenceKey(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("preferenceKey") String preferenceKey);

    /**
     * 新增或更新用户偏好。
     */
    int upsert(@Param("preference") IamUserPreferenceRecord preference);
}
