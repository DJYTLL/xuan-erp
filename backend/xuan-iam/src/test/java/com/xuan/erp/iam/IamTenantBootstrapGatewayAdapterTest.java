package com.xuan.erp.iam;

import com.xuan.erp.iam.infrastructure.persistence.mapper.IamTenantBootstrapMapper;
import com.xuan.erp.iam.infrastructure.persistence.repository.IamTenantBootstrapGatewayAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IamTenantBootstrapGatewayAdapterTest {

    @Test
    void delegatesBootstrapFunctionToMapper() {
        IamTenantBootstrapMapper mapper = mock(IamTenantBootstrapMapper.class);
        IamTenantBootstrapGatewayAdapter gateway = new IamTenantBootstrapGatewayAdapter(mapper);
        when(mapper.bootstrapTenant(1001L, "admin", "hash", "租户管理员", null, null, "standard", "tenant-service")).thenReturn(12);

        Integer insertedCount = gateway.bootstrapTenant(1001L, "admin", "hash", "租户管理员", null, null, "standard", "tenant-service");

        assertEquals(12, insertedCount);
        verify(mapper).bootstrapTenant(1001L, "admin", "hash", "租户管理员", null, null, "standard", "tenant-service");
    }

    @Test
    void returnsZeroWhenMapperReturnsNull() {
        IamTenantBootstrapMapper mapper = mock(IamTenantBootstrapMapper.class);
        IamTenantBootstrapGatewayAdapter gateway = new IamTenantBootstrapGatewayAdapter(mapper);
        when(mapper.bootstrapTenant(1001L, "admin", "hash", "租户管理员", null, null, null, "tenant-service")).thenReturn(null);

        Integer insertedCount = gateway.bootstrapTenant(1001L, "admin", "hash", "租户管理员", null, null, null, "tenant-service");

        assertEquals(0, insertedCount);
    }
}
