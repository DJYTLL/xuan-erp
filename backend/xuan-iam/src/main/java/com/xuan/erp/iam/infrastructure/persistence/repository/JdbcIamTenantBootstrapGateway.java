package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.application.port.IamTenantBootstrapGateway;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * IAM 租户初始化 JDBC 网关适配器。
 *
 * 这里封装 PostgreSQL 函数调用细节，避免应用服务直接持有 SQL。
 */
@Repository
public class JdbcIamTenantBootstrapGateway implements IamTenantBootstrapGateway {

    /**
     * 调用 PostgreSQL 中的租户 IAM 初始化函数。
     *
     * 返回值表示本次实际新增的租户菜单授权数量。
     */
    private static final String BOOTSTRAP_SQL = "SELECT bootstrap_iam_tenant(:tenantId, :requestedBy)";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcIamTenantBootstrapGateway(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Integer bootstrapTenant(Long tenantId, String requestedBy) {
        Integer insertedCount = jdbcTemplate.queryForObject(
                BOOTSTRAP_SQL,
                Map.of("tenantId", tenantId, "requestedBy", requestedBy),
                Integer.class);
        return insertedCount == null ? 0 : insertedCount;
    }
}
