package com.xuan.erp.iam;

import com.xuan.erp.iam.infrastructure.persistence.repository.JdbcIamTenantBootstrapGateway;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcIamTenantBootstrapGatewayTest {

    @Test
    void callsDatabaseBootstrapFunctionWithTenantAndRequester() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcIamTenantBootstrapGateway gateway = new JdbcIamTenantBootstrapGateway(jdbcTemplate);
        when(jdbcTemplate.queryForObject(
                eq("SELECT bootstrap_iam_tenant(:tenantId, :requestedBy)"),
                org.mockito.ArgumentMatchers.<Map<String, ?>>any(),
                same(Integer.class))).thenReturn(12);

        Integer insertedCount = gateway.bootstrapTenant(1001L, "tenant-service");

        assertEquals(12, insertedCount);
        ArgumentCaptor<Map<String, ?>> paramsCaptor = mapCaptor();
        verify(jdbcTemplate).queryForObject(
                eq("SELECT bootstrap_iam_tenant(:tenantId, :requestedBy)"),
                paramsCaptor.capture(),
                same(Integer.class));
        assertEquals(1001L, paramsCaptor.getValue().get("tenantId"));
        assertEquals("tenant-service", paramsCaptor.getValue().get("requestedBy"));
    }

    @Test
    void returnsZeroWhenDatabaseFunctionReturnsNull() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcIamTenantBootstrapGateway gateway = new JdbcIamTenantBootstrapGateway(jdbcTemplate);
        when(jdbcTemplate.queryForObject(
                eq("SELECT bootstrap_iam_tenant(:tenantId, :requestedBy)"),
                org.mockito.ArgumentMatchers.<Map<String, ?>>any(),
                same(Integer.class))).thenReturn(null);

        Integer insertedCount = gateway.bootstrapTenant(1001L, "tenant-service");

        assertEquals(0, insertedCount);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Map<String, ?>> mapCaptor() {
        return ArgumentCaptor.forClass((Class) Map.class);
    }
}
