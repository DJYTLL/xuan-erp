package com.xuan.erp.tenant;

import com.xuan.erp.tenant.interfaces.controller.TenantController;
import com.xuan.erp.tenant.interfaces.dto.ChangeTenantStatusRequest;
import com.xuan.erp.tenant.interfaces.dto.CreateTenantRequest;
import com.xuan.erp.tenant.interfaces.dto.DeleteRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createTenantRequestRequiresCodeAndName() {
        CreateTenantRequest request = new CreateTenantRequest(" ", "", "张三", "13800000000", "备注", "idem-1");

        Set<?> violations = validator.validate(request);

        assertEquals(2, violations.size());
    }

    @Test
    void deleteAndStatusRequestsRequireReasonWhenProvidedByApi() {
        DeleteRequest deleteRequest = new DeleteRequest(" ", "admin", null);
        ChangeTenantStatusRequest statusRequest = new ChangeTenantStatusRequest("", "admin", null);

        assertEquals(1, validator.validate(deleteRequest).size());
        assertEquals(1, validator.validate(statusRequest).size());
    }

    @Test
    void tenantControllerPageParametersMustBePositive() throws Exception {
        TenantController controller = new TenantController(null);
        Method method = TenantController.class.getDeclaredMethod("listTenants", long.class, long.class);
        ExecutableValidator executableValidator = validator.forExecutables();

        Set<?> violations = executableValidator.validateParameters(controller, method, new Object[]{0L, 0L});

        assertTrue(violations.size() >= 2);
    }
}
