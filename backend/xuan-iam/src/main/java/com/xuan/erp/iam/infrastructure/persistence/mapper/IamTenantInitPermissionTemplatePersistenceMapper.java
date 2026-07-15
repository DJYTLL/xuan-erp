package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamTenantInitPermissionTemplateRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 租户初始化权限模板 MyBatis Mapper，负责模板表查询和写入。
 */
@Mapper
public interface IamTenantInitPermissionTemplatePersistenceMapper {

    IamTenantInitPermissionTemplateRecord findById(@Param("id") Long id);

    IamTenantInitPermissionTemplateRecord findActiveByCode(@Param("code") String code);

    List<IamTenantInitPermissionTemplateRecord> findActiveTemplates();

    int insert(@Param("template") IamTenantInitPermissionTemplateRecord template);

    int update(@Param("template") IamTenantInitPermissionTemplateRecord template);

    int clearDefaultTemplate(@Param("operator") String operator);
}
