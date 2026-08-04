package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamColumnPermissionTemplateCommand;
import com.xuan.erp.iam.application.command.IamColumnPermissionTemplateItemCommand;
import com.xuan.erp.iam.application.command.IamRoleColumnPermissionRuleCommand;
import com.xuan.erp.iam.application.command.SetIamColumnPermissionTemplateItemsCommand;
import com.xuan.erp.iam.application.command.SetIamRoleColumnPermissionsCommand;
import com.xuan.erp.iam.application.command.SetIamRoleColumnPermissionTemplateCommand;
import com.xuan.erp.iam.application.command.SetIamTenantColumnPermissionTemplatesCommand;
import com.xuan.erp.iam.application.command.UpdateIamColumnPermissionTemplateCommand;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplate;
import com.xuan.erp.iam.domain.model.IamColumnPermissionTemplateItem;
import com.xuan.erp.iam.domain.model.IamResourceColumn;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionRule;
import com.xuan.erp.iam.domain.model.IamRoleColumnPermissionTemplateBinding;
import com.xuan.erp.iam.domain.model.IamTenantColumnPermissionTemplateAssignment;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamColumnPermissionManagementRepository;
import com.xuan.erp.iam.domain.repository.IamColumnPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 列权限应用服务，负责模板维护、模板规则维护和角色模板绑定。
 */
@Service
public class IamColumnPermissionApplicationService {

    private static final List<String> ACCESS_MODES = List.of("VISIBLE", "MASKED", "HIDDEN");

    private final IamColumnPermissionManagementRepository managementRepository;
    private final IamColumnPermissionRepository columnPermissionRepository;
    private final IamRoleRepository roleRepository;
    private final IamRolePermissionRepository rolePermissionRepository;
    private final IamUserRepository userRepository;
    private final IamAuthorizationSnapshotRepository snapshotRepository;

