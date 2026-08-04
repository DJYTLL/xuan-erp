package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplate;
import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplateItem;
import com.xuan.erp.iam.domain.model.IamResourceColumn;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionRule;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionTemplateBinding;
import com.xuan.erp.iam.domain.model.IamTenantColumnPermissionTemplateAssignment;
import java.util.List;
import java.util.Optional;

/**
 * IAM 列权限管理仓储端口，隔离模板维护和角色绑定的持久化细节。
 */
public interface IamColumnPermissionManagementRepository {

    List<IamResourceColumn> findEnabledResourceColumns();

    List<IamColumnPermissionTemplate> findActiveTemplates(Long tenantId, String keyword, Boolean enabled);

    Optional<IamColumnPermissionTemplate> findTemplateById(Long templateId);

    Optional<IamColumnPermissionTemplate> findTemplateByTenantIdAndCode(Long tenantId, String code);

    IamColumnPermissionTemplate saveTemplate(IamColumnPermissionTemplate template, String operator);

    void setTemplateEnabled(Long templateId, boolean enabled, String operator);

    List<IamColumnPermissionTemplateItem> findTemplateItems(Long templateId);

    Optional<IamResourceColumn> findResourceColumnById(Long resourceColumnId);

    void replaceTemplateItems(Long templateId, List<IamColumnPermissionTemplateItem> items, String operator);

    List<IamTenantColumnPermissionTemplateAssignment> findTenantTemplateAssignments(Long tenantId);

    boolean existsTenantTemplateAssignment(Long tenantId, Long templateId);

    void replaceTenantTemplateAssignments(Long tenantId, List<Long> templateIds, Long defaultTemplateId, String operator);

    List<IamColumnPermissionTemplateItem> findTenantAssignableColumnRules(Long tenantId);

    List<IamRoleColumnPermissionRule> findRoleColumnPermissionRules(Long tenantId, Long roleId);

    void replaceRoleColumnPermissionRules(Long tenantId, Long roleId, List<IamRoleColumnPermissionRule> rules, String operator);

    void disableRoleColumnPermissionRulesOutsideTenantAssignments(Long tenantId, String operator);

    Optional<IamRoleColumnPermissionTemplateBinding> findRoleTemplateBinding(Long tenantId, Long roleId);

    List<IamRoleColumnPermissionTemplateBinding> findRoleBindingsByTemplateId(Long templateId);

    void replaceRoleTemplateBinding(Long tenantId, Long roleId, Long templateId, String operator);

    void disableRoleTemplateBindingsOutsideTenantAssignments(Long tenantId, String operator);
}
