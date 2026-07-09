package com.xuan.erp.tenant;

import com.xuan.erp.tenant.interfaces.controller.TenantController;
import com.xuan.erp.tenant.interfaces.controller.TenantConfigController;
import com.xuan.erp.tenant.interfaces.controller.TenantContactController;
import com.xuan.erp.tenant.interfaces.controller.TenantDomainController;
import com.xuan.erp.tenant.interfaces.controller.TenantPlanController;
import com.xuan.erp.tenant.interfaces.controller.TenantPlanAssignmentController;
import com.xuan.erp.tenant.interfaces.controller.TenantResourceController;
import com.xuan.erp.tenant.interfaces.controller.TenantScopedConfigController;
import com.xuan.erp.tenant.interfaces.dto.CreateTenantPlanRequest;
import com.xuan.erp.tenant.interfaces.dto.CreateTenantRequest;
import com.xuan.erp.tenant.interfaces.dto.TenantPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantOpenApiDocumentationTest {

    private static final List<Class<?>> TENANT_CONTROLLERS = List.of(
            TenantController.class,
            TenantPlanController.class,
            TenantResourceController.class,
            TenantPlanAssignmentController.class,
            TenantContactController.class,
            TenantDomainController.class,
            TenantConfigController.class,
            TenantScopedConfigController.class);

    @Test
    void tenantControllersExposeOpenApiTagsAndOperations() throws NoSuchMethodException {
        assertNotNull(TenantController.class.getAnnotation(Tag.class));
        assertNotNull(TenantPlanController.class.getAnnotation(Tag.class));

        Method listTenants = TenantController.class.getDeclaredMethod("listTenants", long.class, long.class);
        Method createPlan = TenantPlanController.class.getDeclaredMethod("createPlan", CreateTenantPlanRequest.class);

        assertNotNull(listTenants.getAnnotation(Operation.class));
        assertNotNull(createPlan.getAnnotation(Operation.class));
    }

    @Test
    void allTenantControllersExposeChineseOpenApiTags() {
        for (Class<?> controller : TENANT_CONTROLLERS) {
            Tag tag = controller.getAnnotation(Tag.class);

            assertNotNull(tag, controller.getSimpleName() + " 缺少 @Tag");
            assertChineseText(tag.name(), controller.getSimpleName() + " 的 @Tag.name 需要使用中文");
            assertFalse(tag.name().contains("controller"), controller.getSimpleName() + " 的 @Tag.name 不应使用默认英文控制器名称");
        }
    }

    @Test
    void allTenantHttpOperationsExposeChineseSummaries() {
        for (Class<?> controller : TENANT_CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!hasHttpMapping(method)) {
                    continue;
                }

                Operation operation = method.getAnnotation(Operation.class);

                assertNotNull(operation, controller.getSimpleName() + "#" + method.getName() + " 缺少 @Operation");
                assertChineseText(operation.summary(), controller.getSimpleName() + "#" + method.getName() + " 的 summary 需要使用中文");
            }
        }
    }

    @Test
    void allTenantPathVariablesDeclareOpenApiVisibleNames() {
        for (Class<?> controller : TENANT_CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                    if (pathVariable == null) {
                        continue;
                    }

                    assertTrue(hasText(pathVariable.value()) || hasText(pathVariable.name()),
                            controller.getSimpleName() + "#" + method.getName() + " 的 @PathVariable 需要显式声明变量名");
                }
            }
        }
    }

    @Test
    void tenantDtosExposeSchemaDescriptions() throws NoSuchFieldException {
        assertNotNull(CreateTenantRequest.class.getDeclaredField("code").getAnnotation(Schema.class));
        assertNotNull(CreateTenantRequest.class.getDeclaredField("adminUsername").getAnnotation(Schema.class));
        assertNotNull(CreateTenantRequest.class.getDeclaredField("adminPassword").getAnnotation(Schema.class));
        assertNotNull(CreateTenantRequest.class.getDeclaredField("adminDisplayName").getAnnotation(Schema.class));
        assertNotNull(CreateTenantRequest.class.getDeclaredField("adminEmail").getAnnotation(Schema.class));
        assertNotNull(CreateTenantRequest.class.getDeclaredField("adminPhone").getAnnotation(Schema.class));
        assertNotNull(TenantPlanResponse.class.getDeclaredField("code").getAnnotation(Schema.class));
    }

    private static boolean hasHttpMapping(Method method) {
        return hasAnnotation(method, GetMapping.class)
                || hasAnnotation(method, PostMapping.class)
                || hasAnnotation(method, PutMapping.class)
                || hasAnnotation(method, DeleteMapping.class);
    }

    private static boolean hasAnnotation(Method method, Class<? extends Annotation> annotationType) {
        return method.getAnnotation(annotationType) != null;
    }

    private static void assertChineseText(String value, String message) {
        assertNotNull(value, message);
        assertTrue(value.matches(".*[\\u4e00-\\u9fa5].*"), message);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
