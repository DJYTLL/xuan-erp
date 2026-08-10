package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamResourceActionCommand;
import com.xuan.erp.iam.application.command.CreateIamResourceStateCommand;
import com.xuan.erp.iam.application.command.IamRoleStateActionRuleCommand;
import com.xuan.erp.iam.application.command.SetIamRoleStateActionRulesCommand;
import com.xuan.erp.iam.application.command.UpdateIamResourceActionCommand;
import com.xuan.erp.iam.application.command.UpdateIamResourceStateCommand;
import com.xuan.erp.iam.domain.model.IamResourceAction;
import com.xuan.erp.iam.domain.model.IamResourceState;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.model.IamRoleStateActionRule;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamColumnPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.domain.repository.IamStateActionRuleManagementRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 状态动作权限应用服务，负责资源状态、资源动作和角色状态动作矩阵维护。
 */
@Service
public class IamStateActionRuleApplicationService {

    private final IamStateActionRuleManagementRepository managementRepository;
    private final IamRoleRepository roleRepository;
    private final IamPermissionRepository permissionRepository;
    private final IamRolePermissionRepository rolePermissionRepository;
    private final IamUserRepository userRepository;
    private final IamAuthorizationSnapshotRepository snapshotRepository;
    private final IamColumnPermissionRepository columnPermissionRepository;

    public IamStateActionRuleApplicationService(
            IamStateActionRuleManagementRepository managementRepository,
            IamRoleRepository roleRepository,
            IamPermissionRepository permissionRepository,
            IamRolePermissionRepository rolePermissionRepository,
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository snapshotRepository,
            IamColumnPermissionRepository columnPermissionRepository) {
        this.managementRepository = managementRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.snapshotRepository = snapshotRepository;
        this.columnPermissionRepository = columnPermissionRepository;
    }

    public List<IamResourceState> listStates(Long tenantId, String resourceKey, Boolean enabled) {
        return managementRepository.findStates(tenantId, trimToNull(resourceKey), enabled);
    }

    public List<IamResourceAction> listActions(Long tenantId, String resourceKey, Boolean enabled) {
        return managementRepository.findActions(tenantId, trimToNull(resourceKey), enabled);
    }

    public List<IamRoleStateActionRule> listRoleStateActionRules(Long tenantId, Long roleId) {
        IamRole role = requireRoleInTenantForRead(tenantId, roleId);
        return managementRepository.findRoleRules(role.tenantId(), role.id());
    }

    @Transactional
    public IamResourceState createState(CreateIamResourceStateCommand command) {
        Long tenantId = requireNonNegativeTenant(command.tenantId());
        String resourceKey = normalizeResourceKey(command.resourceKey());
        String stateCode = normalizeStateCode(command.stateCode());
        if (managementRepository.findState(tenantId, resourceKey, stateCode).isPresent()) {
            throw new BusinessException("IAM_RESOURCE_STATE_CODE_EXISTS", "资源状态编码已存在");
        }
        return managementRepository.saveState(new IamResourceState(
                null,
                tenantId,
                resourceKey,
                stateCode,
                requireText(command.stateName(), "状态名称不能为空"),
                trimToNull(command.description()),
                command.sortNo() == null ? 0 : command.sortNo(),
                command.enabled() == null || command.enabled(),
                metadata(command.metadataJson())), operator(command.operator()));
    }

    @Transactional
    public IamResourceState updateState(Long stateId, UpdateIamResourceStateCommand command) {
        IamResourceState existing = requireStateById(stateId);
        return managementRepository.saveState(new IamResourceState(
                existing.id(),
                existing.tenantId(),
                existing.resourceKey(),
                existing.stateCode(),
                requireText(command.stateName(), "状态名称不能为空"),
                trimToNull(command.description()),
                command.sortNo() == null ? existing.sortNo() : command.sortNo(),
                command.enabled() == null ? existing.enabled() : command.enabled(),
                metadata(command.metadataJson())), operator(command.operator()));
    }

    @Transactional
    public IamResourceState setStateEnabled(Long stateId, boolean enabled, String operator) {
        IamResourceState existing = requireStateById(stateId);
        managementRepository.setStateEnabled(existing.id(), enabled, operator(operator));
        return new IamResourceState(
                existing.id(),
                existing.tenantId(),
                existing.resourceKey(),
                existing.stateCode(),
                existing.stateName(),
                existing.description(),
                existing.sortNo(),
                enabled,
                existing.metadataJson());
    }

