package com.xuan.erp.tenant.infrastructure.persistence.mapper;

import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantConfigRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantConfigPersistenceMapper {

    TenantConfigRecord findById(@Param("id") Long id);

    TenantConfigRecord findByIdIncludingDeleted(@Param("id") Long id);

    TenantConfigRecord findActiveByTenantIdAndKey(@Param("tenantId") Long tenantId, @Param("configKey") String configKey);

    List<TenantConfigRecord> findByTenantId(@Param("tenantId") Long tenantId, @Param("offset") long offset, @Param("limit") long limit);

    long countByTenantId(@Param("tenantId") Long tenantId);

    List<TenantConfigRecord> findPublicByTenantId(@Param("tenantId") Long tenantId);

    int insert(TenantConfigRecord record);

    int update(TenantConfigRecord record);
}
