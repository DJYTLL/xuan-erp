package com.xuan.erp.tenant;

import com.xuan.erp.tenant.interfaces.controller.TenantController;
import com.xuan.erp.tenant.interfaces.controller.TenantConfigController;
import com.xuan.erp.tenant.interfaces.controller.TenantContactController;
import com.xuan.erp.tenant.interfaces.controller.TenantDomainController;
import com.xuan.erp.tenant.interfaces.controller.TenantInternalStatusController;
import com.xuan.erp.tenant.interfaces.controller.TenantOutboxEventController;
import com.xuan.erp.tenant.interfaces.controller.TenantPlanAssignmentController;
import com.xuan.erp.tenant.interfaces.controller.TenantPlanController;
import com.xuan.erp.tenant.interfaces.controller.TenantProvisionTaskController;
import com.xuan.erp.tenant.interfaces.controller.TenantResourceController;
import com.xuan.erp.tenant.interfaces.controller.TenantScopedConfigController;
import com.xuan.erp.tenant.infrastructure.config.TenantSecurityConfiguration;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantControllerContractTest {

    @Test
    void everyExternalTenantApiHandlerHasMethodLevelAuthorization() {
        List<Class<?>> externalApiControllers = List.of(
                TenantController.class,
                TenantConfigController.class,
                TenantContactController.class,
                TenantDomainController.class,
                TenantOutboxEventController.class,
                TenantPlanAssignmentController.class,
                TenantPlanController.class,
                TenantProvisionTaskController.class,
                TenantResourceController.class,
                TenantScopedConfigController.class
        );

        for (Class<?> controller : externalApiControllers) {
            RequestMapping requestMapping = controller.getAnnotation(RequestMapping.class);
            assertNotNull(requestMapping, controller.getName() + " 缺少类级 @RequestMapping");
            org.junit.jupiter.api.Assertions.assertTrue(
                    requestMapping.value()[0].startsWith("/api"),
                    controller.getName() + " 不是外部 /api Controller，不能放进本测试清单");

            for (Method method : controller.getDeclaredMethods()) {
                if (isHandlerMethod(method)) {
                    assertNotNull(method.getAnnotation(PreAuthorize.class),
                            controller.getSimpleName() + "#" + method.getName() + " 缺少方法级 @PreAuthorize");
                }
            }
        }
    }

    @Test
    void tenantControllerMethodsExposeExpectedPermissionCodes() throws Exception {
        assertEquals("@xuanPermission.has('tenant:view')", permission(TenantController.class.getDeclaredMethod("listTenants", long.class, long.class)));
        assertEquals("@xuanPermission.hasAny('tenant:view', 'iam-column-permission:view', 'iam-role-column-permission:view')",
                permission(TenantController.class.getDeclaredMethod("listColumnPermissionTenants", long.class, long.class)));
        assertEquals("@xuanPermission.has('tenant:view')", permission(TenantController.class.getDeclaredMethod("getTenant", Long.class)));
        assertEquals("@xuanPermission.has('tenant:create')", permission(TenantController.class.getDeclaredMethod("createTenant", com.xuan.erp.tenant.interfaces.dto.CreateTenantRequest.class)));
        assertEquals("@xuanPermission.has('tenant:update')", permission(TenantController.class.getDeclaredMethod("updateTenant", Long.class, com.xuan.erp.tenant.interfaces.dto.UpdateTenantRequest.class)));
        assertEquals("@xuanPermission.has('tenant:enable')", permission(TenantController.class.getDeclaredMethod("enableTenant", Long.class, com.xuan.erp.tenant.interfaces.dto.ChangeTenantStatusRequest.class)));
        assertEquals("@xuanPermission.has('tenant:disable')", permission(TenantController.class.getDeclaredMethod("disableTenant", Long.class, com.xuan.erp.tenant.interfaces.dto.ChangeTenantStatusRequest.class)));
        assertEquals("@xuanPermission.has('tenant:delete')", permission(TenantController.class.getDeclaredMethod("deleteTenant", Long.class, com.xuan.erp.tenant.interfaces.dto.DeleteRequest.class)));
        assertEquals("@xuanPermission.has('tenant-plan:assign')", permission(TenantController.class.getDeclaredMethod("repairTenantPermissionSync", Long.class)));
    }

    @Test
    void tenantConfigControllerMethodsExposeExpectedPermissionCodes() throws Exception {
        assertEquals("@xuanPermission.has('tenant-config:view')", permission(TenantConfigController.class.getDeclaredMethod("listConfigs", Long.class, long.class, long.class)));
        assertEquals("@xuanPermission.has('tenant-config:view')", permission(TenantConfigController.class.getDeclaredMethod("listPublicConfigs", Long.class)));
        assertEquals("@xuanPermission.has('tenant-config:view')", permission(TenantConfigController.class.getDeclaredMethod("getConfig", Long.class)));
        assertEquals("@xuanPermission.has('tenant-config:manage')", permission(TenantConfigController.class.getDeclaredMethod("createConfig", com.xuan.erp.tenant.interfaces.dto.TenantConfigRequest.class)));
        assertEquals("@xuanPermission.has('tenant-config:manage')", permission(TenantConfigController.class.getDeclaredMethod("updateConfig", Long.class, com.xuan.erp.tenant.interfaces.dto.TenantConfigRequest.class)));
        assertEquals("@xuanPermission.has('tenant-config:manage')", permission(TenantConfigController.class.getDeclaredMethod("deleteConfig", Long.class, com.xuan.erp.tenant.interfaces.dto.DeleteRequest.class)));
    }

    @Test
    void tenantPlanControllerMethodsExposeExpectedPermissionCodes() throws Exception {
        assertEquals("@xuanPermission.has('tenant-plan:view')", permission(TenantPlanController.class.getDeclaredMethod("listPlans")));
        assertEquals("@xuanPermission.has('tenant-plan:view')", permission(TenantPlanController.class.getDeclaredMethod("getPlan", Long.class)));
        assertEquals("@xuanPermission.has('tenant-plan:manage')", permission(TenantPlanController.class.getDeclaredMethod("createPlan", com.xuan.erp.tenant.interfaces.dto.CreateTenantPlanRequest.class)));
        assertEquals("@xuanPermission.has('tenant-plan:manage')", permission(TenantPlanController.class.getDeclaredMethod("updatePlan", Long.class, com.xuan.erp.tenant.interfaces.dto.UpdateTenantPlanRequest.class)));
        assertEquals("@xuanPermission.has('tenant-plan:manage')", permission(TenantPlanController.class.getDeclaredMethod("enablePlan", Long.class, com.xuan.erp.tenant.interfaces.dto.ChangeTenantPlanStatusRequest.class)));
        assertEquals("@xuanPermission.has('tenant-plan:manage')", permission(TenantPlanController.class.getDeclaredMethod("disablePlan", Long.class, com.xuan.erp.tenant.interfaces.dto.ChangeTenantPlanStatusRequest.class)));
        assertEquals("@xuanPermission.has('tenant-plan:manage')", permission(TenantPlanController.class.getDeclaredMethod("deletePlan", Long.class, com.xuan.erp.tenant.interfaces.dto.DeleteRequest.class)));
    }

    @Test
    void tenantPlanAssignmentControllerMethodsExposeExpectedPermissionCodes() throws Exception {
        assertEquals("@xuanPermission.has('tenant-plan:view')", permission(TenantPlanAssignmentController.class.getDeclaredMethod("listAssignments")));
        assertEquals("@xuanPermission.has('tenant-plan:view')", permission(TenantPlanAssignmentController.class.getDeclaredMethod("getAssignment", Long.class)));
        assertEquals("@xuanPermission.has('tenant-plan:assign')", permission(TenantPlanAssignmentController.class.getDeclaredMethod("createAssignment", com.xuan.erp.tenant.interfaces.dto.TenantPlanAssignmentRequest.class)));
        assertEquals("@xuanPermission.has('tenant-plan:assign')", permission(TenantPlanAssignmentController.class.getDeclaredMethod("updateAssignment", Long.class, com.xuan.erp.tenant.interfaces.dto.TenantPlanAssignmentRequest.class)));
        assertEquals("@xuanPermission.has('tenant-plan:assign')", permission(TenantPlanAssignmentController.class.getDeclaredMethod("deleteAssignment", Long.class, com.xuan.erp.tenant.interfaces.dto.DeleteRequest.class)));
    }

    @Test
    void tenantDomainControllerMethodsExposeExpectedPermissionCodes() throws Exception {
        assertEquals("@xuanPermission.has('tenant-domain:view')", permission(TenantDomainController.class.getDeclaredMethod("listDomains")));
        assertEquals("@xuanPermission.has('tenant-domain:view')", permission(TenantDomainController.class.getDeclaredMethod("getDomain", Long.class)));
        assertEquals("@xuanPermission.has('tenant-domain:manage')", permission(TenantDomainController.class.getDeclaredMethod("createDomain", com.xuan.erp.tenant.interfaces.dto.TenantDomainRequest.class)));
        assertEquals("@xuanPermission.has('tenant-domain:manage')", permission(TenantDomainController.class.getDeclaredMethod("updateDomain", Long.class, com.xuan.erp.tenant.interfaces.dto.TenantDomainRequest.class)));
        assertEquals("@xuanPermission.has('tenant-domain:manage')", permission(TenantDomainController.class.getDeclaredMethod("deleteDomain", Long.class, com.xuan.erp.tenant.interfaces.dto.DeleteRequest.class)));
    }

    @Test
    void tenantContactControllerMethodsExposeExpectedPermissionCodes() throws Exception {
        assertEquals("@xuanPermission.has('tenant-contact:view')", permission(TenantContactController.class.getDeclaredMethod("listContacts")));
        assertEquals("@xuanPermission.has('tenant-contact:view')", permission(TenantContactController.class.getDeclaredMethod("getContact", Long.class)));
        assertEquals("@xuanPermission.has('tenant-contact:manage')", permission(TenantContactController.class.getDeclaredMethod("createContact", com.xuan.erp.tenant.interfaces.dto.TenantContactRequest.class)));
        assertEquals("@xuanPermission.has('tenant-contact:manage')", permission(TenantContactController.class.getDeclaredMethod("updateContact", Long.class, com.xuan.erp.tenant.interfaces.dto.TenantContactRequest.class)));
        assertEquals("@xuanPermission.has('tenant-contact:manage')", permission(TenantContactController.class.getDeclaredMethod("deleteContact", Long.class, com.xuan.erp.tenant.interfaces.dto.DeleteRequest.class)));
    }

    @Test
    void tenantResourceControllerMethodsExposeExpectedPermissionGuards() throws Exception {
        assertEquals("@tenantResourcePermissionGuard.canRead(#resourceName)",
                permission(TenantResourceController.class.getDeclaredMethod("list", String.class)));
        assertEquals("@tenantResourcePermissionGuard.canRead(#resourceName)",
                permission(TenantResourceController.class.getDeclaredMethod("get", String.class, Long.class)));
        assertEquals("@tenantResourcePermissionGuard.canCreate(#resourceName)",
                permission(TenantResourceController.class.getDeclaredMethod("create", String.class, java.util.Map.class)));
        assertEquals("@tenantResourcePermissionGuard.canUpdate(#resourceName)",
                permission(TenantResourceController.class.getDeclaredMethod("update", String.class, Long.class, java.util.Map.class)));
        assertEquals("@tenantResourcePermissionGuard.canDelete(#resourceName)",
                permission(TenantResourceController.class.getDeclaredMethod("delete", String.class, Long.class, com.xuan.erp.tenant.interfaces.dto.DeleteRequest.class)));
    }

    @Test
    void scopedConfigControllerUsesTenantSubresourceRouteAndExpectedPermissions() throws Exception {
        RequestMapping mapping = TenantScopedConfigController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/api/tenants/{tenantId}/configs", mapping.value()[0]);

        Method listConfigs = TenantScopedConfigController.class.getDeclaredMethod("listConfigs", Long.class, long.class, long.class);
        GetMapping getMapping = listConfigs.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
        assertEquals("@xuanPermission.has('tenant-config:view')", permission(listConfigs));

        Method updateConfig = TenantScopedConfigController.class.getDeclaredMethod(
                "updateConfig",
                Long.class,
                String.class,
                com.xuan.erp.tenant.interfaces.dto.UpdateTenantScopedConfigRequest.class
        );
        PutMapping putMapping = updateConfig.getAnnotation(PutMapping.class);
        assertNotNull(putMapping);
        assertEquals("/{configKey}", putMapping.value()[0]);
        assertEquals("tenantId", updateConfig.getParameters()[0].getAnnotation(PathVariable.class).value());
        assertEquals("configKey", updateConfig.getParameters()[1].getAnnotation(PathVariable.class).value());
        assertEquals("@xuanPermission.has('tenant-config:manage')", permission(updateConfig));
    }

    @Test
    void internalStatusControllerExposesNarrowTenantStatusRouteForIam() throws Exception {
        RequestMapping mapping = TenantInternalStatusController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/internal/tenants", mapping.value()[0]);

        Method getStatus = TenantInternalStatusController.class.getDeclaredMethod("getTenantStatus", Long.class);
        GetMapping getMapping = getStatus.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
        assertEquals("/{tenantId}/status", getMapping.value()[0]);
        assertEquals("tenantId", getStatus.getParameters()[0].getAnnotation(PathVariable.class).value());
        assertEquals(null, getStatus.getAnnotation(PreAuthorize.class));

        Method getStatusByCode = TenantInternalStatusController.class.getDeclaredMethod("getTenantStatusByCode", String.class);
        GetMapping getByCodeMapping = getStatusByCode.getAnnotation(GetMapping.class);
        assertNotNull(getByCodeMapping);
        assertEquals("/by-code/{tenantCode}/status", getByCodeMapping.value()[0]);
        assertEquals("tenantCode", getStatusByCode.getParameters()[0].getAnnotation(PathVariable.class).value());
        assertEquals(null, getStatusByCode.getAnnotation(PreAuthorize.class));
    }

    @Test
    void tenantRuntimeEnablesIamPermissionSnapshotChecks() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/application.yml"));

        assertNotNull(config);
        org.junit.jupiter.api.Assertions.assertTrue(config.contains("permission:"));
        org.junit.jupiter.api.Assertions.assertTrue(config.contains("enabled: ${XUAN_SECURITY_PERMISSION_ENABLED:true}"));
        org.junit.jupiter.api.Assertions.assertTrue(config.contains("iam-service-name: ${XUAN_SECURITY_PERMISSION_IAM_SERVICE_NAME:xuan-iam}"));
        org.junit.jupiter.api.Assertions.assertTrue(config.contains("iam-snapshot-path: ${XUAN_SECURITY_PERMISSION_IAM_SNAPSHOT_PATH:/api/iam/permissions/current}"));
        org.junit.jupiter.api.Assertions.assertFalse(config.contains("http://127.0.0.1:8101/api/iam/permissions/current"));
        org.junit.jupiter.api.Assertions.assertFalse(config.contains("http://xuan-iam:8101/api/iam/permissions/current"));
    }

    @Test
    void tenantRuntimeEnablesIamJwkDiscoveryChecks() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/application.yml"));

        assertNotNull(config);
        org.junit.jupiter.api.Assertions.assertTrue(config.contains("jwt:"));
        org.junit.jupiter.api.Assertions.assertTrue(config.contains("iam-service-name: ${XUAN_SECURITY_JWT_IAM_SERVICE_NAME:xuan-iam}"));
        org.junit.jupiter.api.Assertions.assertTrue(config.contains("jwk-set-path: ${XUAN_SECURITY_JWT_JWK_SET_PATH:/.well-known/jwks.json}"));
        org.junit.jupiter.api.Assertions.assertFalse(config.contains("http://127.0.0.1:8101/.well-known/jwks.json"));
        org.junit.jupiter.api.Assertions.assertFalse(config.contains("http://xuan-iam:8101/.well-known/jwks.json"));
    }

    @Test
    void tenantSecurityConfigurationEnablesMethodSecurity() {
        assertNotNull(TenantSecurityConfiguration.class.getAnnotation(EnableMethodSecurity.class));
    }

    @Test
    void tenantSecurityAllowsIamToResolveTenantStatusByCodeBeforeLogin() throws Exception {
        String securityConfiguration = Files.readString(Path.of(
                "src/main/java/com/xuan/erp/tenant/infrastructure/config/TenantSecurityConfiguration.java"));

        org.junit.jupiter.api.Assertions.assertTrue(securityConfiguration.contains("\"/internal/tenants/*/status\""));
        org.junit.jupiter.api.Assertions.assertTrue(securityConfiguration.contains("\"/internal/tenants/by-code/*/status\""));
    }

    @Test
    void tenantResponseExposesPermissionSyncStatusForFrontend() throws Exception {
        String response = Files.readString(Path.of("src/main/java/com/xuan/erp/tenant/interfaces/dto/TenantResponse.java"));
        String detailView = Files.readString(Path.of("src/main/java/com/xuan/erp/tenant/application/query/TenantDetailView.java"));
        String internalStatus = Files.readString(Path.of("src/main/java/com/xuan/erp/tenant/application/query/TenantInternalStatusView.java"));

        org.junit.jupiter.api.Assertions.assertTrue(response.contains("String permissionSyncStatus"));
        org.junit.jupiter.api.Assertions.assertTrue(response.contains("String permissionSyncStatusLabel"));
        org.junit.jupiter.api.Assertions.assertTrue(response.contains("OffsetDateTime permissionSyncLastCheckedAt"));
        org.junit.jupiter.api.Assertions.assertTrue(response.contains("OffsetDateTime permissionSyncLastSyncedAt"));
        org.junit.jupiter.api.Assertions.assertTrue(response.contains("String permissionSyncLastErrorMessage"));
        org.junit.jupiter.api.Assertions.assertTrue(detailView.contains("String permissionSyncExpectedHash"));
        org.junit.jupiter.api.Assertions.assertTrue(detailView.contains("String permissionSyncStatus"));
        org.junit.jupiter.api.Assertions.assertTrue(internalStatus.contains("String permissionHash"));
        org.junit.jupiter.api.Assertions.assertTrue(internalStatus.contains("String iamInitTemplateCode"));
        org.junit.jupiter.api.Assertions.assertTrue(internalStatus.contains("List<String> columnPermissionTemplateCodes"));
    }

    @Test
    void tenantSecurityAcceptsDirectBearerTokenAsWellAsGatewayIdentityHeaders() throws Exception {
        String securityConfiguration = Files.readString(Path.of(
                "src/main/java/com/xuan/erp/tenant/infrastructure/config/TenantSecurityConfiguration.java"));

        org.junit.jupiter.api.Assertions.assertTrue(securityConfiguration.contains("BusinessJwtAuthenticationFilter"));
        org.junit.jupiter.api.Assertions.assertTrue(securityConfiguration.contains("ObjectProvider<BusinessJwtAuthenticationFilter>"));
        org.junit.jupiter.api.Assertions.assertTrue(securityConfiguration.contains("addFilterAfter(jwtFilter, SecurityContextHolderFilter.class)"));
        org.junit.jupiter.api.Assertions.assertTrue(securityConfiguration.contains("addFilterAfter(gatewayIdentityAuthenticationFilter, BusinessJwtAuthenticationFilter.class)"));
    }

    private static String permission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, method.getName() + " 缺少 @PreAuthorize");
        return preAuthorize.value();
    }

    private static boolean isHandlerMethod(Method method) {
        return method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
