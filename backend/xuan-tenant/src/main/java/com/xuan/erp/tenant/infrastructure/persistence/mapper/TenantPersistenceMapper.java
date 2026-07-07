package com.xuan.erp.tenant.infrastructure.persistence.mapper;

import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantRecord;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantPersistenceMapper {

    Optional<TenantRecord> findById(Long id);

    Optional<TenantRecord> findActiveByNormalizedCode(String normalizedCode);
}
