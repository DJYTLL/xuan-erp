package com.xuan.erp.tenant.infrastructure.persistence.mapper;

import com.xuan.erp.tenant.infrastructure.persistence.entity.TenantOutboxEventRecord;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantOutboxEventPersistenceMapper {

    TenantOutboxEventRecord findById(@Param("id") Long id);

    TenantOutboxEventRecord findLatestByEventId(@Param("eventId") String eventId);

    List<TenantOutboxEventRecord> findPublishable(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    int insert(TenantOutboxEventRecord record);

    int update(TenantOutboxEventRecord record);
}
