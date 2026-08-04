package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.BootstrapTenantAdminCommand;
import com.xuan.erp.iam.application.port.IamTenantBootstrapGateway;
import com.xuan.erp.iam.domain.repository.IamTenantPermissionSyncStateRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * IAM 租户初始化应用服务。
 *
 * 负责租户开通后的 IAM 初始化用例编排：参数校验、操作者默认值处理，
 * 以及把真正的初始化动作委托给下层网关。
 */
@Service
public class IamTenantBootstrapApplicationService {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";
    private static final String DEFAULT_ADMIN_DISPLAY_NAME = "租户管理员";

    private final IamTenantBootstrapGateway bootstrapGateway;
    private final PasswordEncoder passwordEncoder;
    private final IamColumnPermissionApplicationService columnPermissionApplicationService;
    private final IamTenantPermissionSyncStateRepository syncStateRepository;

    public IamTenantBootstrapApplicationService(IamTenantBootstrapGateway bootstrapGateway, PasswordEncoder passwordEncoder) {
        this(bootstrapGateway, passwordEncoder, null, null);
    }

    public IamTenantBootstrapApplicationService(
            IamTenantBootstrapGateway bootstrapGateway,
            PasswordEncoder passwordEncoder,
            @Nullable IamColumnPermissionApplicationService columnPermissionApplicationService) {
        this(bootstrapGateway, passwordEncoder, columnPermissionApplicationService, null);
    }

    @Autowired
    public IamTenantBootstrapApplicationService(
            IamTenantBootstrapGateway bootstrapGateway,
            PasswordEncoder passwordEncoder,
            @Nullable IamColumnPermissionApplicationService columnPermissionApplicationService,
            @Nullable IamTenantPermissionSyncStateRepository syncStateRepository) {
        this.bootstrapGateway = bootstrapGateway;
        this.passwordEncoder = passwordEncoder;
        this.columnPermissionApplicationService = columnPermissionApplicationService;
        this.syncStateRepository = syncStateRepository;
    }

    /**
     * 初始化指定租户的 IAM 基础授权。
     *
     * 首次调用会写入租户菜单授权、初始化任务和 outbox 事件；
     * 重复调用由下层数据库函数按幂等键处理，通常返回 0。
     */
    public Integer bootstrapTenant(Long tenantId, String requestedBy) {
        return bootstrapTenant(tenantId, null, requestedBy);
    }

    public Integer bootstrapTenant(Long tenantId, BootstrapTenantAdminCommand adminCommand, String requestedBy) {
        return bootstrapTenant(tenantId, adminCommand, null, requestedBy);
    }

    /**
     * 初始化指定租户的 IAM 基础授权，并创建租户管理员账号。
     *
     * 管理员信息由租户开通流程传入；未传入时使用 admin/123456
     * 作为当前阶段的默认账号。明文密码会在进入数据库函数前转成哈希。
     */
    public Integer bootstrapTenant(
            Long tenantId,
            BootstrapTenantAdminCommand adminCommand,
            String iamInitTemplateCode,
            String requestedBy) {
        return bootstrapTenant(tenantId, adminCommand, iamInitTemplateCode, null, null, null, requestedBy);
    }

    public Integer bootstrapTenant(
            Long tenantId,
            BootstrapTenantAdminCommand adminCommand,
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode,
            String requestedBy) {
        return bootstrapTenant(
                tenantId,
                adminCommand,
                iamInitTemplateCode,
                columnPermissionTemplateCodes,
                defaultColumnPermissionTemplateCode,
                null,
                requestedBy);
    }

    public Integer bootstrapTenant(
            Long tenantId,
            BootstrapTenantAdminCommand adminCommand,
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode,
            String permissionHash,
            String requestedBy) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", "租户 ID 不能为空");
        }

        String operator = operator(requestedBy);
        String adminUsername = textOrDefault(adminCommand == null ? null : adminCommand.username(), DEFAULT_ADMIN_USERNAME);
        String adminPasswordHash = adminPasswordHash(adminCommand);
        String adminDisplayName = textOrDefault(adminCommand == null ? null : adminCommand.displayName(), DEFAULT_ADMIN_DISPLAY_NAME);
        String adminEmail = textOrNull(adminCommand == null ? null : adminCommand.email());
        String adminPhone = textOrNull(adminCommand == null ? null : adminCommand.phone());

        Integer insertedCount = bootstrapGateway.bootstrapTenant(
                tenantId,
                adminUsername,
                adminPasswordHash,
                adminDisplayName,
                adminEmail,
                adminPhone,
                textOrNull(iamInitTemplateCode),
                operator);
        if (columnPermissionApplicationService != null && columnPermissionTemplateCodes != null) {
            columnPermissionApplicationService.replaceTenantTemplateAssignmentsByCodes(
                    tenantId,
                    normalizeColumnTemplateCodes(columnPermissionTemplateCodes),
                    textOrNull(defaultColumnPermissionTemplateCode),
                    operator);
        }
        markPermissionHashSynced(tenantId, permissionHash, operator);
        return insertedCount;
    }

    public void ensureTenantPermissionSynced(
            Long tenantId,
            String iamInitTemplateCode,
            List<String> columnPermissionTemplateCodes,
            String defaultColumnPermissionTemplateCode,
            String permissionHash,
            String requestedBy) {
        String normalizedHash = textOrNull(permissionHash);
        if (tenantId == null || tenantId <= 0 || normalizedHash == null || syncStateRepository == null) {
            return;
        }
        if (syncStateRepository.findLastSyncedPermissionHash(tenantId)
                .filter(normalizedHash::equals)
                .isPresent()) {
            return;
        }
        bootstrapTenant(
                tenantId,
                null,
                iamInitTemplateCode,
                columnPermissionTemplateCodes,
                defaultColumnPermissionTemplateCode,
                normalizedHash,
                requestedBy);
    }

    public String lastSyncedPermissionHash(Long tenantId) {
        if (tenantId == null || tenantId <= 0 || syncStateRepository == null) {
            return null;
        }
        return syncStateRepository.findLastSyncedPermissionHash(tenantId).orElse(null);
    }

    /**
     * 规范化触发来源。
     *
     * 未传入时默认认为由租户开通流程触发。
     */
    private String operator(String requestedBy) {
        return requestedBy == null || requestedBy.isBlank() ? "tenant-provision" : requestedBy.trim();
    }

    private String adminPasswordHash(BootstrapTenantAdminCommand adminCommand) {
        if (adminCommand != null && adminCommand.passwordAlreadyEncoded()) {
            return requireText(adminCommand.password(), "管理员密码哈希不能为空");
        }
        String rawPassword = textOrDefault(adminCommand == null ? null : adminCommand.password(), DEFAULT_ADMIN_PASSWORD);
        return passwordEncoder.encode(rawPassword);
    }

    private String textOrDefault(String value, String defaultValue) {
        String text = textOrNull(value);
        return text == null ? defaultValue : text;
    }

    private String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String requireText(String value, String message) {
        String text = textOrNull(value);
        if (text == null) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
        return text;
    }

    private List<String> normalizeColumnTemplateCodes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::textOrNull)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet<String>::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .stream()
                .toList();
    }

    private void markPermissionHashSynced(Long tenantId, String permissionHash, String operator) {
        String normalizedHash = textOrNull(permissionHash);
        if (syncStateRepository == null || normalizedHash == null) {
            return;
        }
        syncStateRepository.markSynced(tenantId, normalizedHash, operator, OffsetDateTime.now());
    }
}
