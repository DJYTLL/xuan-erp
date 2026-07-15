package com.xuan.erp.tenant;

import com.xuan.erp.tenant.interfaces.controller.TenantOutboxEventController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantOutboxEventControllerContractTest {

    @Test
    void retryRouteUsesManagePermission() throws Exception {
        RequestMapping mapping = TenantOutboxEventController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/api", mapping.value()[0]);

        Method retryEvent = TenantOutboxEventController.class.getDeclaredMethod(
                "retryEvent",
                Long.class,
                com.xuan.erp.tenant.interfaces.dto.RetryTenantOutboxEventRequest.class
        );
        PostMapping postMapping = retryEvent.getAnnotation(PostMapping.class);
        assertNotNull(postMapping);
        assertEquals("/tenant-outbox-events/{eventId}/retry", postMapping.value()[0]);
        assertEquals("eventId", retryEvent.getParameters()[0].getAnnotation(PathVariable.class).value());
        assertEquals("@xuanPermission.has('tenant-provision:manage')", permission(retryEvent));
    }

    private static String permission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, method.getName() + " 缺少 @PreAuthorize");
        return preAuthorize.value();
    }
}
