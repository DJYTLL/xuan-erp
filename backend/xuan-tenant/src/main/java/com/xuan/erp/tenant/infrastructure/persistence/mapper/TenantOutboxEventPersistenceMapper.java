package com.xuan.erp.tenant.infrastructure.persistence.mapper;

import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantOutboxEventRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantOutboxEventPersistenceMapper {

    TenantOutboxEventRecord findById(@Param("id") Long id);

    TenantOutboxEventRecord findLatestByEventId(@Param("eventId") String eventId);

    int insert(TenantOutboxEventRecord record);

    int update(TenantOutboxEventRecord record);
}
