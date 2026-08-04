package com.xuan.erp.tenant.infrastructure.persistence.repository;

import com.xuan.erp.tenant.domain.model.Tenant;
import com.xuan.erp.tenant.domain.model.TenantDetailSupplement;
import com.xuan.erp.tenant.domain.repository.TenantRepository;
import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantDetailSupplementRecord;
import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantRecord;
import com.xuan.erp.tenant.infrastructure.persistence.mapper.TenantPersistenceMapper;
import com.xuan.erp.tenant.domain.model.type.TenantStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRepositoryAdapter implements TenantRepository {

    private final TenantPersistenceMapper mapper;

    public TenantRepositoryAdapter(TenantPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Tenant> findActiveByNormalizedCode(String normalizedCode) {
        return Optional.ofNullable(mapper.findActiveByNormalizedCode(normalizedCode)).map(this::toDomain);
    }

    @Override
    public List<Tenant> findActiveTenants() {
        return mapper.findPage(0, Long.MAX_VALUE).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Tenant> findActiveTenants(long offset, long limit) {
        return mapper.findPage(offset, limit).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countActiveTenants() {
        return mapper.countActive();
    }

    @Override
    public TenantDetailSupplement getDetailSupplement(Long tenantId) {
        return Optional.ofNullable(mapper.findDetailSupplement(tenantId))
                .map(this::toDetailSupplement)
                .orElse(TenantDetailSupplement.empty());
    }

    @Override
    public Tenant save(Tenant tenant) {
        if (tenant.id() == null) {
            mapper.insert(toRecord(tenant));
            return mapper.findActiveByNormalizedCode(tenant.normalizedCode()) == null
                    ? tenant
                    : toDomain(mapper.findActiveByNormalizedCode(tenant.normalizedCode()));
        }
        mapper.update(toRecord(tenant));
        return Optional.ofNullable(mapper.findByIdIncludingDeleted(tenant.id()))
                .map(this::toDomain)
                .orElse(tenant);
    }

    private Tenant toDomain(TenantRecord record) {
        return new Tenant(
                record.id(),
                record.code(),
                record.normalizedCode(),
                record.name(),
                TenantStatus.fromCode(record.status()),
                record.contactName(),
                record.contactPhone(),
                record.provisionedAt(),
                record.enabledAt(),
                record.disabledAt(),
                record.disabledReason(),
                record.remark(),
                record.createdBy(),
                record.createdAt(),
                record.updatedBy(),
                record.updatedAt(),
                record.deletedBy(),
                record.deleteReason(),
                record.deletedAt()
        );
    }

    private TenantDetailSupplement toDetailSupplement(TenantDetailSupplementRecord record) {
        return new TenantDetailSupplement(
                record.currentPlanAssignmentId(),
                record.currentPlanId(),
                record.currentPlanCode(),
                record.currentPlanName(),
                record.currentPlanExpiresAt(),
                record.primaryDomainId(),
                record.primaryDomain(),
                record.permissionSyncExpectedHash(),
                record.permissionSyncStatus(),
                record.permissionSyncLastCheckedAt(),
                record.permissionSyncLastSyncedAt(),
                record.permissionSyncLastErrorCode(),
                record.permissionSyncLastErrorMessage(),
                record.statusHistoryCount() == null ? 0L : record.statusHistoryCount(),
                record.latestStatusChangeType(),
                record.latestStatusChangedAt()
        );
    }

    private TenantRecord toRecord(Tenant tenant) {
        return new TenantRecord(
                tenant.id(),
                tenant.code(),
                tenant.normalizedCode(),
                tenant.name(),
                tenant.status().code(),
                tenant.contactName(),
                tenant.contactPhone(),
                tenant.provisionedAt(),
                tenant.enabledAt(),
                tenant.disabledAt(),
                tenant.disabledReason(),
                tenant.remark(),
                tenant.createdBy(),
                tenant.createdAt(),
                tenant.updatedBy(),
                tenant.updatedAt(),
                tenant.deletedBy(),
                tenant.deleteReason(),
                tenant.deletedAt()
        );
    }
}