    @Transactional
    public IamResourceAction createAction(CreateIamResourceActionCommand command) {
        Long tenantId = requireNonNegativeTenant(command.tenantId());
        String resourceKey = normalizeResourceKey(command.resourceKey());
        String actionCode = normalizeActionCode(command.actionCode());
        if (managementRepository.findAction(tenantId, resourceKey, actionCode).isPresent()) {
            throw new BusinessException("IAM_RESOURCE_ACTION_CODE_EXISTS", "资源动作编码已存在");
        }
        String permissionCode = normalizePermissionCode(command.permissionCode());
        requirePermissionIfPresent(permissionCode);
        return managementRepository.saveAction(new IamResourceAction(
                null,
                tenantId,
                resourceKey,
                actionCode,
                requireText(command.actionName(), "动作名称不能为空"),
                permissionCode,
                trimToNull(command.description()),
                command.sortNo() == null ? 0 : command.sortNo(),
                command.enabled() == null || command.enabled(),
                metadata(command.metadataJson())), operator(command.operator()));
    }

    @Transactional
    public IamResourceAction updateAction(Long actionId, UpdateIamResourceActionCommand command) {
        IamResourceAction existing = requireActionById(actionId);
        String permissionCode = normalizePermissionCode(command.permissionCode());
        requirePermissionIfPresent(permissionCode);
        return managementRepository.saveAction(new IamResourceAction(
                existing.id(),
                existing.tenantId(),
                existing.resourceKey(),
                existing.actionCode(),
                requireText(command.actionName(), "动作名称不能为空"),
                permissionCode,
                trimToNull(command.description()),
                command.sortNo() == null ? existing.sortNo() : command.sortNo(),
                command.enabled() == null ? existing.enabled() : command.enabled(),
                metadata(command.metadataJson())), operator(command.operator()));
    }

    @Transactional
    public IamResourceAction setActionEnabled(Long actionId, boolean enabled, String operator) {
        IamResourceAction existing = requireActionById(actionId);
        managementRepository.setActionEnabled(existing.id(), enabled, operator(operator));
        return new IamResourceAction(
                existing.id(),
                existing.tenantId(),
                existing.resourceKey(),
                existing.actionCode(),
                existing.actionName(),
                existing.permissionCode(),
                existing.description(),
                existing.sortNo(),
                enabled,
                existing.metadataJson());
    }

    @Transactional
    public List<IamRoleStateActionRule> replaceRoleStateActionRules(SetIamRoleStateActionRulesCommand command) {
        Long tenantId = requireNonNegativeTenant(command.tenantId());
        IamRole role = requireRoleInTenant(tenantId, command.roleId());
        List<IamRoleStateActionRule> rules = normalizeRules(tenantId, role.id(), command.rules());
        String operator = operator(command.operator());
        managementRepository.replaceRoleRules(tenantId, role.id(), rules, operator);
        refreshAffectedUserAuthorizationSnapshots(tenantId, role.id(), operator);
        return managementRepository.findRoleRules(tenantId, role.id());
    }

    private List<IamRoleStateActionRule> normalizeRules(
            Long tenantId,
            Long roleId,
            List<IamRoleStateActionRuleCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, IamRoleStateActionRule> rules = new LinkedHashMap<>();
        for (IamRoleStateActionRuleCommand command : commands) {
            String resourceKey = normalizeResourceKey(command.resourceKey());
            String stateCode = normalizeStateCode(command.stateCode());
            String actionCode = normalizeActionCode(command.actionCode());
            IamResourceState state = managementRepository.findState(tenantId, resourceKey, stateCode)
                    .filter(IamResourceState::enabled)
                    .orElseThrow(() -> new BusinessException("IAM_RESOURCE_STATE_NOT_FOUND", "资源状态不存在或已停用"));
            IamResourceAction action = managementRepository.findAction(tenantId, resourceKey, actionCode)
                    .filter(IamResourceAction::enabled)
                    .orElseThrow(() -> new BusinessException("IAM_RESOURCE_ACTION_NOT_FOUND", "资源动作不存在或已停用"));
            rules.put(resourceKey + ":" + state.stateCode() + ":" + action.actionCode(), new IamRoleStateActionRule(
                    null,
                    tenantId,
                    roleId,
                    resourceKey,
                    state.stateCode(),
                    action.actionCode(),
                    true));
        }
        return rules.values().stream().toList();
    }

