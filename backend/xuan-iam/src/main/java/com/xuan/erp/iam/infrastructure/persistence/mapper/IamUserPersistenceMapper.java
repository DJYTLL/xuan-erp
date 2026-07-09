package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamUserRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 用户持久化 Mapper，负责 iam_user 表的读写。
 */
@Mapper
public interface IamUserPersistenceMapper {

    /**
     * 按主键查询有效用户记录。
     */
    IamUserRecord findById(@Param("id") Long id);

    /**
     * 按主键查询用户记录，包含逻辑删除数据。
     */
    IamUserRecord findByIdIncludingDeleted(@Param("id") Long id);

    /**
     * 按租户和用户名查询有效用户记录。
     */
    IamUserRecord findActiveByTenantIdAndUsername(@Param("tenantId") Long tenantId, @Param("username") String username);

    /**
     * 查询指定租户下全部有效用户记录。
     */
    List<IamUserRecord> findActiveUsers(@Param("tenantId") Long tenantId);

    /**
     * 新增用户记录。
     */
    int insert(IamUserRecord record);

    /**
     * 更新用户记录。
     */
    int update(IamUserRecord record);
}
