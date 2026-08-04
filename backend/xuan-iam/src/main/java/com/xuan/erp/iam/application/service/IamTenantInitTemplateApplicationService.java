package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamTenantInitTemplateCommand;
import com.xuan.erp.iam.application.command.SetIamTenantInitTemplatePermissionsCommand;
import com.xuan.erp.iam.application.command.UpdateIamTenantInitTemplateCommand;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.model.IamTenantInitPermissionTemplate;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamTenantInitPermissionTemplateRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 租户初始化模板应用服务，负责模板维护和权限集合校验。
 */
@Service
public class IamTenantInitTemplateApplicationService {

    private final IamTenantInitPermissionTemplateRepository templateRepository;
    private final IamPermissionRepository permissionRepository;
    private final IamTenantPermissionEntitlementApplicationService entitlementApplicationService;

    public IamTenantInitTemplateApplicationService(
            IamTenantInitPermissionTemplateRepository templateRepository,
            IamPermissionRepository permissionRepository,
            IamTenantPermissionEntitlementApplicationService entitlementApplicationService) {
        this.templateRepository = templateRepository;
        this.permissionRepository = permissionRepository;
        this.entitlementApplicationService = entitlementApplicationService;
    }

    public List<IamTenantInitPermissionTemplate> listTemplates() {
        return templateRepository.findActiveTemplates();
    }

    @Transactional
    public IamTenantInitPermissionTemplate createTemplate(CreateIamTenantInitTemplateCommand command) {
        String code = requireText(command.code(), "模板编码不能为空");
        if (templateRepository.findActiveByCode(code).isPresent()) {
            throw new BusinessException("IAM_TENANT_INIT_TEMPLATE_CODE_EXISTS", "初始化模板编码已存在");
        }
        List<String> permissionCodes = requireEnabledPermissionCodes(command.permissionCodes());
        String operator = "system";
        OffsetDateTime now = OffsetDateTime.now();
        boolean defaultTemplate = Boolean.TRUE.equals(command.defaultTemplate());
        if (defaultTemplate) {
            templateRepository.clearDefaultTemplate(operator);
        }
        return templateRepository.save(new IamTenantInitPermissionTemplate(
                null,
                code,
                requireText(command.name(), "模板名称不能为空"),
                trimToNull(command.description()),
                permissionCodes,
                defaultTemplate,
                command.enabled() == null || command.enabled(),
                operator,
                now,
                operator,
                now,
                null,
                null,
                null));
    }

    @Transactional
    public IamTenantInitPermissionTemplate updateTemplate(Long templateId, UpdateIamTenantInitTemplateCommand command) {
        IamTenantInitPermissionTemplate existing = requireTemplate(templateId);
        String operator = "system";
        OffsetDateTime now = OffsetDateTime.now();
        boolean defaultTemplate = command.defaultTemplate() == null ? existing.defaultTemplate() : command.defaultTemplate();
        if (defaultTemplate && !existing.defaultTemplate()) {
            templateRepository.clearDefaultTemplate(operator);
        }
        return templateRepository.save(new IamTenantInitPermissionTemplate(
                existing.id(),
                existing.code(),
                requireText(command.name(), "模板名称不能为空"),
                trimToNull(command.description()),
                existing.permissionCodes(),
                defaultTemplate,
                command.enabled() == null ? existing.enabled() : command.enabled(),
                existing.createdBy(),
                existing.createdAt(),
                operator,
                now,
                null,
                null,
                null));
    }

    @Transactional
    public IamTenantInitPermissionTemplate replaceTemplatePermissions(SetIamTenantInitTemplatePermissionsCommand command) {
        IamTenantInitPermissionTemplate existing = requireTemplate(command.templateId());
        List<String> permissionCodes = requireEnabledPermissionCodes(command.permissionCodes());
        String operator = trimToNull(command.operator()) == null ? "system" : command.operator().trim();
        OffsetDateTime now = OffsetDateTime.now();
        IamTenantInitPermissionTemplate saved = templateRepository.save(new IamTenantInitPermissionTemplate(
                existing.id(),
                existing.code(),
                existing.name(),
                existing.description(),
                permissionCodes,
                existing.defaultTemplate(),
                existing.enabled(),
                existing.createdBy(),
                existing.createdAt(),
                operator,
                now,
                null,
                null,
                null));
        entitlementApplicationService.syncTemplateEntitlements(saved.code(), permissionCodes, operator);
        return saved;
    }

    private IamTenantInitPermissionTemplate requireTemplate(Long templateId) {
        if (templateId == null || templateId <= 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", "初始化模板 ID 不能为空");
        }
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("IAM_TENANT_INIT_TEMPLATE_NOT_FOUND", "初始化模板不存在"));
    }

    private List<String> requireEnabledPermissionCodes(List<String> values) {
        List<String> codes = normalizeCodes(values);
        for (String code : codes) {
            IamPermission permission = permissionRepository.findByCode(code)
                    .filter(IamPermission::enabled)
                    .orElseThrow(() -> new BusinessException("IAM_PERMISSION_NOT_FOUND", "权限不存在或已停用: " + code));
            if (!permission.code().equals(code)) {
                throw new BusinessException("IAM_PERMISSION_NOT_FOUND", "权限不存在或已停用: " + code);
            }
        }
        return codes;
    }

    private List<String> normalizeCodes(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(LinkedHashSet<String>::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .stream()
                .sorted()
                .toList();
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
