package com.xuan.erp.tenant.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.tenant.application.command.ChangeTenantStatusCommand;
import com.xuan.erp.tenant.application.command.CreateTenantCommand;
import com.xuan.erp.tenant.application.command.DeleteTenantCommand;
import com.xuan.erp.tenant.application.command.UpdateTenantCommand;
import com.xuan.erp.tenant.application.query.TenantDetailView;
import com.xuan.erp.tenant.domain.model.Tenant;
import com.xuan.erp.tenant.domain.model.TenantStatusHistory;
import com.xuan.erp.tenant.domain.repository.TenantRepository;
import com.xuan.erp.tenant.domain.repository.TenantStatusHistoryRepository;
import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TenantApplicationService {

    private final TenantRepository tenantRepository;
    private final TenantStatusHistoryRepository statusHistoryRepository;

    public TenantApplicationService(TenantRepository tenantRepository, TenantStatusHistoryRepository statusHistoryRepository) {
        this.tenantRepository = tenantRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public TenantDetailView getTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException("TENANT_NOT_FOUND", "租户不存在"));
        return toDetailView(tenant);
    }

    public TenantDetailView createTenant(CreateTenantCommand command) {
        String normalizedCode = Tenant.normalizeCode(command.code());
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
                "system",
                now,
                "system",
                now,
                null,
                null,
                null
        ));
        appendHistory(saved.id(), null, saved.status(), "CREATE", "租户创建", "system");
        return toDetailView(saved);
    }

    public TenantDetailView updateTenant(Long tenantId, UpdateTenantCommand command) {
        Tenant tenant = requireTenant(tenantId);
        OffsetDateTime now = OffsetDateTime.now();
        Tenant saved = tenantRepository.save(new Tenant(
                tenant.id(),
                tenant.code(),
                tenant.normalizedCode(),
                requireText(command.name(), "租户名称不能为空"),
                tenant.status(),
                command.contactName(),
                command.contactPhone(),
                tenant.provisionedAt(),
                tenant.enabledAt(),
                tenant.disabledAt(),
                tenant.disabledReason(),
                command.remark(),
                tenant.createdBy(),
                tenant.createdAt(),
                "system",
                now,
                tenant.deletedBy(),
                tenant.deleteReason(),
                tenant.deletedAt()
        ));
        return toDetailView(saved);
    }

    public List<TenantDetailView> listTenants() {
        return tenantRepository.findActiveTenants().stream()
                .map(this::toDetailView)
                .toList();
    }

    public TenantDetailView enableTenant(Long tenantId, ChangeTenantStatusCommand command) {
        Tenant tenant = requireTenant(tenantId);
        OffsetDateTime now = OffsetDateTime.now();
        Tenant saved = tenantRepository.save(new Tenant(
                tenant.id(),
                tenant.code(),
                tenant.normalizedCode(),
                tenant.name(),
                TenantStatus.ENABLED,
                tenant.contactName(),
                tenant.contactPhone(),
                tenant.provisionedAt() == null ? now : tenant.provisionedAt(),
                now,
                null,
                null,
                tenant.remark(),
                tenant.createdBy(),
                tenant.createdAt(),
                operator(command.operator()),
                now,
                tenant.deletedBy(),
                tenant.deleteReason(),
                tenant.deletedAt()
        ));
        appendHistory(saved.id(), tenant.status(), saved.status(), "ENABLE", command.reason(), operator(command.operator()));
        return toDetailView(saved);
    }

    public TenantDetailView disableTenant(Long tenantId, ChangeTenantStatusCommand command) {
        Tenant tenant = requireTenant(tenantId);
        String reason = requireText(command.reason(), "停用原因不能为空");
        OffsetDateTime now = OffsetDateTime.now();
        Tenant saved = tenantRepository.save(new Tenant(
                tenant.id(),
                tenant.code(),
                tenant.normalizedCode(),
                tenant.name(),
                TenantStatus.DISABLED,
                tenant.contactName(),
                tenant.contactPhone(),
                tenant.provisionedAt(),
                tenant.enabledAt(),
                now,
                reason,
                tenant.remark(),
                tenant.createdBy(),
                tenant.createdAt(),
                operator(command.operator()),
                now,
                tenant.deletedBy(),
                tenant.deleteReason(),
                tenant.deletedAt()
        ));
        appendHistory(saved.id(), tenant.status(), saved.status(), "DISABLE", reason, operator(command.operator()));
        return toDetailView(saved);
    }

    public void deleteTenant(Long tenantId, DeleteTenantCommand command) {
        Tenant tenant = requireTenant(tenantId);
        String reason = requireText(command.reason(), "删除原因不能为空");
        OffsetDateTime now = OffsetDateTime.now();
        Tenant saved = tenantRepository.save(new Tenant(
                tenant.id(),
                tenant.code(),
                tenant.normalizedCode(),
                tenant.name(),
                tenant.status(),
                tenant.contactName(),
                tenant.contactPhone(),
                tenant.provisionedAt(),
                tenant.enabledAt(),
                tenant.disabledAt(),
                tenant.disabledReason(),
                tenant.remark(),
                tenant.createdBy(),
                tenant.createdAt(),
                operator(command.operator()),
                now,
                operator(command.operator()),
                reason,
                now
        ));
        appendHistory(saved.id(), tenant.status(), tenant.status(), "DELETE", reason, operator(command.operator()));
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
}
