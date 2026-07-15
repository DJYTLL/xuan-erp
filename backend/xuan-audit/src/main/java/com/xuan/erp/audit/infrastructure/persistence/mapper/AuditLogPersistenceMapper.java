package com.xuan.erp.audit.infrastructure.persistence.mapper;

import com.xuan.erp.audit.application.query.AuditLogQuery;
import com.xuan.erp.audit.infrastructure.persistence.entity.AuditLogRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogPersistenceMapper {

    Long insert(@Param("auditLog") AuditLogRecord auditLog);

    List<AuditLogRecord> search(@Param("query") AuditLogQuery query);
}
