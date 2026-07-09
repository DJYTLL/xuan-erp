package com.xuan.erp.tenant.infrastructure.persistence.mapper;

import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantRecord;
import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantDetailSupplementRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantPersistenceMapper {

    TenantRecord findById(@Param("id") Long id);

    TenantRecord findByIdIncludingDeleted(@Param("id") Long id);

    TenantRecord findActiveByNormalizedCode(@Param("normalizedCode") String normalizedCode);

    List<TenantRecord> findPage(@Param("offset") long offset, @Param("limit") long limit);

    long countActive();

    TenantDetailSupplementRecord findDetailSupplement(@Param("tenantId") Long tenantId);

    int insert(TenantRecord record);

    int update(TenantRecord record);
}
