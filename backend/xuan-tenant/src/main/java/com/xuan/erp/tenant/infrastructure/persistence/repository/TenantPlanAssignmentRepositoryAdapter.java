package com.xuan.erp.tenant.infrastructure.persistence.repository;

import com.xuan.erp.tenant.domain.model.TenantPlanAssignment;
import com.xuan.erp.tenant.domain.model.type.PlanAssignmentStatus;
import com.xuan.erp.tenant.domain.repository.TenantPlanAssignmentRepository;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TenantPlanAssignmentRepositoryAdapter implements TenantPlanAssignmentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TenantPlanAssignmentRepositoryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TenantPlanAssignment save(TenantPlanAssignment assignment) {
        if (assignment.id() != null) {
            throw new UnsupportedOperationException("Tenant plan assignment update is not supported yet");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO tenant_plan_assignment (
                    tenant_id, previous_plan_id, plan_id, status, effective_at, expires_at,
                    assigned_at, assigned_by, change_reason, source, remark,
                    created_by, created_at, updated_by, updated_at,
                    deleted_by, delete_reason, deleted_at
                ) VALUES (
                    :tenantId, :previousPlanId, :planId, :status, :effectiveAt, :expiresAt,
                    :assignedAt, :assignedBy, :changeReason, :source, :remark,
                    :createdBy, :createdAt, :updatedBy, :updatedAt,
                    :deletedBy, :deleteReason, :deletedAt
                )
                """, toParams(assignment), keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        return key == null ? assignment : new TenantPlanAssignment(
                key.longValue(),
                assignment.tenantId(),
                assignment.previousPlanId(),
                assignment.planId(),
                assignment.status(),
                assignment.effectiveAt(),
                assignment.expiresAt(),
                assignment.assignedAt(),
                assignment.assignedBy(),
                assignment.changeReason(),
                assignment.source(),
                assignment.remark(),
                assignment.createdBy(),
                assignment.createdAt(),
                assignment.updatedBy(),
                assignment.updatedAt(),
                assignment.deletedBy(),
                assignment.deleteReason(),
                assignment.deletedAt());
    }

    @Override
    public List<TenantPlanAssignment> findActiveByPlanId(Long planId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, previous_plan_id, plan_id, status, effective_at, expires_at,
                       assigned_at, assigned_by, change_reason, source, remark,
                       created_by, created_at, updated_by, updated_at,
                       deleted_by, delete_reason, deleted_at
                FROM tenant_plan_assignment
                WHERE plan_id = :planId
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                ORDER BY id
                """, new MapSqlParameterSource("planId", planId), (rs, rowNum) -> mapAssignment(rs));
    }

    private TenantPlanAssignment mapAssignment(ResultSet rs) throws java.sql.SQLException {
        return new TenantPlanAssignment(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                longValue(rs, "previous_plan_id"),
                rs.getLong("plan_id"),
                PlanAssignmentStatus.fromCode(rs.getString("status")),
                offsetDateTime(rs, "effective_at"),
                offsetDateTime(rs, "expires_at"),
                offsetDateTime(rs, "assigned_at"),
                rs.getString("assigned_by"),
                rs.getString("change_reason"),
                rs.getString("source"),
                rs.getString("remark"),
                rs.getString("created_by"),
                offsetDateTime(rs, "created_at"),
                rs.getString("updated_by"),
                offsetDateTime(rs, "updated_at"),
                rs.getString("deleted_by"),
                rs.getString("delete_reason"),
                offsetDateTime(rs, "deleted_at"));
    }

    private MapSqlParameterSource toParams(TenantPlanAssignment assignment) {
        return new MapSqlParameterSource()
                .addValue("tenantId", assignment.tenantId())
                .addValue("previousPlanId", assignment.previousPlanId())
                .addValue("planId", assignment.planId())
                .addValue("status", assignment.status().code())
                .addValue("effectiveAt", toTimestamp(assignment.effectiveAt()))
                .addValue("expiresAt", toTimestamp(assignment.expiresAt()))
                .addValue("assignedAt", toTimestamp(assignment.assignedAt()))
                .addValue("assignedBy", assignment.assignedBy())
                .addValue("changeReason", assignment.changeReason())
                .addValue("source", assignment.source())
                .addValue("remark", assignment.remark())
                .addValue("createdBy", assignment.createdBy())
                .addValue("createdAt", toTimestamp(assignment.createdAt()))
                .addValue("updatedBy", assignment.updatedBy())
                .addValue("updatedAt", toTimestamp(assignment.updatedAt()))
                .addValue("deletedBy", assignment.deletedBy())
                .addValue("deleteReason", assignment.deleteReason())
                .addValue("deletedAt", toTimestamp(assignment.deletedAt()));
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private OffsetDateTime offsetDateTime(ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
    }

    private Long longValue(ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
