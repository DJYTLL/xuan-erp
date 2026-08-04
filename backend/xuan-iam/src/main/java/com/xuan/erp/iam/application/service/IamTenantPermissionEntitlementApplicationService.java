package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.port.IamTenantPlanUsageGateway;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IAM 租户权限池应用服务，负责同步租户可授权权限上限并裁剪越界角色授权。
 */
@Service
public class IamTenantPermissionEntitlementApplicationService {

    private static final String TENANT_ADMIN_ROLE_CODE = "tenant_admin";

    private final IamPermissionRepository permissionRepository;
    private final IamTenantPermissionEntitlementRepository entitlementRepository;
    private final IamRolePermissionRepository rolePermissionRepository;
    private final IamRoleRepository roleRepository;
    private final IamUserRepository userRepository;
    private final IamAuthorizationSnapshotRepository snapshotRepository;
    private final IamTenantPlanUsageGateway tenantPlanUsageGateway;

    public IamTenantPermissionEntitlementApplicationService(
            IamPermissionRepository permissionRepository,
            IamTenantPermissionEntitlementRepository entitlementRepository,
            IamRolePermissionRepository rolePermissionRepository,
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository snapshotRepository) {
        this(permissionRepository, entitlementRepository, rolePermissionRepository, null, userRepository, snapshotRepository, null);
    }

    public IamTenantPermissionEntitlementApplicationService(
            IamPermissionRepository permissionRepository,
            IamTenantPermissionEntitlementRepository entitlementRepository,
            IamRolePermissionRepository rolePermissionRepository,
            IamRoleRepository roleRepository,
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository snapshotRepository) {
        this(permissionRepository, entitlementRepository, rolePermissionRepository, roleRepository, userRepository, snapshotRepository, null);
    }

    public IamTenantPermissionEntitlementApplicationService(
            IamPermissionRepository permissionRepository,
            IamTenantPermissionEntitlementRepository entitlementRepository,
            IamRolePermissionRepository rolePermissionRepository,
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository snapshotRepository,
            IamTenantPlanUsageGateway tenantPlanUsageGateway) {
        this(permissionRepository, entitlementRepository, rolePermissionRepository, null, userRepository, snapshotRepository, tenantPlanUsageGateway);
    }

    @Autowired
    public IamTenantPermissionEntitlementApplicationService(
            IamPermissionRepository permissionRepository,
            IamTenantPermissionEntitlementRepository entitlementRepository,
            IamRolePermissionRepository rolePermissionRepository,
            IamRoleRepository roleRepository,
            IamUserRepository userRepository,
            IamAuthorizationSnapshotRepository snapshotRepository,
            IamTenantPlanUsageGateway tenantPlanUsageGateway) {
        this.permissionRepository = permissionRepository;
        this.entitlementRepository = entitlementRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.snapshotRepository = snapshotRepository;
        this.tenantPlanUsageGateway = tenantPlanUsageGateway;
    }

    @Transactional
    public void syncTemplateEntitlements(String initTemplateCode, List<String> permissionCodes, String operator) {
        String templateCode = requireText(initTemplateCode, "初始化模板编码不能为空");
        String resolvedOperator = resolveOperator(operator);
        for (Long tenantId : tenantIdsByInitTemplateCode(templateCode)) {
            replaceTenantEntitlements(tenantId, templateCode, permissionCodes, resolvedOperator);
        }
    }

    @Transactional
    public void replaceTenantEntitlements(
            Long tenantId,
            String initTemplateCode,
            List<String> permissionCodes,
            String operator) {
        requirePositive(tenantId, "租户 ID 不能为空");
        String templateCode = requireText(initTemplateCode, "初始化模板编码不能为空");
        String resolvedOperator = resolveOperator(operator);
        List<IamPermission> permissions = normalizeCodes(permissionCodes).stream()
                .map(code -> permissionRepository.findByCode(code)
                        .filter(IamPermission::enabled)
                        .orElseThrow(() -> new BusinessException("IAM_PERMISSION_NOT_FOUND", "权限不存在或已停用: " + code)))
                .toList();
        long nextVersion = entitlementRepository.nextEntitlementVersion(tenantId);
        List<Long> permissionIds = permissions.stream().map(IamPermission::id).sorted().toList();
        entitlementRepository.replaceTenantEntitlements(
                tenantId,
                templateCode,
                permissionIds,
                nextVersion,
                resolvedOperator);
        synchronizeTenantAdminRolePermissions(tenantId, permissionIds, resolvedOperator);
        rolePermissionRepository.removeRolePermissionsOutsideTenantEntitlements(tenantId, resolvedOperator);
        refreshTenantAuthorizationSnapshots(tenantId, resolvedOperator);
    }

    private void synchronizeTenantAdminRolePermissions(Long tenantId, List<Long> permissionIds, String operator) {
        if (roleRepository == null) {
            return;
        }
        IamRole adminRole = roleRepository.findActiveByTenantIdAndCode(tenantId, TENANT_ADMIN_ROLE_CODE)
                .orElseGet(() -> createTenantAdminRole(tenantId, operator));
        rolePermissionRepository.grantRoleToTenantAdmins(tenantId, adminRole.id(), operator);
        rolePermissionRepository.replaceRolePermissions(
                tenantId,
                adminRole.id(),
                permissionIds,
                operator);
    }

    private IamRole createTenantAdminRole(Long tenantId, String operator) {
        OffsetDateTime now = OffsetDateTime.now();
        return roleRepository.save(new IamRole(
                null,
                tenantId,
                TENANT_ADMIN_ROLE_CODE,
                "租户管理员",
                "租户内管理员角色，自动拥有租户权限池全部权限。",
                true,
                operator,
                now,
                operator,
                now,
                null,
                null,
                null));
    }

    private void refreshTenantAuthorizationSnapshots(Long tenantId, String operator) {
        for (IamUser user : userRepository.findActiveUsers(tenantId)) {
            refreshUserAuthorizationSnapshot(user, operator);
        }
    }

    private List<Long> tenantIdsByInitTemplateCode(String templateCode) {
        LinkedHashSet<Long> tenantIds = new LinkedHashSet<>();
        entitlementRepository.findTenantIdsByInitTemplateCode(templateCode).stream()
                .filter(this::positive)
                .forEach(tenantIds::add);
        if (tenantPlanUsageGateway != null) {
            tenantPlanUsageGateway.findActiveTenantIdsByIamInitTemplateCode(templateCode).stream()
                    .filter(this::positive)
                    .forEach(tenantIds::add);
        }
        return tenantIds.stream().sorted().toList();
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

    private boolean positive(Long value) {
        return value != null && value > 0;
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return trimmed;
    }

    private String resolveOperator(String operator) {
        String trimmed = trimToNull(operator);
        return trimmed == null ? "system" : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
