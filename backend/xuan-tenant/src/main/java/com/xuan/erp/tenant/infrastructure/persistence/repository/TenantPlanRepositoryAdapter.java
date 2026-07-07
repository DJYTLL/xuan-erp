package com.xuan.erp.tenant.infrastructure.persistence.repository;

import com.xuan.erp.tenant.domain.model.TenantPlan;
import com.xuan.erp.tenant.domain.model.type.BillingCycle;
import com.xuan.erp.tenant.domain.model.type.TenantPlanStatus;
import com.xuan.erp.tenant.domain.repository.TenantPlanRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TenantPlanRepositoryAdapter implements TenantPlanRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TenantPlanRepositoryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<TenantPlan> findById(Long id) {
        List<TenantPlan> plans = jdbcTemplate.query("""
                SELECT *
                FROM tenant_plan
                WHERE id = :id
                  AND deleted_at IS NULL
                """, Map.of("id", id), new TenantPlanRowMapper());
        return plans.stream().findFirst();
    }

    @Override
    public Optional<TenantPlan> findActiveByCode(String code) {
        List<TenantPlan> plans = jdbcTemplate.query("""
                SELECT *
                FROM tenant_plan
                WHERE code = :code
                  AND deleted_at IS NULL
                ORDER BY id
                LIMIT 1
                """, Map.of("code", code), new TenantPlanRowMapper());
        return plans.stream().findFirst();
    }

    @Override
    public List<TenantPlan> findActivePlans() {
        return jdbcTemplate.query("""
                SELECT *
                FROM tenant_plan
                WHERE deleted_at IS NULL
                ORDER BY sort_no, id
                """, Map.of(), new TenantPlanRowMapper());
    }

    @Override
    public TenantPlan save(TenantPlan plan) {
        if (plan.id() == null) {
            return insert(plan);
        }
        update(plan);
        return findByIdIncludingDeleted(plan.id()).orElse(plan);
    }

    private TenantPlan insert(TenantPlan plan) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO tenant_plan (
                    code, name, status, billing_cycle, price_amount, currency,
                    max_user_count, max_warehouse_count, max_storage_gb, feature_flags,
                    sort_no, remark, created_by, created_at, updated_by, updated_at,
                    deleted_by, delete_reason, deleted_at
                ) VALUES (
                    :code, :name, :status, :billingCycle, :priceAmount, :currency,
                    :maxUserCount, :maxWarehouseCount, :maxStorageGb, CAST(:featureFlagsJson AS jsonb),
                    :sortNo, :remark, :createdBy, :createdAt, :updatedBy, :updatedAt,
                    :deletedBy, :deleteReason, :deletedAt
                )
                """, toParams(plan), keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        return key == null ? plan : findByIdIncludingDeleted(key.longValue()).orElse(plan);
    }

    private void update(TenantPlan plan) {
        jdbcTemplate.update("""
                UPDATE tenant_plan
                SET code = :code,
                    name = :name,
                    status = :status,
                    billing_cycle = :billingCycle,
                    price_amount = :priceAmount,
                    currency = :currency,
                    max_user_count = :maxUserCount,
                    max_warehouse_count = :maxWarehouseCount,
                    max_storage_gb = :maxStorageGb,
                    feature_flags = CAST(:featureFlagsJson AS jsonb),
                    sort_no = :sortNo,
                    remark = :remark,
                    created_by = :createdBy,
                    created_at = :createdAt,
                    updated_by = :updatedBy,
                    updated_at = :updatedAt,
                    deleted_by = :deletedBy,
                    delete_reason = :deleteReason,
                    deleted_at = :deletedAt
                WHERE id = :id
                """, toParams(plan));
    }

    private Optional<TenantPlan> findByIdIncludingDeleted(Long id) {
        List<TenantPlan> plans = jdbcTemplate.query("""
                SELECT *
                FROM tenant_plan
                WHERE id = :id
                """, Map.of("id", id), new TenantPlanRowMapper());
        return plans.stream().findFirst();
    }

    private MapSqlParameterSource toParams(TenantPlan plan) {
        return new MapSqlParameterSource()
                .addValue("id", plan.id())
                .addValue("code", plan.code())
                .addValue("name", plan.name())
                .addValue("status", plan.status().code())
                .addValue("billingCycle", plan.billingCycle().code())
                .addValue("priceAmount", plan.priceAmount())
                .addValue("currency", plan.currency())
                .addValue("maxUserCount", plan.maxUserCount())
                .addValue("maxWarehouseCount", plan.maxWarehouseCount())
                .addValue("maxStorageGb", plan.maxStorageGb())
                .addValue("featureFlagsJson", plan.featureFlagsJson())
                .addValue("sortNo", plan.sortNo())
                .addValue("remark", plan.remark())
                .addValue("createdBy", plan.createdBy())
                .addValue("createdAt", toTimestamp(plan.createdAt()))
                .addValue("updatedBy", plan.updatedBy())
                .addValue("updatedAt", toTimestamp(plan.updatedAt()))
                .addValue("deletedBy", plan.deletedBy())
                .addValue("deleteReason", plan.deleteReason())
                .addValue("deletedAt", toTimestamp(plan.deletedAt()));
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static final class TenantPlanRowMapper implements RowMapper<TenantPlan> {

        @Override
        public TenantPlan mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TenantPlan(
                    rs.getLong("id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    TenantPlanStatus.fromCode(rs.getString("status")),
                    BillingCycle.fromCode(rs.getString("billing_cycle")),
                    rs.getBigDecimal("price_amount"),
                    rs.getString("currency"),
                    intObject(rs, "max_user_count"),
                    intObject(rs, "max_warehouse_count"),
                    rs.getBigDecimal("max_storage_gb"),
                    rs.getString("feature_flags"),
                    rs.getInt("sort_no"),
                    rs.getString("remark"),
                    rs.getString("created_by"),
                    offset(rs, "created_at"),
                    rs.getString("updated_by"),
                    offset(rs, "updated_at"),
                    rs.getString("deleted_by"),
                    rs.getString("delete_reason"),
                    offset(rs, "deleted_at")
            );
        }

        private Integer intObject(ResultSet rs, String column) throws SQLException {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        }

        private OffsetDateTime offset(ResultSet rs, String column) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(column);
            return timestamp == null ? null : OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
        }
    }
}
