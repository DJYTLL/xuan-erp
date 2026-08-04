package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplate;
import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplateItem;
import com.xuan.erp.iam.domain.model.IamResourceColumn;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionRule;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionTemplateBinding;
import com.xuan.erp.iam.domain.model.IamTenantColumnPermissionTemplateAssignment;
import com.xuan.erp.iam.domain.repository.IamColumnPermissionManagementRepository;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamColumnPermissionManagementPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 列权限管理仓储适配器。
 */
@Repository
public class IamColumnPermissionManagementRepositoryAdapter implements IamColumnPermissionManagementRepository {

    private final IamColumnPermissionManagementPersistenceMapper mapper;

    public IamColumnPermissionManagementRepositoryAdapter(IamColumnPermissionManagementPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<IamResourceColumn> findEnabledResourceColumns() {
        return mapper.findEnabledResourceColumns();
    }

    @Override
    public List<IamColumnPermissionTemplate> findActiveTemplates(Long tenantId, String keyword, Boolean enabled) {
        return mapper.findTemplates(tenantId, keyword, enabled);
    }

    @Override
    public Optional<IamColumnPermissionTemplate> findTemplateById(Long templateId) {
        return Optional.ofNullable(mapper.findTemplateById(templateId));
    }

    @Override
    public Optional<IamColumnPermissionTemplate> findTemplateByTenantIdAndCode(Long tenantId, String code) {
        return Optional.ofNullable(mapper.findTemplateByTenantIdAndCode(tenantId, code));
    }

    @Override
    public IamColumnPermissionTemplate saveTemplate(IamColumnPermissionTemplate template, String operator) {
        if (template.id() == null) {
            Long id = mapper.insertTemplate(
                    template.tenantId(),
                    template.code(),
                    template.name(),
                    template.description(),
                    template.enabled(),
                    operator);
            return mapper.findTemplateById(id);
        }
        mapper.updateTemplate(
                template.id(),
                template.name(),
                template.description(),
                template.enabled(),
                operator);
        return mapper.findTemplateById(template.id());
    }

    @Override
    public void setTemplateEnabled(Long templateId, boolean enabled, String operator) {
        mapper.setTemplateEnabled(templateId, enabled, operator);
    }

    @Override
    public List<IamColumnPermissionTemplateItem> findTemplateItems(Long templateId) {
        return mapper.findTemplateItems(templateId);
    }

    @Override
    public Optional<IamResourceColumn> findResourceColumnById(Long resourceColumnId) {
        return Optional.ofNullable(mapper.findResourceColumnById(resourceColumnId));
    }

    @Override
    @Transactional
    public void replaceTemplateItems(Long templateId, List<IamColumnPermissionTemplateItem> items, String operator) {
        mapper.disableTemplateItems(templateId, operator);
        for (IamColumnPermissionTemplateItem item : items) {
            mapper.insertTemplateItem(templateId, item.resourceColumnId(), item.accessMode(), operator);
        }
    }

    @Override
    public List<IamTenantColumnPermissionTemplateAssignment> findTenantTemplateAssignments(Long tenantId) {
        return mapper.findTenantTemplateAssignments(tenantId);
    }

    @Override
    public boolean existsTenantTemplateAssignment(Long tenantId, Long templateId) {
        return mapper.existsTenantTemplateAssignment(tenantId, templateId);
    }

    @Override
    @Transactional
    public void replaceTenantTemplateAssignments(Long tenantId, List<Long> templateIds, Long defaultTemplateId, String operator) {
        mapper.disableTenantTemplateAssignments(tenantId, operator);
        for (Long templateId : templateIds) {
            mapper.insertTenantTemplateAssignment(tenantId, templateId, templateId.equals(defaultTemplateId), operator);
        }
    }

    @Override
    public List<IamColumnPermissionTemplateItem> findTenantAssignableColumnRules(Long tenantId) {
        return mapper.findTenantAssignableColumnRules(tenantId);
    }

    @Override
    public List<IamRoleColumnPermissionRule> findRoleColumnPermissionRules(Long tenantId, Long roleId) {
        return mapper.findRoleColumnPermissionRules(tenantId, roleId);
    }

    @Override
    @Transactional
    public void replaceRoleColumnPermissionRules(Long tenantId, Long roleId, List<IamRoleColumnPermissionRule> rules, String operator) {
        mapper.disableRoleColumnPermissionRules(tenantId, roleId, operator);
        for (IamRoleColumnPermissionRule rule : rules) {
            mapper.insertRoleColumnPermissionRule(tenantId, roleId, rule.resourceColumnId(), rule.accessMode(), operator);
        }
    }

    @Override
    public void disableRoleColumnPermissionRulesOutsideTenantAssignments(Long tenantId, String operator) {
        mapper.disableRoleColumnPermissionRulesOutsideTenantAssignments(tenantId, operator);
    }

    @Override
    public Optional<IamRoleColumnPermissionTemplateBinding> findRoleTemplateBinding(Long tenantId, Long roleId) {
        return Optional.ofNullable(mapper.findRoleTemplateBinding(tenantId, roleId));
    }

    @Override
    public List<IamRoleColumnPermissionTemplateBinding> findRoleBindingsByTemplateId(Long templateId) {
        return mapper.findRoleBindingsByTemplateId(templateId);
    }

    @Override
    @Transactional
    public void replaceRoleTemplateBinding(Long tenantId, Long roleId, Long templateId, String operator) {
        mapper.disableRoleTemplateBinding(tenantId, roleId, operator);
        mapper.insertRoleTemplateBinding(tenantId, roleId, templateId, operator);
    }

    @Override
    public void disableRoleTemplateBindingsOutsideTenantAssignments(Long tenantId, String operator) {
        mapper.disableRoleTemplateBindingsOutsideTenantAssignments(tenantId, operator);
    }
}
