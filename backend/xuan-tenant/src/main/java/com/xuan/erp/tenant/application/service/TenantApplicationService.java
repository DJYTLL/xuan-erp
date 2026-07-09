package com.xuan.erp.tenant.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuan.erp.common.api.PageResult;
import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.command.ChangeTenantStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantCommand;
import com.xuan.erp.tenant.application.command.DeleteTenantCommand;
import com.xuan.erp.tenant.application.command.TenantAdminBootstrapCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantCommand;
import com.xuan.erp.tenant.application.query.TenantDetailView;
import com.xuan.erp.tenant.domain.model.Tenant;
import com.xuan.erp.tenant.domain.model.TenantDetailSupplement;
import com.xuan.erp.tenant.domain.model.TenantProvisionTask;
import com.xuan.erp.tenant.domain.model.TenantStatusHistory;
import com.xuan.erp.tenant.domain.model.type.ProvisionTaskStatus;
import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import com.xuan.erp.tenant.domain.repository.TenantProvisionTaskRepository;
import com.xuan.erp.tenant.domain.repository.TenantRepository;
import com.xuan.erp.tenant.domain.repository.TenantStatusHistoryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 租户应用服务，负责协调租户聚合的创建、查询、更新、状态流转、删除与写操作幂等。
 */
@Service
public class TenantApplicationService {

    private final TenantRepository tenantRepository;
    private final TenantStatusHistoryRepository statusHistoryRepository;
    private final TenantProvisionTaskRepository provisionTaskRepository;
    private final TenantProvisionApplicationService tenantProvisionApplicationService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TenantApplicationService(TenantRepository tenantRepository, TenantStatusHistoryRepository statusHistoryRepository) {
        this(tenantRepository, statusHistoryRepository, null, null);
    }

    public TenantApplicationService(
            TenantRepository tenantRepository,
            TenantStatusHistoryRepository statusHistoryRepository,
            TenantProvisionTaskRepository provisionTaskRepository) {
        this(tenantRepository, statusHistoryRepository, provisionTaskRepository, null);
    }

    public TenantApplicationService(
            TenantRepository tenantRepository,
            TenantStatusHistoryRepository statusHistoryRepository,
            @Nullable TenantProvisionTaskRepository provisionTaskRepository,
            @Nullable TenantProvisionApplicationService tenantProvisionApplicationService) {
        this(tenantRepository, statusHistoryRepository, provisionTaskRepository, tenantProvisionApplicationService, null);
    }

    @Autowired
    public TenantApplicationService(
            TenantRepository tenantRepository,
            TenantStatusHistoryRepository statusHistoryRepository,
            @Nullable TenantProvisionTaskRepository provisionTaskRepository,
            @Nullable TenantProvisionApplicationService tenantProvisionApplicationService,
            @Nullable PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.provisionTaskRepository = provisionTaskRepository;
        this.tenantProvisionApplicationService = tenantProvisionApplicationService;
        this.passwordEncoder = passwordEncoder == null ? PasswordEncoderFactories.createDelegatingPasswordEncoder() : passwordEncoder;
    }

    public TenantDetailView getTenant(Long tenantId) {
        Tenant tenant = requireTenant(tenantId);
        return toDetailView(tenant, tenantRepository.getDetailSupplement(tenantId));
    }

