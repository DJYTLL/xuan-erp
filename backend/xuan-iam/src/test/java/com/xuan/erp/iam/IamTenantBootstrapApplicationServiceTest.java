package com.xuan.erp.iam;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.port.IamTenantBootstrapGateway;
import com.xuan.erp.iam.application.service.IamTenantBootstrapApplicationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IamTenantBootstrapApplicationServiceTest {

    @Test
    void delegatesBootstrapToGatewayAndReturnsInsertedMenuCount() {
        IamTenantBootstrapGateway bootstrapGateway = mock(IamTenantBootstrapGateway.class);
        IamTenantBootstrapApplicationService service = new IamTenantBootstrapApplicationService(bootstrapGateway);
        when(bootstrapGateway.bootstrapTenant(1001L, "tenant-service")).thenReturn(12);

        Integer insertedCount = service.bootstrapTenant(1001L, " tenant-service ");

        assertEquals(12, insertedCount);
        verify(bootstrapGateway).bootstrapTenant(1001L, "tenant-service");
    }

    @Test
    void defaultsRequesterToTenantProvisionWhenBlank() {
        IamTenantBootstrapGateway bootstrapGateway = mock(IamTenantBootstrapGateway.class);
        IamTenantBootstrapApplicationService service = new IamTenantBootstrapApplicationService(bootstrapGateway);
        when(bootstrapGateway.bootstrapTenant(1001L, "tenant-provision")).thenReturn(0);

        Integer insertedCount = service.bootstrapTenant(1001L, " ");

        assertEquals(0, insertedCount);
        verify(bootstrapGateway).bootstrapTenant(1001L, "tenant-provision");
    }

    @Test
    void rejectsEmptyTenantIdBeforeCallingGateway() {
        IamTenantBootstrapGateway bootstrapGateway = mock(IamTenantBootstrapGateway.class);
        IamTenantBootstrapApplicationService service = new IamTenantBootstrapApplicationService(bootstrapGateway);

        BusinessException error = assertThrows(BusinessException.class, () -> service.bootstrapTenant(0L, "tenant-service"));

        assertEquals("IAM_INVALID_ARGUMENT", error.code());
        assertTrue(error.getMessage().contains("租户 ID"));
        verifyNoInteractions(bootstrapGateway);
    }
}
