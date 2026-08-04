package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.CreateIamRoleCommand;
import com.xuan.erp.iam.application.command.SetIamRolePermissionsCommand;
import com.xuan.erp.iam.application.command.UpdateIamRoleCommand;
import com.xuan.erp.iam.application.query.IamAssignablePermissionView;
import com.xuan.erp.iam.application.query.IamRolePermissionGrantView;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.model.IamPermission;
import com.xuan.erp.iam.domain.model.IamRole;
import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import com.xuan.erp.iam.domain.repository.IamPermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRolePermissionRepository;
import com.xuan.erp.iam.domain.repository.IamRoleRepository;
import com.xuan.erp.iam.domain.repository.IamTenantPermissionEntitlementRepository;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * IAM 角色应用服务，负责租户内角色查询用例。
 */
public class IamRoleApplicationService {

    private final IamRoleRepository roleRepository;
    private final IamPermissionRepository permissionRepository;
    private final IamRolePermissionRepository rolePermissionRepository;
    private final IamUserRepository userRepository;
    private final IamAuthorizationSnapshotRepository snapshotRepository;
    private final IamTenantPermissionEntitlementRepository tenantPermissionEntitlementRepository;

    public IamRoleApplicationService(
            IamRoleRepository roleRepository,
            IamPermissionRepository permissionRepository,
            IamRolePermissionRepository rolePermissionRepository,
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository snapshotRepository,
            IamTenantPermissionEntitlementRepository tenantPermissionEntitlementRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.snapshotRepository = snapshotRepository;
        this.tenantPermissionEntitlementRepository = tenantPermissionEntitlementRepository;
    }

    public List<IamRole> listRoles(Long tenantId) {
        return roleRepository.findActiveRoles(tenantId);
    }

    public IamRole createRole(CreateIamRoleCommand command) {
        requirePositive(command.tenantId(), "租户 ID 不能为空");
        String code = requireText(command.code(), "角色编码不能为空");
        if (roleRepository.findActiveByTenantIdAndCode(command.tenantId(), code).isPresent()) {
            throw new BusinessException("IAM_ROLE_CODE_EXISTS", "角色编码已存在");
        }
        OffsetDateTime now = OffsetDateTime.now();
        return roleRepository.save(new IamRole(
                null,
                command.tenantId(),
                code,
                requireText(command.name(), "角色名称不能为空"),
                trimToNull(command.description()),
                true,
                "system",
                now,
                "system",
                now,
                null,
                null,
                null));
    }

    public IamRole updateRole(Long roleId, UpdateIamRoleCommand command) {
        IamRole existing = requireWritableRole(roleId);
        OffsetDateTime now = OffsetDateTime.now();
        return roleRepository.save(new IamRole(
                existing.id(),
                existing.tenantId(),
                existing.code(),
                requireText(command.name(), "角色名称不能为空"),
                trimToNull(command.description()),
                command.enabled() == null ? existing.enabled() : command.enabled(),
                existing.createdBy(),
                existing.createdAt(),
                "system",
                now,
                null,
                null,
                null));
    }

    public IamRole setRoleEnabled(Long roleId, boolean enabled) {
        IamRole existing = requireWritableRole(roleId);
        OffsetDateTime now = OffsetDateTime.now();
        return roleRepository.save(new IamRole(
                existing.id(),
                existing.tenantId(),
                existing.code(),
                existing.name(),
                existing.description(),
                enabled,
                existing.createdBy(),
                existing.createdAt(),
                "system",
                now,
                null,
                null,
                null));
    }

    public IamRolePermissionGrantView getRolePermissions(Long tenantId, Long roleId) {
        requireRoleInTenant(tenantId, roleId);
        List<String> availablePermissionCodes = availablePermissionCodes(tenantId);
        return new IamRolePermissionGrantView(
                tenantId,
                roleId,
                trimByAvailablePermissionCodes(
                        rolePermissionRepository.findPermissionCodesByRoleId(tenantId, roleId),
                        availablePermissionCodes),
                availablePermissionCodes,
                availablePermissionViews(availablePermissionCodes));
    }

    @Transactional
    public IamRolePermissionGrantView replaceRolePermissions(SetIamRolePermissionsCommand command) {
        requireWritableRoleInTenant(command.tenantId(), command.roleId());
        List<String> codes = normalizeCodes(command.permissionCodes());
        List<String> availablePermissionCodes = availablePermissionCodes(command.tenantId());
        requireWithinTenantEntitlementPool(command.tenantId(), codes, availablePermissionCodes);
        List<IamPermission> permissions = codes.stream()
                .map(code -> permissionRepository.findByCode(code)
                        .filter(IamPermission::enabled)
                        .orElseThrow(() -> new BusinessException("IAM_PERMISSION_NOT_FOUND", "权限不存在或已停用: " + code)))
                .toList();
        rolePermissionRepository.replaceRolePermissions(
                command.tenantId(),
                command.roleId(),
                permissions.stream().map(IamPermission::id).sorted().toList(),
                trimToNull(command.operator()) == null ? "system" : command.operator().trim());
        refreshAffectedUserAuthorizationSnapshots(
                command.tenantId(),
                command.roleId(),
                trimToNull(command.operator()) == null ? "system" : command.operator().trim());
        return new IamRolePermissionGrantView(
                command.tenantId(),
                command.roleId(),
                codes,
                availablePermissionCodes,
                availablePermissionViews(availablePermissionCodes));
    }

