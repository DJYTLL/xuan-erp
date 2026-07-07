package com.xuan.erp.tenant.infrastructure.persistence.repository;

import com.xuan.erp.tenant.domain.model.Tenant;
import com.xuan.erp.tenant.domain.repository.TenantRepository;
import com.xuan.erp.tenant.domain.model.type.TenantStatus;
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
public class TenantRepositoryAdapter implements TenantRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TenantRepositoryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        List<Tenant> tenants = jdbcTemplate.query("""
                SELECT *
                FROM tenant
                WHERE id = :id
                  AND deleted_at IS NULL
                """, Map.of("id", id), new TenantRowMapper());
        return tenants.stream().findFirst();
    }

    @Override
    public Optional<Tenant> findActiveByNormalizedCode(String normalizedCode) {
        List<Tenant> tenants = jdbcTemplate.query("""
                SELECT *
                FROM tenant
                WHERE normalized_code = :normalizedCode
                  AND deleted_at IS NULL
                ORDER BY id
                LIMIT 1
                """, Map.of("normalizedCode", normalizedCode), new TenantRowMapper());
        return tenants.stream().findFirst();
    }

    @Override
    public List<Tenant> findActiveTenants() {
        return jdbcTemplate.query("""
                SELECT *
                FROM tenant
                WHERE deleted_at IS NULL
                ORDER BY id
                """, Map.of(), new TenantRowMapper());
    }

    @Override
    public Tenant save(Tenant tenant) {
        if (tenant.id() == null) {
            return insert(tenant);
        }
        update(tenant);
        return findByIdIncludingDeleted(tenant.id()).orElse(tenant);
    }

    private Tenant insert(Tenant tenant) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = toParams(tenant);
        jdbcTemplate.update("""
                INSERT INTO tenant (
                    code, normalized_code, name, status, contact_name, contact_phone,
                    provisioned_at, enabled_at, disabled_at, disabled_reason, remark,
                    created_by, created_at, updated_by, updated_at, deleted_by, delete_reason, deleted_at
                ) VALUES (
                    :code, :normalizedCode, :name, :status, :contactName, :contactPhone,
                    :provisionedAt, :enabledAt, :disabledAt, :disabledReason, :remark,
                    :createdBy, :createdAt, :updatedBy, :updatedAt, :deletedBy, :deleteReason, :deletedAt
                )
                """, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            return tenant;
        }
        return findByIdIncludingDeleted(key.longValue()).orElse(tenant);
    }

    private void update(Tenant tenant) {
        jdbcTemplate.update("""
                UPDATE tenant
                SET code = :code,
                    normalized_code = :normalizedCode,
                    name = :name,
                    status = :status,
                    contact_name = :contactName,
                    contact_phone = :contactPhone,
                    provisioned_at = :provisionedAt,
                    enabled_at = :enabledAt,
                    disabled_at = :disabledAt,
                    disabled_reason = :disabledReason,
                    remark = :remark,
                    created_by = :createdBy,
                    created_at = :createdAt,
                    updated_by = :updatedBy,
                    updated_at = :updatedAt,
                    deleted_by = :deletedBy,
                    delete_reason = :deleteReason,
                    deleted_at = :deletedAt
                WHERE id = :id
                """, toParams(tenant));
    }

    private Optional<Tenant> findByIdIncludingDeleted(Long id) {
        List<Tenant> tenants = jdbcTemplate.query("""
                SELECT *
                FROM tenant
                WHERE id = :id
                """, Map.of("id", id), new TenantRowMapper());
        return tenants.stream().findFirst();
    }

    private MapSqlParameterSource toParams(Tenant tenant) {
        return new MapSqlParameterSource()
                .addValue("id", tenant.id())
                .addValue("code", tenant.code())
                .addValue("normalizedCode", tenant.normalizedCode())
                .addValue("name", tenant.name())
                .addValue("status", tenant.status().code())
                .addValue("contactName", tenant.contactName())
                .addValue("contactPhone", tenant.contactPhone())
                .addValue("provisionedAt", toTimestamp(tenant.provisionedAt()))
                .addValue("enabledAt", toTimestamp(tenant.enabledAt()))
                .addValue("disabledAt", toTimestamp(tenant.disabledAt()))
                .addValue("disabledReason", tenant.disabledReason())
                .addValue("remark", tenant.remark())
                .addValue("createdBy", tenant.createdBy())
                .addValue("createdAt", toTimestamp(tenant.createdAt()))
                .addValue("updatedBy", tenant.updatedBy())
                .addValue("updatedAt", toTimestamp(tenant.updatedAt()))
                .addValue("deletedBy", tenant.deletedBy())
                .addValue("deleteReason", tenant.deleteReason())
                .addValue("deletedAt", toTimestamp(tenant.deletedAt()));
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static final class TenantRowMapper implements RowMapper<Tenant> {

        @Override
        public Tenant mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Tenant(
                    rs.getLong("id"),
                    rs.getString("code"),
                    rs.getString("normalized_code"),
                    rs.getString("name"),
                    TenantStatus.fromCode(rs.getString("status")),
                    rs.getString("contact_name"),
                    rs.getString("contact_phone"),
                    offset(rs, "provisioned_at"),
                    offset(rs, "enabled_at"),
                    offset(rs, "disabled_at"),
                    rs.getString("disabled_reason"),
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

        private OffsetDateTime offset(ResultSet rs, String column) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(column);
            return timestamp == null ? null : OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
        }
    }
}
