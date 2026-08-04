package com.xuan.erp.iam;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.BootstrapTenantAdminCommand;
import com.xuan.erp.iam.application.port.IamTenantBootstrapGateway;
import com.xuan.erp.iam.application.service.IamColumnPermissionApplicationService;
import com.xuan.erp.iam.application.service.IamTenantBootstrapApplicationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IamTenantBootstrapApplicationServiceTest {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void delegatesBootstrapToGatewayAndReturnsInsertedMenuCount() {
        IamTenantBootstrapGateway bootstrapGateway = mock(IamTenantBootstrapGateway.class);
        IamColumnPermissionApplicationService columnPermissionApplicationService = mock(IamColumnPermissionApplicationService.class);
        IamTenantBootstrapApplicationService service = new IamTenantBootstrapApplicationService(
                bootstrapGateway,
                passwordEncoder,
                columnPermissionApplicationService);
        when(bootstrapGateway.bootstrapTenant(1001L, "admin", "hashed-password", "租户管理员", null, null, "standard", "tenant-service")).thenReturn(12);

        Integer insertedCount = service.bootstrapTenant(
                1001L,
                new BootstrapTenantAdminCommand("admin", "hashed-password", "租户管理员", null, null, true),
                " standard ",
                List.of(" tenant-basic ", "iam-user-basic", "tenant-basic"),
                " tenant-basic ",
                " tenant-service ");

        assertEquals(12, insertedCount);
        verify(bootstrapGateway).bootstrapTenant(1001L, "admin", "hashed-password", "租户管理员", null, null, "standard", "tenant-service");
        verify(columnPermissionApplicationService).replaceTenantTemplateAssignmentsByCodes(
                1001L,
                List.of("tenant-basic", "iam-user-basic"),
                "tenant-basic",
                "tenant-service");
    }

    @Test
    void defaultsRequesterToTenantProvisionWhenBlank() {
        IamTenantBootstrapGateway bootstrapGateway = mock(IamTenantBootstrapGateway.class);
        IamTenantBootstrapApplicationService service = new IamTenantBootstrapApplicationService(bootstrapGateway, passwordEncoder);
        when(bootstrapGateway.bootstrapTenant(1001L, "admin", "hashed-password", "租户管理员", null, null, null, "tenant-provision")).thenReturn(0);

        Integer insertedCount = service.bootstrapTenant(
                1001L,
                new BootstrapTenantAdminCommand(null, "hashed-password", null, null, null, true),
                " ");

        assertEquals(0, insertedCount);
        verify(bootstrapGateway).bootstrapTenant(1001L, "admin", "hashed-password", "租户管理员", null, null, null, "tenant-provision");
    }

    @Test
    void hashesPlainDefaultAdminPasswordBeforeCallingGateway() {
        IamTenantBootstrapGateway bootstrapGateway = mock(IamTenantBootstrapGateway.class);
        IamTenantBootstrapApplicationService service = new IamTenantBootstrapApplicationService(bootstrapGateway, passwordEncoder);
        when(bootstrapGateway.bootstrapTenant(org.mockito.ArgumentMatchers.eq(1001L),
                org.mockito.ArgumentMatchers.eq("admin"),
                org.mockito.ArgumentMatchers.argThat(hash -> passwordEncoder.matches("123456", hash)),
                org.mockito.ArgumentMatchers.eq("租户管理员"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("tenant-provision"))).thenReturn(1);

        Integer insertedCount = service.bootstrapTenant(1001L, null, null);

        assertEquals(1, insertedCount);
    }

    @Test
    void rejectsEmptyTenantIdBeforeCallingGateway() {
        IamTenantBootstrapGateway bootstrapGateway = mock(IamTenantBootstrapGateway.class);
        IamTenantBootstrapApplicationService service = new IamTenantBootstrapApplicationService(bootstrapGateway, passwordEncoder);

        BusinessException error = assertThrows(BusinessException.class, () -> service.bootstrapTenant(0L, "tenant-service"));

        assertEquals("IAM_INVALID_ARGUMENT", error.code());
        assertTrue(error.getMessage().contains("租户 ID"));
        verifyNoInteractions(bootstrapGateway);
    }
}
