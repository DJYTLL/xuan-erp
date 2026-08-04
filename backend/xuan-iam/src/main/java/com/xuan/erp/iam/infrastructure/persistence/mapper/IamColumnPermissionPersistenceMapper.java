package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamColumnPermissionRuleRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 列权限持久化 Mapper，负责读取角色自己的列权限规则。
 */
@Mapper
public interface IamColumnPermissionPersistenceMapper {

    List<IamColumnPermissionRuleRecord> findColumnPermissionRulesByRoleIds(
            @Param("tenantId") Long tenantId,
            @Param("roleIds") List<Long> roleIds);
}
