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
     * 按租户和角色编码查询有效角色记录。
     */
    IamRoleRecord findActiveByTenantIdAndCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    /**
     * 查询指定租户下全部有效角色记录。
     */
    List<IamRoleRecord> findActiveRoles(@Param("tenantId") Long tenantId);

    /**
     * 新增角色记录。
     */
    int insert(@Param("role") IamRoleRecord role);

    /**
     * 更新角色记录。
     */
    int update(@Param("role") IamRoleRecord role);
}
