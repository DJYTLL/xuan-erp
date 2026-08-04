package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplate;
import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplateItem;
import com.xuan.erp.iam.domain.model.IamResourceColumn;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionRule;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionTemplateBinding;
import com.xuan.erp.iam.domain.model.IamTenantColumnPermissionTemplateAssignment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM 列权限管理持久化 Mapper。
 */
@Mapper
public interface IamColumnPermissionManagementPersistenceMapper {

    List<IamResourceColumn> findEnabledResourceColumns();

    List<IamColumnPermissionTemplate> findTemplates(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("enabled") Boolean enabled);

    IamColumnPermissionTemplate findTemplateById(@Param("templateId") Long templateId);

    IamColumnPermissionTemplate findTemplateByTenantIdAndCode(
            @Param("tenantId") Long tenantId,
            @Param("code") String code);

    Long insertTemplate(
            @Param("tenantId") Long tenantId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("description") String description,
            @Param("enabled") boolean enabled,
            @Param("operator") String operator);

    void updateTemplate(
            @Param("templateId") Long templateId,
            @Param("name") String name,
            @Param("description") String description,
            @Param("enabled") boolean enabled,
            @Param("operator") String operator);

    void setTemplateEnabled(
            @Param("templateId") Long templateId,
            @Param("enabled") boolean enabled,
            @Param("operator") String operator);

    List<IamColumnPermissionTemplateItem> findTemplateItems(@Param("templateId") Long templateId);

    IamResourceColumn findResourceColumnById(@Param("resourceColumnId") Long resourceColumnId);

    void disableTemplateItems(
            @Param("templateId") Long templateId,
            @Param("operator") String operator);

    void insertTemplateItem(
            @Param("templateId") Long templateId,
            @Param("resourceColumnId") Long resourceColumnId,
            @Param("accessMode") String accessMode,
            @Param("operator") String operator);

    List<IamTenantColumnPermissionTemplateAssignment> findTenantTemplateAssignments(@Param("tenantId") Long tenantId);

    boolean existsTenantTemplateAssignment(
            @Param("tenantId") Long tenantId,
            @Param("templateId") Long templateId);

    void disableTenantTemplateAssignments(
            @Param("tenantId") Long tenantId,
            @Param("operator") String operator);

    void insertTenantTemplateAssignment(
            @Param("tenantId") Long tenantId,
            @Param("templateId") Long templateId,
            @Param("defaultTemplate") boolean defaultTemplate,
            @Param("operator") String operator);

    List<IamColumnPermissionTemplateItem> findTenantAssignableColumnRules(@Param("tenantId") Long tenantId);

    List<IamRoleColumnPermissionRule> findRoleColumnPermissionRules(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId);

    void disableRoleColumnPermissionRules(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId,
            @Param("operator") String operator);

    void insertRoleColumnPermissionRule(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId,
            @Param("resourceColumnId") Long resourceColumnId,
            @Param("accessMode") String accessMode,
            @Param("operator") String operator);

    void disableRoleColumnPermissionRulesOutsideTenantAssignments(
            @Param("tenantId") Long tenantId,
            @Param("operator") String operator);

    IamRoleColumnPermissionTemplateBinding findRoleTemplateBinding(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId);

    List<IamRoleColumnPermissionTemplateBinding> findRoleBindingsByTemplateId(@Param("templateId") Long templateId);

    void disableRoleTemplateBinding(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId,
            @Param("operator") String operator);

    void insertRoleTemplateBinding(
            @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId,
            @Param("templateId") Long templateId,
            @Param("operator") String operator);

    void disableRoleTemplateBindingsOutsideTenantAssignments(
            @Param("tenantId") Long tenantId,
            @Param("operator") String operator);
}
