package com.xuan.erp.tenant.infrastructure.persistence.repository;

import com.xuan.erp.tenant.domain.repository.TenantResourceRepository;
import com.xuan.erp.tenant.domain.model.resource.TenantResourceDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTenantResourceRepository implements TenantResourceRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcTenantResourceRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> list(TenantResourceDefinition resource) {
        String sql = "SELECT * FROM " + resource.tableName() + activeWhere(resource) + " ORDER BY id";
        return jdbcTemplate.queryForList(sql, Map.of());
    }

    @Override
    public Optional<Map<String, Object>> findById(TenantResourceDefinition resource, Long id) {
        String sql = "SELECT * FROM " + resource.tableName() + " WHERE id = :id" + activeAnd(resource);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, Map.of("id", id));
        return rows.stream().findFirst();
    }

    @Override
    public Map<String, Object> create(TenantResourceDefinition resource, Map<String, Object> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("create values must not be empty");
        }
        MapSqlParameterSource params = new MapSqlParameterSource(values);
        StringJoiner columns = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");
        values.keySet().forEach(column -> {
            columns.add(column);
            placeholders.add(":" + column);
        });
        String sql = "INSERT INTO " + resource.tableName() + " (" + columns + ") VALUES (" + placeholders + ") RETURNING *";
        return jdbcTemplate.queryForMap(sql, params);
    }

    @Override
    public Map<String, Object> update(TenantResourceDefinition resource, Long id, Map<String, Object> values) {
        if (values.isEmpty()) {
            return findById(resource, id).orElse(Map.of());
        }
        Map<String, Object> paramsMap = new LinkedHashMap<>(values);
        paramsMap.put("id", id);
        MapSqlParameterSource params = new MapSqlParameterSource(paramsMap);
        StringJoiner assignments = new StringJoiner(", ");
        values.keySet().forEach(column -> assignments.add(column + " = :" + column));
        String sql = "UPDATE " + resource.tableName() + " SET " + assignments + " WHERE id = :id RETURNING *";
        return jdbcTemplate.queryForMap(sql, params);
    }

    @Override
    public void softDelete(TenantResourceDefinition resource, Long id, String reason, String operator) {
        jdbcTemplate.update("""
                UPDATE %s
                SET deleted_at = now(),
                    deleted_by = :operator,
                    delete_reason = :reason
                WHERE id = :id
                  AND deleted_at IS NULL
                """.formatted(resource.tableName()), Map.of("id", id, "reason", reason, "operator", operator));
    }

    private String activeWhere(TenantResourceDefinition resource) {
        return resource.supportsSoftDelete() ? " WHERE deleted_at IS NULL" : "";
    }

    private String activeAnd(TenantResourceDefinition resource) {
        return resource.supportsSoftDelete() ? " AND deleted_at IS NULL" : "";
    }
}