    public TenantDetailView createTenant(CreateTenantCommand command) {
        String normalizedCode = Tenant.normalizeCode(command.code());
        String taskKey = "tenant:create:" + normalizedCode;
        String operator = "system";
        return executeIdempotent(taskKey, command.idempotencyKey(), 0L, operator,
                () -> tenantRepository.findActiveByNormalizedCode(normalizedCode)
                        .map(this::toDetailView)
                        .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "租户不存在")),
                () -> {
                    tenantRepository.findActiveByNormalizedCode(normalizedCode)
                            .ifPresent(existing -> {
                                throw new BusinessException("TENANT_CODE_EXISTS", "租户编码已存在");
                            });
                    OffsetDateTime now = OffsetDateTime.now();
                    Tenant saved = tenantRepository.save(new Tenant(
                            null,
                            command.code().trim(),
                            normalizedCode,
                            requireText(command.name(), "租户名称不能为空"),
                            TenantStatus.PROVISIONING,
                            command.contactName(),
                            command.contactPhone(),
                            null,
                            null,
                            null,
                            null,
                            command.remark(),
                            operator,
                            now,
                            operator,
                            now,
                            null,
                            null,
                            null
                    ));
                    appendHistory(saved.id(), null, saved.status(), "CREATE", "租户创建", operator);
                    startProvisioning(saved.id(), taskKey, command.idempotencyKey(), operator, command);
                    return saved;
                });
    }

    public TenantDetailView updateTenant(Long tenantId, UpdateTenantCommand command) {
        String operator = operator("system");
        return executeIdempotent("tenant:update:" + tenantId, command.idempotencyKey(), tenantId, operator,
                () -> toDetailView(requireTenant(tenantId)),
                () -> tenantRepository.save(requireTenant(tenantId).updateProfile(
                        command.name(),
                        command.contactName(),
                        command.contactPhone(),
                        command.remark(),
                        operator,
                        OffsetDateTime.now())));
    }

    public List<TenantDetailView> listTenants() {
        return tenantRepository.findActiveTenants().stream()
                .map(this::toDetailView)
                .toList();
    }

    public PageResult<TenantDetailView> listTenants(long pageNum, long pageSize) {
        long normalizedPageNum = Math.max(pageNum, 1);
        long normalizedPageSize = Math.max(pageSize, 1);
        long offset = (normalizedPageNum - 1) * normalizedPageSize;
        List<TenantDetailView> records = tenantRepository.findActiveTenants(offset, normalizedPageSize).stream()
                .map(this::toDetailView)
                .toList();
        return new PageResult<>(records, tenantRepository.countActiveTenants(), normalizedPageNum, normalizedPageSize);
    }

    public TenantDetailView enableTenant(Long tenantId, ChangeTenantStatusCommand command) {
        String operator = operator(command.operator());
        String reason = requireText(command.reason(), "启用原因不能为空");
        return executeIdempotent("tenant:enable:" + tenantId, command.idempotencyKey(), tenantId, operator,
                () -> toDetailView(requireTenant(tenantId)),
                () -> {
                    Tenant tenant = requireTenant(tenantId);
                    Tenant saved = tenantRepository.save(tenant.enable(reason, operator, OffsetDateTime.now()));
                    appendHistory(saved.id(), tenant.status(), saved.status(), "ENABLE", reason, operator);
                    return saved;
                });
    }

    public TenantDetailView disableTenant(Long tenantId, ChangeTenantStatusCommand command) {
        String operator = operator(command.operator());
        String reason = requireText(command.reason(), "停用原因不能为空");
        return executeIdempotent("tenant:disable:" + tenantId, command.idempotencyKey(), tenantId, operator,
                () -> toDetailView(requireTenant(tenantId)),
                () -> {
                    Tenant tenant = requireTenant(tenantId);
                    Tenant saved = tenantRepository.save(tenant.disable(reason, operator, OffsetDateTime.now()));
                    appendHistory(saved.id(), tenant.status(), saved.status(), "DISABLE", reason, operator);
                    return saved;
                });
    }

    public void deleteTenant(Long tenantId, DeleteTenantCommand command) {
        String operator = operator(command.operator());
        String reason = requireText(command.reason(), "删除原因不能为空");
        executeIdempotent("tenant:delete:" + tenantId, command.idempotencyKey(), tenantId, operator,
                () -> null,
                () -> {
                    Tenant tenant = requireTenant(tenantId);
                    if (tenant.status() == TenantStatus.ENABLED) {
                        throw new BusinessException("TENANT_DELETE_FORBIDDEN", "启用中的租户不允许删除");
                    }
                    Tenant deleted = tenantRepository.save(tenant.markDeleted(reason, operator, OffsetDateTime.now()));
                    appendHistory(deleted.id(), tenant.status(), tenant.status(), "DELETE", reason, operator);
                    return deleted;
                });
    }

    private TenantDetailView executeIdempotent(
            String taskKey,
            String idempotencyKey,
            Long taskTenantId,
            String operator,
            Supplier<TenantDetailView> replayResultLoader,
            Supplier<Tenant> action) {
        if (provisionTaskRepository == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            Tenant result = action.get();
            return result == null ? null : toDetailView(result);
        }

        Optional<TenantProvisionTask> existing = provisionTaskRepository.findActiveByTaskKeyAndIdempotencyKey(taskKey, idempotencyKey.trim());
        if (existing.isPresent()) {
            TenantProvisionTask task = existing.get();
            if (task.status() == ProvisionTaskStatus.SUCCEEDED) {
                return replayResultLoader.get();
            }
            throw new BusinessException("TENANT_IDEMPOTENT_REQUEST_IN_PROGRESS", "相同幂等键的请求正在处理中");
        }

        OffsetDateTime now = OffsetDateTime.now();
        TenantProvisionTask runningTask = provisionTaskRepository.save(new TenantProvisionTask(
                null,
                taskTenantId,
                taskKey,
                "TENANT_WRITE",
                ProvisionTaskStatus.RUNNING,
                idempotencyKey.trim(),
                taskKey,
                toJson(Map.of("taskKey", taskKey, "idempotencyKey", idempotencyKey.trim())),
                "{}",
                0,
                5,
                null,
                null,
                now,
                null,
                operator,
                now,
                operator,
                now,
                null,
                null,
                null
        ));

        try {
            Tenant saved = action.get();
            provisionTaskRepository.save(new TenantProvisionTask(
                    runningTask.id(),
                    saved == null ? taskTenantId : saved.id() == null ? taskTenantId : saved.id(),
                    runningTask.taskKey(),
                    runningTask.taskType(),
                    ProvisionTaskStatus.SUCCEEDED,
                    runningTask.idempotencyKey(),
                    runningTask.stepName(),
                    runningTask.requestPayloadJson(),
                    toJson(saved == null ? Map.of("deleted", true) : Map.of("tenantId", saved.id())),
                    runningTask.retryCount(),
                    runningTask.maxRetryCount(),
                    null,
                    null,
                    runningTask.startedAt(),
                    OffsetDateTime.now(),
                    runningTask.createdBy(),
                    runningTask.createdAt(),
                    operator,
                    OffsetDateTime.now(),
                    null,
                    null,
                    null
            ));
            return saved == null ? null : toDetailView(saved);
        } catch (RuntimeException error) {
            provisionTaskRepository.save(new TenantProvisionTask(
                    runningTask.id(),
                    runningTask.tenantId(),
                    runningTask.taskKey(),
                    runningTask.taskType(),
                    ProvisionTaskStatus.FAILED,
                    runningTask.idempotencyKey(),
                    runningTask.stepName(),
                    runningTask.requestPayloadJson(),
                    runningTask.resultPayloadJson(),
                    runningTask.retryCount() + 1,
                    runningTask.maxRetryCount(),
                    error.getClass().getSimpleName(),
                    error.getMessage(),
                    runningTask.startedAt(),
                    OffsetDateTime.now(),
                    runningTask.createdBy(),
                    runningTask.createdAt(),
                    operator,
                    OffsetDateTime.now(),
                    null,
                    null,
                    null
            ));
            throw error;
        }
    }

    private TenantDetailView toDetailView(Tenant tenant) {
        return new TenantDetailView(
                tenant.id(),
                tenant.code(),
                tenant.name(),
                tenant.status(),
                tenant.contactName(),
                tenant.contactPhone(),
                tenant.provisionedAt(),
                tenant.enabledAt(),
                tenant.remark()
        );
    }

    private TenantDetailView toDetailView(Tenant tenant, TenantDetailSupplement supplement) {
        return new TenantDetailView(
                tenant.id(),
                tenant.code(),
                tenant.name(),
                tenant.status(),
                tenant.contactName(),
                tenant.contactPhone(),
                tenant.provisionedAt(),
                tenant.enabledAt(),
                tenant.remark(),
                supplement.currentPlanId(),
                supplement.currentPlanCode(),
                supplement.currentPlanName(),
                supplement.primaryDomainId(),
                supplement.primaryDomain(),
                supplement.statusHistoryCount(),
                supplement.latestStatusChangeType(),
                supplement.latestStatusChangedAt()
        );
    }

    private Tenant requireTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "租户不存在"));
    }

    private void appendHistory(Long tenantId, TenantStatus fromStatus, TenantStatus toStatus, String changeType, String reason, String operator) {
        OffsetDateTime now = OffsetDateTime.now();
        statusHistoryRepository.append(new TenantStatusHistory(
                null,
                tenantId,
                fromStatus,
                toStatus,
                changeType,
                reason,
                now,
                operator,
                null,
                null,
                "API",
                operator,
                now
        ));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("TENANT_INVALID_ARGUMENT", message);
        }
        return value.trim();
    }

    private String operator(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("failed to serialize idempotency payload", error);
        }
    }

    private void startProvisioning(Long tenantId, String taskKey, String idempotencyKey, String requestedBy, CreateTenantCommand command) {
        if (tenantProvisionApplicationService == null) {
            return;
        }
        tenantProvisionApplicationService.startProvisioning(tenantId, taskKey, idempotencyKey, requestedBy, adminBootstrap(command));
    }

    private TenantAdminBootstrapCommand adminBootstrap(CreateTenantCommand command) {
        String adminUsername = textOrDefault(command.adminUsername(), "admin");
        String adminPassword = textOrDefault(command.adminPassword(), "123456");
        String adminDisplayName = textOrDefault(command.adminDisplayName(), "租户管理员");
        return new TenantAdminBootstrapCommand(
                adminUsername,
                passwordEncoder.encode(adminPassword),
                adminDisplayName,
                textOrNull(command.adminEmail()),
                textOrNull(command.adminPhone()));
    }

    private String textOrDefault(String value, String defaultValue) {
        String text = textOrNull(value);
        return text == null ? defaultValue : text;
    }

    private String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
