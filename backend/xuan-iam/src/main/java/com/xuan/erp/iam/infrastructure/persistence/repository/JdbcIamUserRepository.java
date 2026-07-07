package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamUser;
import com.xuan.erp.iam.domain.repository.IamUserRepository;
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
/**
 * IAM 用户 JDBC 仓储适配器，负责 iam_user 表的基础读写。
 */
public class JdbcIamUserRepository implements IamUserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcIamUserRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<IamUser> findById(Long id) {
        List<IamUser> users = jdbcTemplate.query("""
                SELECT *
                FROM iam_user
                WHERE id = :id
                  AND deleted_at IS NULL
                """, Map.of("id", id), new IamUserRowMapper());
        return users.stream().findFirst();
    }

    @Override
    public Optional<IamUser> findActiveByTenantIdAndUsername(Long tenantId, String username) {
        List<IamUser> users = jdbcTemplate.query("""
                SELECT *
                FROM iam_user
                WHERE tenant_id = :tenantId
                  AND username = :username
                  AND deleted_at IS NULL
                ORDER BY id
                LIMIT 1
                """, Map.of("tenantId", tenantId, "username", username), new IamUserRowMapper());
        return users.stream().findFirst();
    }

    @Override
    public List<IamUser> findActiveUsers(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT *
                FROM iam_user
                WHERE tenant_id = :tenantId
                  AND deleted_at IS NULL
                ORDER BY id
                """, Map.of("tenantId", tenantId), new IamUserRowMapper());
    }

    @Override
    public IamUser save(IamUser user) {
        if (user.id() == null) {
            return insert(user);
        }
        update(user);
        return findByIdIncludingDeleted(user.id()).orElse(user);
    }

    private IamUser insert(IamUser user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO iam_user (
                    tenant_id, username, password_hash, display_name, email, phone, avatar_url,
                    is_enabled, account_non_expired, account_non_locked, credentials_non_expired,
                    last_login_at, password_changed_at, failed_login_count, last_failed_login_at,
                    locked_until, mfa_enabled, auth_version, remark,
                    created_by, created_at, updated_by, updated_at, deleted_by, delete_reason, deleted_at
                ) VALUES (
                    :tenantId, :username, :passwordHash, :displayName, :email, :phone, :avatarUrl,
                    :enabled, :accountNonExpired, :accountNonLocked, :credentialsNonExpired,
                    :lastLoginAt, :passwordChangedAt, :failedLoginCount, :lastFailedLoginAt,
                    :lockedUntil, :mfaEnabled, :authVersion, :remark,
                    :createdBy, :createdAt, :updatedBy, :updatedAt, :deletedBy, :deleteReason, :deletedAt
                )
                """, toParams(user), keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        return key == null ? user : findByIdIncludingDeleted(key.longValue()).orElse(user);
    }

    private void update(IamUser user) {
        jdbcTemplate.update("""
                UPDATE iam_user
                SET tenant_id = :tenantId,
                    username = :username,
                    password_hash = :passwordHash,
                    display_name = :displayName,
                    email = :email,
                    phone = :phone,
                    avatar_url = :avatarUrl,
                    is_enabled = :enabled,
                    account_non_expired = :accountNonExpired,
                    account_non_locked = :accountNonLocked,
                    credentials_non_expired = :credentialsNonExpired,
                    last_login_at = :lastLoginAt,
                    password_changed_at = :passwordChangedAt,
                    failed_login_count = :failedLoginCount,
                    last_failed_login_at = :lastFailedLoginAt,
                    locked_until = :lockedUntil,
                    mfa_enabled = :mfaEnabled,
                    auth_version = :authVersion,
                    remark = :remark,
                    created_by = :createdBy,
                    created_at = :createdAt,
                    updated_by = :updatedBy,
                    updated_at = :updatedAt,
                    deleted_by = :deletedBy,
                    delete_reason = :deleteReason,
                    deleted_at = :deletedAt
                WHERE id = :id
                """, toParams(user));
    }

    private Optional<IamUser> findByIdIncludingDeleted(Long id) {
        List<IamUser> users = jdbcTemplate.query("""
                SELECT *
                FROM iam_user
                WHERE id = :id
                """, Map.of("id", id), new IamUserRowMapper());
        return users.stream().findFirst();
    }

    private MapSqlParameterSource toParams(IamUser user) {
        return new MapSqlParameterSource()
                .addValue("id", user.id())
                .addValue("tenantId", user.tenantId())
                .addValue("username", user.username())
                .addValue("passwordHash", user.passwordHash())
                .addValue("displayName", user.displayName())
                .addValue("email", user.email())
                .addValue("phone", user.phone())
                .addValue("avatarUrl", user.avatarUrl())
                .addValue("enabled", user.enabled())
                .addValue("accountNonExpired", user.accountNonExpired())
                .addValue("accountNonLocked", user.accountNonLocked())
                .addValue("credentialsNonExpired", user.credentialsNonExpired())
                .addValue("lastLoginAt", toTimestamp(user.lastLoginAt()))
                .addValue("passwordChangedAt", toTimestamp(user.passwordChangedAt()))
                .addValue("failedLoginCount", user.failedLoginCount())
                .addValue("lastFailedLoginAt", toTimestamp(user.lastFailedLoginAt()))
                .addValue("lockedUntil", toTimestamp(user.lockedUntil()))
                .addValue("mfaEnabled", user.mfaEnabled())
                .addValue("authVersion", user.authVersion())
                .addValue("remark", user.remark())
                .addValue("createdBy", user.createdBy())
                .addValue("createdAt", toTimestamp(user.createdAt()))
                .addValue("updatedBy", user.updatedBy())
                .addValue("updatedAt", toTimestamp(user.updatedAt()))
                .addValue("deletedBy", user.deletedBy())
                .addValue("deleteReason", user.deleteReason())
                .addValue("deletedAt", toTimestamp(user.deletedAt()));
    }

    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static final class IamUserRowMapper implements RowMapper<IamUser> {

        @Override
        public IamUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new IamUser(
                    rs.getLong("id"),
                    rs.getLong("tenant_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("display_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("avatar_url"),
                    rs.getBoolean("is_enabled"),
                    rs.getBoolean("account_non_expired"),
                    rs.getBoolean("account_non_locked"),
                    rs.getBoolean("credentials_non_expired"),
                    offset(rs, "last_login_at"),
                    offset(rs, "password_changed_at"),
                    rs.getInt("failed_login_count"),
                    offset(rs, "last_failed_login_at"),
                    offset(rs, "locked_until"),
                    rs.getBoolean("mfa_enabled"),
                    rs.getLong("auth_version"),
                    rs.getString("remark"),
                    rs.getString("created_by"),
                    offset(rs, "created_at"),
                    rs.getString("updated_by"),
                    offset(rs, "updated_at"),
                    rs.getString("deleted_by"),
                    rs.getString("delete_reason"),
                    offset(rs, "deleted_at"));
        }

        private OffsetDateTime offset(ResultSet rs, String column) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(column);
            return timestamp == null ? null : OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
        }
    }
}