    private List<String> availablePermissionCodes(Long tenantId) {
        if (tenantId != null && tenantId <= 0) {
            return permissionRepository.findActivePermissions().stream()
                    .filter(IamPermission::enabled)
                    .map(IamPermission::code)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .sorted()
                    .toList();
        }
        return tenantPermissionEntitlementRepository.findPermissionCodesByTenantId(tenantId).stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }

    private List<IamAssignablePermissionView> availablePermissionViews(List<String> availablePermissionCodes) {
        if (availablePermissionCodes == null || availablePermissionCodes.isEmpty()) {
            return List.of();
        }
        Map<String, Boolean> available = availablePermissionCodes.stream()
                .collect(LinkedHashMap::new, (map, code) -> map.put(code, true), LinkedHashMap::putAll);
        return permissionRepository.findActivePermissions().stream()
                .filter(IamPermission::enabled)
                .filter(permission -> available.containsKey(permission.code()))
                .map(permission -> new IamAssignablePermissionView(
                        permission.id(),
                        permission.code(),
                        permission.name(),
                        permission.serviceName(),
                        permission.menuCode(),
                        permission.description(),
                        permission.enabled()))
                .sorted((left, right) -> left.code().compareTo(right.code()))
                .toList();
    }

    private void requireWithinTenantEntitlementPool(Long tenantId, List<String> requestedCodes, List<String> availablePermissionCodes) {
        if (tenantId == null || tenantId <= 0 || requestedCodes.isEmpty()) {
            return;
        }
        Map<String, Boolean> available = availablePermissionCodes.stream()
                .collect(LinkedHashMap::new, (map, code) -> map.put(code, true), LinkedHashMap::putAll);
        List<String> outsideCodes = requestedCodes.stream()
                .filter(code -> !available.containsKey(code))
                .toList();
        if (!outsideCodes.isEmpty()) {
            throw new BusinessException(
                    "IAM_TENANT_PERMISSION_OUT_OF_SCOPE",
                    "角色授权不能超出租户权限池: " + String.join(", ", outsideCodes));
        }
    }

    private List<String> trimByAvailablePermissionCodes(List<String> permissionCodes, List<String> availablePermissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return List.of();
        }
        if (availablePermissionCodes == null || availablePermissionCodes.isEmpty()) {
            return List.of();
        }
        Map<String, Boolean> available = availablePermissionCodes.stream()
                .collect(LinkedHashMap::new, (map, code) -> map.put(code, true), LinkedHashMap::putAll);
        return permissionCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .filter(available::containsKey)
                .distinct()
                .sorted()
                .toList();
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

    private void refreshUserAuthorizationSnapshot(IamUser user, String operator) {
        OffsetDateTime now = OffsetDateTime.now();
        IamAuthorizationSnapshot existing = snapshotRepository
                .findByTenantIdAndUserId(user.tenantId(), user.id())
                .orElse(null);
        long nextAuthVersion = Math.max(user.authVersion(), existing == null ? user.authVersion() : existing.authVersion()) + 1;
        List<Long> roleIds = rolePermissionRepository.findRoleIdsByUserId(user.tenantId(), user.id());
        List<String> permissionCodes = rolePermissionRepository.findPermissionCodesByUserId(user.tenantId(), user.id());
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
                List.of(),
                existing == null ? Map.of() : existing.columnSettings(),
                IamAuthorizationSnapshotHash.from(roleIds, permissionCodes),
                existing == null ? null : existing.expiresAt(),
                now,
                existing == null ? operator : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                operator,
                now));
    }

    private void requireRoleInTenant(Long tenantId, Long roleId) {
        if (tenantId == null) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", "租户 ID 不能为空");
        }
        IamRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("IAM_ROLE_NOT_FOUND", "角色不存在"));
        if (!role.tenantId().equals(tenantId)) {
            throw new BusinessException("IAM_ROLE_TENANT_MISMATCH", "角色不属于指定租户");
        }
    }

    private IamRole requireWritableRole(Long roleId) {
        IamRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("IAM_ROLE_NOT_FOUND", "角色不存在"));
        ensureRoleWritable(role);
        return role;
    }

    private IamRole requireWritableRoleInTenant(Long tenantId, Long roleId) {
        if (tenantId == null) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", "租户 ID 不能为空");
        }
        IamRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("IAM_ROLE_NOT_FOUND", "角色不存在"));
        if (!role.tenantId().equals(tenantId)) {
            throw new BusinessException("IAM_ROLE_TENANT_MISMATCH", "角色不属于指定租户");
        }
        ensureRoleWritable(role);
        return role;
    }

    private void ensureRoleWritable(IamRole role) {
        if (role.tenantId() != null && role.tenantId() == 0L) {
            throw new BusinessException("IAM_PLATFORM_ROLE_READ_ONLY", "平台级角色当前仅支持查看");
        }
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

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
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