    public IamColumnPermissionApplicationService(
            IamColumnPermissionManagementRepository managementRepository,
            IamColumnPermissionRepository columnPermissionRepository,
            IamRoleRepository roleRepository,
            IamRolePermissionRepository rolePermissionRepository,
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository snapshotRepository) {
        this.managementRepository = managementRepository;
        this.columnPermissionRepository = columnPermissionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public List<IamResourceColumn> listResourceColumns() {
        return managementRepository.findEnabledResourceColumns();
    }

    public List<IamColumnPermissionTemplate> listTemplates(Long tenantId, String keyword, Boolean enabled) {
        return managementRepository.findActiveTemplates(tenantId, trimToNull(keyword), enabled);
    }

    public List<IamColumnPermissionTemplateItem> listTemplateItems(Long templateId) {
        requireTemplate(templateId);
        return managementRepository.findTemplateItems(templateId);
    }

    public IamRoleColumnPermissionTemplateBinding getRoleTemplateBinding(Long tenantId, Long roleId) {
        requireRoleInTenantForRead(tenantId, roleId);
        return managementRepository.findRoleTemplateBinding(tenantId, roleId).orElse(null);
    }

    public List<IamRoleColumnPermissionRule> listRoleColumnPermissionRules(Long tenantId, Long roleId) {
        IamRole role = requireRoleInTenantForRead(tenantId, roleId);
        return managementRepository.findRoleColumnPermissionRules(role.tenantId(), role.id());
    }

    public List<IamTenantColumnPermissionTemplateAssignment> listTenantTemplateAssignments(Long tenantId) {
        return managementRepository.findTenantTemplateAssignments(requirePositive(tenantId, "租户 ID 不能为空"));
    }

    @Transactional
    public IamColumnPermissionTemplate createTemplate(CreateIamColumnPermissionTemplateCommand command) {
        Long tenantId = requireNonNegativeTenant(command.tenantId());
        String code = requireText(command.code(), "模板编码不能为空");
        if (managementRepository.findTemplateByTenantIdAndCode(tenantId, code).isPresent()) {
            throw new BusinessException("IAM_COLUMN_PERMISSION_TEMPLATE_CODE_EXISTS", "列权限模板编码已存在");
        }
        return managementRepository.saveTemplate(new IamColumnPermissionTemplate(
                null,
                tenantId,
                code,
                requireText(command.name(), "模板名称不能为空"),
                trimToNull(command.description()),
                command.enabled() == null || command.enabled()), operator(command.operator()));
    }

    @Transactional
    public IamColumnPermissionTemplate updateTemplate(Long templateId, UpdateIamColumnPermissionTemplateCommand command) {
        IamColumnPermissionTemplate existing = requireTemplate(templateId);
        return managementRepository.saveTemplate(new IamColumnPermissionTemplate(
                existing.id(),
                existing.tenantId(),
                existing.code(),
                requireText(command.name(), "模板名称不能为空"),
                trimToNull(command.description()),
                command.enabled() == null ? existing.enabled() : command.enabled()), operator(command.operator()));
    }

    @Transactional
    public IamColumnPermissionTemplate setTemplateEnabled(Long templateId, boolean enabled, String operator) {
        IamColumnPermissionTemplate existing = requireTemplate(templateId);
        managementRepository.setTemplateEnabled(templateId, enabled, operator(operator));
        return new IamColumnPermissionTemplate(
                existing.id(),
                existing.tenantId(),
                existing.code(),
                existing.name(),
                existing.description(),
                enabled);
    }

    @Transactional
    public List<IamColumnPermissionTemplateItem> replaceTemplateItems(SetIamColumnPermissionTemplateItemsCommand command) {
        IamColumnPermissionTemplate template = requireTemplate(command.templateId());
        if (!template.enabled()) {
            throw new BusinessException("IAM_COLUMN_PERMISSION_TEMPLATE_DISABLED", "列权限模板已停用");
        }
        List<IamColumnPermissionTemplateItem> items = normalizeItems(template.id(), command.items());
        managementRepository.replaceTemplateItems(template.id(), items, operator(command.operator()));
        refreshUsersByTemplate(template.tenantId(), template.id(), operator(command.operator()));
        return items;
    }

    @Transactional
    public List<IamTenantColumnPermissionTemplateAssignment> replaceTenantTemplateAssignments(
            SetIamTenantColumnPermissionTemplatesCommand command) {
        Long tenantId = requirePositive(command.tenantId(), "租户 ID 不能为空");
        List<Long> templateIds = normalizeTemplateIds(command.templateIds());
        Long defaultTemplateId = resolveDefaultTemplateId(templateIds, command.defaultTemplateId());
        for (Long templateId : templateIds) {
            IamColumnPermissionTemplate template = requireTemplate(templateId);
            if (!template.enabled()) {
                throw new BusinessException("IAM_COLUMN_PERMISSION_TEMPLATE_DISABLED", "列权限模板已停用");
            }
            if (template.tenantId() != 0) {
                throw new BusinessException("IAM_COLUMN_PERMISSION_TEMPLATE_TENANT_MISMATCH", "只能给租户分配平台列权限模板");
            }
        }
        if (defaultTemplateId != null && !templateIds.contains(defaultTemplateId)) {
            throw new BusinessException("IAM_COLUMN_PERMISSION_DEFAULT_TEMPLATE_INVALID", "默认列权限模板必须在租户可用模板范围内");
        }
        String operator = operator(command.operator());
        managementRepository.replaceTenantTemplateAssignments(tenantId, templateIds, defaultTemplateId, operator);
        managementRepository.disableRoleTemplateBindingsOutsideTenantAssignments(tenantId, operator);
        managementRepository.disableRoleColumnPermissionRulesOutsideTenantAssignments(tenantId, operator);
        refreshTenantAuthorizationSnapshots(tenantId, operator);
        return managementRepository.findTenantTemplateAssignments(tenantId);
    }

    @Transactional
    public List<IamTenantColumnPermissionTemplateAssignment> replaceTenantTemplateAssignmentsByCodes(
            Long tenantId,
            List<String> templateCodes,
            String defaultTemplateCode,
            String operator) {
        Long normalizedTenantId = requirePositive(tenantId, "租户 ID 不能为空");
        List<String> codes = normalizeCodes(templateCodes);
        List<Long> templateIds = codes.stream()
                .map(code -> {
                    IamColumnPermissionTemplate template = managementRepository.findTemplateByTenantIdAndCode(0L, code)
                            .orElseThrow(() -> new BusinessException(
                                    "IAM_COLUMN_PERMISSION_TEMPLATE_NOT_FOUND",
                                    "列权限模板不存在：" + code));
                    if (!template.enabled()) {
                        throw new BusinessException("IAM_COLUMN_PERMISSION_TEMPLATE_DISABLED", "列权限模板已停用：" + code);
                    }
                    return template.id();
                })
                .toList();
        Long defaultTemplateId = null;
        String normalizedDefaultCode = trimToNull(defaultTemplateCode);
        if (normalizedDefaultCode != null) {
            int defaultIndex = codes.indexOf(normalizedDefaultCode);
            if (defaultIndex < 0) {
                throw new BusinessException("IAM_COLUMN_PERMISSION_DEFAULT_TEMPLATE_INVALID", "默认列权限模板必须在租户可用模板范围内");
            }
            defaultTemplateId = templateIds.get(defaultIndex);
        }
        return replaceTenantTemplateAssignments(new SetIamTenantColumnPermissionTemplatesCommand(
                normalizedTenantId,
                templateIds,
                defaultTemplateId,
                operator));
    }

    @Transactional
    public List<IamRoleColumnPermissionRule> replaceRoleColumnPermissionRules(SetIamRoleColumnPermissionsCommand command) {
        Long tenantId = requirePositive(command.tenantId(), "租户 ID 不能为空");
        IamRole role = requireRoleInTenant(tenantId, command.roleId());
        List<IamRoleColumnPermissionRule> rules = normalizeRoleColumnRules(tenantId, role.id(), command.rules());
        String operator = operator(command.operator());
        managementRepository.replaceRoleColumnPermissionRules(tenantId, role.id(), rules, operator);
        refreshAffectedUserAuthorizationSnapshots(tenantId, role.id(), operator);
        return managementRepository.findRoleColumnPermissionRules(tenantId, role.id());
    }

    @Transactional
    public IamRoleColumnPermissionTemplateBinding setRoleTemplateBinding(SetIamRoleColumnPermissionTemplateCommand command) {
        Long tenantId = requirePositive(command.tenantId(), "租户 ID 不能为空");
        IamRole role = requireRoleInTenant(tenantId, command.roleId());
        IamColumnPermissionTemplate template = requireTemplate(command.templateId());
        if (!template.enabled()) {
            throw new BusinessException("IAM_COLUMN_PERMISSION_TEMPLATE_DISABLED", "列权限模板已停用");
        }
        if (!managementRepository.existsTenantTemplateAssignment(tenantId, template.id())) {
            throw new BusinessException("IAM_COLUMN_PERMISSION_TEMPLATE_NOT_ASSIGNED", "列权限模板未分配给指定租户");
        }
        String operator = operator(command.operator());
        managementRepository.replaceRoleTemplateBinding(tenantId, role.id(), template.id(), operator);
        refreshAffectedUserAuthorizationSnapshots(tenantId, role.id(), operator);
        return managementRepository.findRoleTemplateBinding(tenantId, role.id())
                .orElse(new IamRoleColumnPermissionTemplateBinding(
                        tenantId,
                        role.id(),
                        template.id(),
                        template.code(),
                        template.name()));
    }

    private List<IamColumnPermissionTemplateItem> normalizeItems(
            Long templateId,
            List<IamColumnPermissionTemplateItemCommand> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<Long, String> accessModeByColumnId = new LinkedHashMap<>();
        for (IamColumnPermissionTemplateItemCommand item : items) {
            Long resourceColumnId = requirePositive(item.resourceColumnId(), "资源字段 ID 不能为空");
            String accessMode = normalizeAccessMode(item.accessMode());
                accessModeByColumnId.put(resourceColumnId, accessMode);
        }
        return accessModeByColumnId.entrySet().stream()
                .map(entry -> toTemplateItem(templateId, entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<IamRoleColumnPermissionRule> normalizeRoleColumnRules(
            Long tenantId,
            Long roleId,
            List<IamRoleColumnPermissionRuleCommand> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        Map<Long, IamColumnPermissionTemplateItem> assignableRuleByColumnId = new LinkedHashMap<>();
        for (IamColumnPermissionTemplateItem item : managementRepository.findTenantAssignableColumnRules(tenantId)) {
            assignableRuleByColumnId.merge(
                    item.resourceColumnId(),
                    item,
                    (current, next) -> accessRank(next.accessMode()) > accessRank(current.accessMode()) ? next : current);
        }
        Map<Long, String> accessModeByColumnId = new LinkedHashMap<>();
        for (IamRoleColumnPermissionRuleCommand rule : rules) {
            Long resourceColumnId = requirePositive(rule.resourceColumnId(), "资源字段 ID 不能为空");
            String accessMode = normalizeAccessMode(rule.accessMode());
            IamColumnPermissionTemplateItem maxRule = assignableRuleByColumnId.get(resourceColumnId);
            if (maxRule == null || accessRank(accessMode) > accessRank(maxRule.accessMode())) {
                throw new BusinessException("IAM_COLUMN_PERMISSION_RULE_OUT_OF_SCOPE", "角色列权限不能超出租户可用模板范围");
            }
            accessModeByColumnId.put(resourceColumnId, accessMode);
        }
        return accessModeByColumnId.entrySet().stream()
                .map(entry -> toRoleColumnRule(tenantId, roleId, entry.getKey(), entry.getValue()))
                .toList();
    }

    private IamRoleColumnPermissionRule toRoleColumnRule(
            Long tenantId,
            Long roleId,
            Long resourceColumnId,
            String accessMode) {
        IamResourceColumn column = managementRepository.findResourceColumnById(resourceColumnId)
                .filter(IamResourceColumn::enabled)
                .orElseThrow(() -> new BusinessException("IAM_RESOURCE_COLUMN_NOT_FOUND", "资源字段不存在或已停用"));
        return new IamRoleColumnPermissionRule(
                null,
                tenantId,
                roleId,
                column.id(),
                column.resourceKey(),
                column.columnKey(),
                column.columnName(),
                accessMode);
    }

    private List<Long> normalizeTemplateIds(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return List.of();
        }
        return templateIds.stream()
                .map(templateId -> requirePositive(templateId, "模板 ID 不能为空"))
                .collect(LinkedHashSet<Long>::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .stream()
                .sorted()
                .toList();
    }

    private List<String> normalizeCodes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet<String>::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .stream()
                .toList();
    }

    private Long resolveDefaultTemplateId(List<Long> templateIds, Long defaultTemplateId) {
        if (templateIds.isEmpty()) {
            return null;
        }
        if (defaultTemplateId == null) {
            return templateIds.get(0);
        }
        return requirePositive(defaultTemplateId, "默认模板 ID 不能为空");
    }

    private IamColumnPermissionTemplateItem toTemplateItem(Long templateId, Long resourceColumnId, String accessMode) {
        IamResourceColumn column = managementRepository.findResourceColumnById(resourceColumnId)
                .filter(IamResourceColumn::enabled)
                .orElseThrow(() -> new BusinessException("IAM_RESOURCE_COLUMN_NOT_FOUND", "资源字段不存在或已停用"));
        return new IamColumnPermissionTemplateItem(
                null,
                templateId,
                column.id(),
                column.resourceKey(),
                column.columnKey(),
                column.columnName(),
                accessMode);
    }

    private String normalizeAccessMode(String value) {
        String accessMode = requireText(value, "字段访问级别不能为空").toUpperCase();
        if (!ACCESS_MODES.contains(accessMode)) {
            throw new BusinessException("IAM_COLUMN_PERMISSION_ACCESS_MODE_INVALID", "字段访问级别只能是 VISIBLE、MASKED 或 HIDDEN");
        }
        return accessMode;
    }

    private int accessRank(String accessMode) {
        return switch (normalizeAccessMode(accessMode)) {
            case "VISIBLE" -> 3;
            case "MASKED" -> 2;
            default -> 1;
        };
    }

    private void refreshUsersByTemplate(Long templateTenantId, Long templateId, String operator) {
        for (IamRoleColumnPermissionTemplateBinding binding : managementRepository.findRoleBindingsByTemplateId(templateId)) {
            refreshAffectedUserAuthorizationSnapshots(binding.tenantId(), binding.roleId(), operator);
        }
    }

    private void refreshAffectedUserAuthorizationSnapshots(Long tenantId, Long roleId, String operator) {
        List<Long> userIds = rolePermissionRepository.findUserIdsByRoleId(tenantId, roleId).stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        for (Long userId : userIds) {
            userRepository.findById(userId)
                    .filter(user -> user.tenantId().equals(tenantId))
                    .ifPresent(user -> refreshUserAuthorizationSnapshot(user, operator));
        }
    }

    private void refreshTenantAuthorizationSnapshots(Long tenantId, String operator) {
        for (IamUser user : userRepository.findActiveUsers(tenantId)) {
            refreshUserAuthorizationSnapshot(user, operator);
        }
    }

    private void refreshUserAuthorizationSnapshot(IamUser user, String operator) {
        OffsetDateTime now = OffsetDateTime.now();
        IamAuthorizationSnapshot existing = snapshotRepository
                .findByTenantIdAndUserId(user.tenantId(), user.id())
                .orElse(null);
        long baseAuthVersion = existing == null ? user.authVersion() : Math.max(user.authVersion(), existing.authVersion());
        long nextAuthVersion = baseAuthVersion + 1;
        List<Long> roleIds = rolePermissionRepository.findRoleIdsByUserId(user.tenantId(), user.id()).stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        List<String> permissionCodes = normalizeStrings(rolePermissionRepository.findPermissionCodesByUserId(user.tenantId(), user.id()));
        List<String> menuCodes = existing == null ? List.of() : existing.menuCodes();
        Map<String, Map<String, String>> columnSettings = copyColumnSettings(
                columnPermissionRepository.findMergedColumnPermissionsByRoleIds(user.tenantId(), roleIds));
        userRepository.save(new IamUser(
                user.id(),
                user.tenantId(),
                user.username(),
                user.passwordHash(),
                user.displayName(),
                user.email(),
                user.phone(),
                user.avatarUrl(),
                user.enabled(),
                user.accountNonExpired(),
                user.accountNonLocked(),
                user.credentialsNonExpired(),
                user.lastLoginAt(),
                user.passwordChangedAt(),
                user.failedLoginCount(),
                user.lastFailedLoginAt(),
                user.lockedUntil(),
                user.mfaEnabled(),
                nextAuthVersion,
                user.remark(),
                user.createdBy(),
                user.createdAt(),
                operator,
                now,
                user.deletedBy(),
                user.deleteReason(),
                user.deletedAt()));
        snapshotRepository.save(new IamAuthorizationSnapshot(
                existing == null ? null : existing.id(),
                user.tenantId(),
                user.id(),
                nextAuthVersion,
                roleIds,
                permissionCodes,
                menuCodes,
                columnSettings,
                IamAuthorizationSnapshotHash.from(roleIds, permissionCodes, menuCodes),
                existing == null ? null : existing.expiresAt(),
                now,
                existing == null ? operator : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                operator,
                now));
    }

    private IamColumnPermissionTemplate requireTemplate(Long templateId) {
        Long id = requirePositive(templateId, "模板 ID 不能为空");
        return managementRepository.findTemplateById(id)
                .orElseThrow(() -> new BusinessException("IAM_COLUMN_PERMISSION_TEMPLATE_NOT_FOUND", "列权限模板不存在"));
    }

    private IamRole requireRoleInTenant(Long tenantId, Long roleId) {
        requirePositive(tenantId, "租户 ID 不能为空");
        IamRole role = roleRepository.findById(requirePositive(roleId, "角色 ID 不能为空"))
                .orElseThrow(() -> new BusinessException("IAM_ROLE_NOT_FOUND", "角色不存在"));
        if (!role.tenantId().equals(tenantId)) {
            throw new BusinessException("IAM_ROLE_TENANT_MISMATCH", "角色不属于指定租户");
        }
        return role;
    }

    private IamRole requireRoleInTenantForRead(Long tenantId, Long roleId) {
        requireNonNegativeTenant(tenantId);
        IamRole role = roleRepository.findById(requirePositive(roleId, "角色 ID 不能为空"))
                .orElseThrow(() -> new BusinessException("IAM_ROLE_NOT_FOUND", "角色不存在"));
        if (!role.tenantId().equals(tenantId)) {
            throw new BusinessException("IAM_ROLE_TENANT_MISMATCH", "角色不属于指定租户");
        }
        return role;
    }

    private Long requireNonNegativeTenant(Long value) {
        if (value == null || value < 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", "租户 ID 不能为空");
        }
        return value;
    }

    private Long requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return value;
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return trimmed;
    }

    private String operator(String value) {
        String operator = trimToNull(value);
        return operator == null ? "system" : operator;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<String> normalizeStrings(List<String> values) {
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

    private Map<String, Map<String, String>> copyColumnSettings(Map<String, Map<String, String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        values.forEach((resourceKey, columns) -> {
            if (resourceKey == null || resourceKey.isBlank() || columns == null || columns.isEmpty()) {
                return;
            }
            Map<String, String> columnModes = new LinkedHashMap<>();
            columns.forEach((columnKey, accessMode) -> {
                if (columnKey != null && !columnKey.isBlank() && accessMode != null && !accessMode.isBlank()) {
                    columnModes.put(columnKey.trim(), normalizeAccessMode(accessMode));
                }
            });
            if (!columnModes.isEmpty()) {
                result.put(resourceKey.trim(), Map.copyOf(columnModes));
            }
        });
        return Map.copyOf(result);
    }
}
