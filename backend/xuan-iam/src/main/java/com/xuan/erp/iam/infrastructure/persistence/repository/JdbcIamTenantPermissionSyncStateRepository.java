package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.repository.IamTenantPermissionSyncStateRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * IAM 租户权限同步状态 JDBC 仓储。
 */
@Repository
public class JdbcIamTenantPermissionSyncStateRepository implements IamTenantPermissionSyncStateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcIamTenantPermissionSyncStateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> findLastSyncedPermissionHash(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT last_synced_permission_hash
                FROM iam_tenant_permission_sync_state
                WHERE tenant_id = :tenantId
                """, Map.of("tenantId", tenantId), (rs, rowNum) -> rs.getString("last_synced_permission_hash"))
                .stream()
                .findFirst();
    }

    @Override
    public void markSynced(Long tenantId, String permissionHash, String source, OffsetDateTime syncedAt) {
        jdbcTemplate.update("""
                INSERT INTO iam_tenant_permission_sync_state (
                    tenant_id, last_synced_permission_hash, last_synced_at, last_sync_source,
                    last_error_code, last_error_message, created_by, created_at, updated_by, updated_at
                ) VALUES (
                    :tenantId, :permissionHash, :syncedAt, :source,
                    NULL, NULL, :source, :syncedAt, :source, :syncedAt
                )
                ON CONFLICT (tenant_id) DO UPDATE
                SET last_synced_permission_hash = EXCLUDED.last_synced_permission_hash,
                    last_synced_at = EXCLUDED.last_synced_at,
                    last_sync_source = EXCLUDED.last_sync_source,
                    last_error_code = NULL,
                    last_error_message = NULL,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, Map.of(
                "tenantId", tenantId,
                "permissionHash", permissionHash,
                "source", source,
                "syncedAt", syncedAt));
    }
}
