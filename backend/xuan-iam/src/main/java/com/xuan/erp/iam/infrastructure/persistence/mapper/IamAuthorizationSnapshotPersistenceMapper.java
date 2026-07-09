package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamAuthorizationSnapshotRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 授权快照持久化 Mapper，负责 iam_authorization_snapshot 表的读写。
 */
@Mapper
public interface IamAuthorizationSnapshotPersistenceMapper {

    /**
     * 按租户和用户查询授权快照记录。
     */
    IamAuthorizationSnapshotRecord findByTenantIdAndUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /**
     * 新增授权快照记录。
     */
    int insert(IamAuthorizationSnapshotRecord record);

    /**
     * 更新授权快照记录。
     */
    int update(IamAuthorizationSnapshotRecord record);
}
