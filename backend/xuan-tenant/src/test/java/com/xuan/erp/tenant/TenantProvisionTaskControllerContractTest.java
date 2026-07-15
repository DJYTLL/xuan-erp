package com.xuan.erp.tenant;

import com.xuan.erp.tenant.interfaces.controller.TenantProvisionTaskController;
import com.xuan.erp.tenant.interfaces.controller.TenantProvisionCallbackController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantProvisionTaskControllerContractTest {

    @Test
    void listRouteUsesTenantSubresourceAndViewPermission() throws Exception {
        RequestMapping mapping = TenantProvisionTaskController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/api", mapping.value()[0]);

        Method listTasks = TenantProvisionTaskController.class.getDeclaredMethod("listTasks", Long.class);
        GetMapping getMapping = listTasks.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
        assertEquals("/tenants/{tenantId}/provision-tasks", getMapping.value()[0]);
        assertEquals("tenantId", listTasks.getParameters()[0].getAnnotation(PathVariable.class).value());
        assertEquals("@xuanPermission.has('tenant-provision:view')", permission(listTasks));
    }

    @Test
    void retryRouteUsesManagePermission() throws Exception {
        Method retryTask = TenantProvisionTaskController.class.getDeclaredMethod(
                "retryTask",
                Long.class,
                com.xuan.erp.tenant.interfaces.dto.RetryTenantProvisionTaskRequest.class
        );
        PostMapping postMapping = retryTask.getAnnotation(PostMapping.class);
        assertNotNull(postMapping);
        assertEquals("/tenant-provision-tasks/{taskId}/retry", postMapping.value()[0]);
        assertEquals("taskId", retryTask.getParameters()[0].getAnnotation(PathVariable.class).value());
        assertEquals("@xuanPermission.has('tenant-provision:manage')", permission(retryTask));
    }

    @Test
    void internalProvisionCallbackRouteUsesDedicatedCallbackPermission() throws Exception {
        RequestMapping mapping = TenantProvisionCallbackController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/internal", mapping.value()[0]);

        Method handleCallback = TenantProvisionCallbackController.class.getDeclaredMethod(
                "handleCallback",
                Long.class,
                com.xuan.erp.tenant.interfaces.dto.TenantProvisionCallbackRequest.class
        );
        PostMapping postMapping = handleCallback.getAnnotation(PostMapping.class);
        assertNotNull(postMapping);
        assertEquals("/tenants/{tenantId}/provision-callbacks", postMapping.value()[0]);
        assertEquals("tenantId", handleCallback.getParameters()[0].getAnnotation(PathVariable.class).value());
        assertEquals("@xuanPermission.has('tenant-provision:callback')", permission(handleCallback));
    }

    private static String permission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, method.getName() + " 缺少 @PreAuthorize");
        return preAuthorize.value();
    }
}
