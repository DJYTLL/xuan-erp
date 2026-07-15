package com.xuan.erp.tenant;

import com.xuan.erp.tenant.interfaces.controller.TenantController;
import com.xuan.erp.tenant.interfaces.controller.TenantConfigController;
import com.xuan.erp.tenant.interfaces.controller.TenantInternalStatusController;
import com.xuan.erp.tenant.interfaces.controller.TenantPlanAssignmentController;
import com.xuan.erp.tenant.interfaces.controller.TenantPlanController;
import com.xuan.erp.tenant.interfaces.controller.TenantScopedConfigController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantControllerContractTest {

    @Test
    void tenantControllerMethodsExposeExpectedPermissionCodes() throws Exception {
        assertEquals("@xuanPermission.has('tenant:view')", permission(TenantController.class.getDeclaredMethod("listTenants", long.class, long.class)));
        assertEquals("@xuanPermission.has('tenant:view')", permission(TenantController.class.getDeclaredMethod("getTenant", Long.class)));
        assertEquals("@xuanPermission.has('tenant:create')", permission(TenantController.class.getDeclaredMethod("createTenant", com.xuan.erp.tenant.interfaces.dto.CreateTenantRequest.class)));
        assertEquals("@xuanPermission.has('tenant:update')", permission(TenantController.class.getDeclaredMethod("updateTenant", Long.class, com.xuan.erp.tenant.interfaces.dto.UpdateTenantRequest.class)));
        assertEquals("@xuanPermission.has('tenant:lifecycle')", permission(TenantController.class.getDeclaredMethod("enableTenant", Long.class, com.xuan.erp.tenant.interfaces.dto.ChangeTenantStatusRequest.class)));
        assertEquals("@xuanPermission.has('tenant:lifecycle')", permission(TenantController.class.getDeclaredMethod("disableTenant", Long.class, com.xuan.erp.tenant.interfaces.dto.ChangeTenantStatusRequest.class)));
        assertEquals("@xuanPermission.has('tenant:delete')", permission(TenantController.class.getDeclaredMethod("deleteTenant", Long.class, com.xuan.erp.tenant.interfaces.dto.DeleteRequest.class)));
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
    }

    private static String permission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, method.getName() + " 缺少 @PreAuthorize");
        return preAuthorize.value();
    }
}