    private void refreshAffectedUserAuthorizationSnapshots(Long tenantId, Long roleId, String operator) {
        // 状态动作规则实时从 IAM 聚合查询，旧快照只需推进版本让前端重新拉取当前权限快照。
        rolePermissionRepository.findUserIdsByRoleId(tenantId, roleId).stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(userId -> userRepository.findById(userId)
                        .ifPresent(user -> snapshotRepository.findByTenantIdAndUserId(tenantId, user.id())
                                .ifPresent(snapshot -> snapshotRepository.save(new com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot(
                                        snapshot.id(),
                                        snapshot.tenantId(),
                                        snapshot.userId(),
                                        snapshot.authVersion() + 1,
                                        snapshot.roleIds(),
                                        snapshot.permissionCodes(),
                                        snapshot.menuCodes(),
                                        columnPermissionRepository.findMergedColumnPermissionsByRoleIds(tenantId, snapshot.roleIds()),
                                        "state-action-refresh:" + tenantId + ":" + user.id(),
                                        snapshot.expiresAt(),
                                        snapshot.builtAt(),
                                        snapshot.createdBy(),
                                        snapshot.createdAt(),
                                        operator,
                                        java.time.OffsetDateTime.now())))));
    }

    private IamResourceState requireStateById(Long stateId) {
        return managementRepository.findStateById(requirePositive(stateId, "状态 ID 不能为空"))
                .orElseThrow(() -> new BusinessException("IAM_RESOURCE_STATE_NOT_FOUND", "资源状态不存在或已停用"));
    }

    private IamResourceAction requireActionById(Long actionId) {
        return managementRepository.findActionById(requirePositive(actionId, "动作 ID 不能为空"))
                .orElseThrow(() -> new BusinessException("IAM_RESOURCE_ACTION_NOT_FOUND", "资源动作不存在或已停用"));
    }

    private IamRole requireRoleInTenant(Long tenantId, Long roleId) {
        requireNonNegativeTenant(tenantId);
        IamRole role = requireRole(roleId);
        if (role.tenantId() <= 0) {
            throw new BusinessException("IAM_PLATFORM_ROLE_READ_ONLY", "平台级角色当前仅支持查看");
        }
        if (!role.tenantId().equals(tenantId)) {
            throw new BusinessException("IAM_ROLE_TENANT_MISMATCH", "角色不属于指定租户");
        }
        return role;
    }

    private IamRole requireRoleInTenantForRead(Long tenantId, Long roleId) {
        requireNonNegativeTenant(tenantId);
        IamRole role = requireRole(roleId);
        if (!role.tenantId().equals(tenantId)) {
            throw new BusinessException("IAM_ROLE_TENANT_MISMATCH", "角色不属于指定租户");
        }
        return role;
    }

    private IamRole requireRole(Long roleId) {
        return roleRepository.findById(requirePositive(roleId, "角色 ID 不能为空"))
                .orElseThrow(() -> new BusinessException("IAM_ROLE_NOT_FOUND", "角色不存在"));
    }

    private void requirePermissionIfPresent(String permissionCode) {
        if (permissionCode != null && permissionRepository.findByCode(permissionCode).filter(IamPermission -> IamPermission.enabled()).isEmpty()) {
            throw new BusinessException("IAM_PERMISSION_NOT_FOUND", "权限不存在或已停用: " + permissionCode);
        }
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

    private String normalizeResourceKey(String value) {
        return requireText(value, "资源标识不能为空").toLowerCase();
    }

    private String normalizeStateCode(String value) {
        return requireText(value, "状态编码不能为空").toUpperCase();
    }

    private String normalizeActionCode(String value) {
        return requireText(value, "动作编码不能为空").toLowerCase();
    }

    private String normalizePermissionCode(String value) {
        return trimToNull(value) == null ? null : value.trim();
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return trimmed;
    }

    private String metadata(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "{}" : trimmed;
    }

    private String operator(String value) {
        String operator = trimToNull(value);
        return operator == null ? "system" : operator;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
