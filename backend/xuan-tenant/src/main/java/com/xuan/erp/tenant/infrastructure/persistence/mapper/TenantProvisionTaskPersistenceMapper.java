package com.xuan.erp.tenant.infrastructure.persistence.mapper;

import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantProvisionTaskRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantProvisionTaskPersistenceMapper {

    TenantProvisionTaskRecord findById(@Param("id") Long id);

    java.util.List<TenantProvisionTaskRecord> findActiveByTenantId(@Param("tenantId") Long tenantId);

    TenantProvisionTaskRecord findActiveByTenantIdAndTaskKey(@Param("tenantId") Long tenantId, @Param("taskKey") String taskKey);

    TenantProvisionTaskRecord findActiveByTaskKeyAndIdempotencyKey(@Param("taskKey") String taskKey, @Param("idempotencyKey") String idempotencyKey);

    TenantProvisionTaskRecord findByIdIncludingDeleted(@Param("id") Long id);

    int insert(TenantProvisionTaskRecord record);

    int update(TenantProvisionTaskRecord record);
}
