package com.xuan.erp.tenant.infrastructure.persistence.repository;

import com.xuan.erp.tenant.domain.model.TenantStatusHistory;
import com.xuan.erp.tenant.domain.repository.TenantStatusHistoryRepository;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TenantStatusHistoryRepositoryAdapter implements TenantStatusHistoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TenantStatusHistoryRepositoryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TenantStatusHistory append(TenantStatusHistory history) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO tenant_status_history (
                    tenant_id, from_status, to_status, change_type, change_reason,
                    changed_at, changed_by, trace_id, request_id, source, created_by, created_at
                ) VALUES (
                    :tenantId, :fromStatus, :toStatus, :changeType, :changeReason,
                    :changedAt, :changedBy, :traceId, :requestId, :source, :createdBy, :createdAt
                )
                """, params(history), keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        return new TenantStatusHistory(
                key == null ? history.id() : key.longValue(),
                history.tenantId(),
                history.fromStatus(),
                history.toStatus(),
                history.changeType(),
                history.changeReason(),
                history.changedAt(),
                history.changedBy(),
                history.traceId(),
                history.requestId(),
                history.source(),
                history.createdBy(),
                history.createdAt()
        );
    }

    private MapSqlParameterSource params(TenantStatusHistory history) {
        return new MapSqlParameterSource(Map.of(
                "tenantId", history.tenantId(),
                "toStatus", history.toStatus().code(),
                "changeType", history.changeType(),
                "changedAt", toTimestamp(history.changedAt()),
                "source", history.source(),
                "createdAt", toTimestamp(history.createdAt())
        ))
                .addValue("fromStatus", history.fromStatus() == null ? null : history.fromStatus().code())
                .addValue("changeReason", history.changeReason())
                .addValue("changedBy", history.changedBy())
                .addValue("traceId", history.traceId())
                .addValue("requestId", history.requestId())
                .addValue("createdBy", history.createdBy());
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }
}
