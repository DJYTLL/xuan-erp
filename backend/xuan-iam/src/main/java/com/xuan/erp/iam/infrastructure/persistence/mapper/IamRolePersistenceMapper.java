package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamRoleRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 角色持久化 Mapper，负责 iam_role 表的读取。
 */
@Mapper
public interface IamRolePersistenceMapper {

    /**
     * 按角色主键查询有效角色记录。
     */
    IamRoleRecord findById(@Param("id") Long id);

    /**
     * 查询指定租户下全部有效角色记录。
     */
    List<IamRoleRecord> findActiveRoles(@Param("tenantId") Long tenantId);
}
