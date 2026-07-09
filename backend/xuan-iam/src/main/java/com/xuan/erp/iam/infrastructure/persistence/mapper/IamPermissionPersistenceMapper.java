package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamPermissionRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 权限持久化 Mapper，负责 iam_permission 表的读取。
 */
@Mapper
public interface IamPermissionPersistenceMapper {

    /**
     * 按权限编码查询有效权限记录。
     */
    IamPermissionRecord findByCode(@Param("code") String code);

    /**
     * 查询全部有效权限记录。
     */
    List<IamPermissionRecord> findActivePermissions();
}
