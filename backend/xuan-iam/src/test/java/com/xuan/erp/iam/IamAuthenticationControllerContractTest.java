package com.xuan.erp.iam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IamAuthenticationControllerContractTest {

    private static final Path CONTROLLER_PATH = Path.of("src/main/java/com/xuan/erp/iam/interfaces/controller/IamAuthenticationController.java");
    private static final Path SECURITY_CONFIGURATION_PATH = Path.of("src/main/java/com/xuan/erp/iam/infrastructure/config/IamSecurityConfiguration.java");
    private static final Path APPLICATION_PATH = Path.of("src/main/java/com/xuan/erp/iam/XuanIamApplication.java");
    private static final Path API_EXCEPTION_HANDLER_PATH = Path.of("src/main/java/com/xuan/erp/iam/interfaces/advice/IamApiExceptionHandler.java");

    @Test
    void authenticationControllerExposesLogoutEndpoint() throws IOException {
        String controller = Files.readString(CONTROLLER_PATH);

        assertTrue(controller.contains("@PostMapping(\"/api/iam/auth/logout\")"));
        assertTrue(controller.contains("authenticationApplicationService.logout"));
    }

    @Test
    void logoutEndpointCanRevokeRefreshTokenWithoutAccessToken() throws IOException {
        String securityConfiguration = Files.readString(SECURITY_CONFIGURATION_PATH);

        assertTrue(securityConfiguration.contains("\"/api/iam/auth/logout\""));
    }

    @Test
    void refreshEndpointCanRotateRefreshTokenWithoutAccessToken() throws IOException {
        String securityConfiguration = Files.readString(SECURITY_CONFIGURATION_PATH);

        assertTrue(securityConfiguration.contains("\"/api/iam/auth/refresh\""));
    }

    @Test
    void currentTokenEndpointsValidateTenantStatusBeforeReturningSessionData() throws IOException {
        String controller = Files.readString(CONTROLLER_PATH);

        int validationCount = controller
                .split("authenticationApplicationService.validateCurrentTenantStatus\\(currentUser\\);", -1).length - 1;
        assertTrue(validationCount >= 3, "current-user、current-menus、current-permissions 都必须校验租户状态");
    }

    @Test
    void iamSecurityFilterChainRunsBeforeSharedDefaultServletSecurityChain() throws IOException {
        String securityConfiguration = Files.readString(SECURITY_CONFIGURATION_PATH);

        assertTrue(securityConfiguration.contains("import org.springframework.core.Ordered;"));
        assertTrue(securityConfiguration.contains("import org.springframework.core.annotation.Order;"));
        assertTrue(securityConfiguration.contains("@Order(Ordered.HIGHEST_PRECEDENCE)"));
        assertTrue(securityConfiguration.contains("\"/api/iam/auth/login\""));
        assertTrue(securityConfiguration.contains("\"/error\""));
    }

    @Test
    void iamApplicationExcludesSharedDefaultServletSecurityChain() throws IOException {
        String application = Files.readString(APPLICATION_PATH);

        assertTrue(application.contains("XuanServletSecurityAutoConfiguration"));
        assertTrue(application.contains("@SpringBootApplication(exclude = XuanServletSecurityAutoConfiguration.class)"));
    }

    @Test
    void iamBusinessExceptionsUseApiEnvelopeInsteadOfFallingThroughToErrorEndpoint() throws IOException {
        String handler = Files.readString(API_EXCEPTION_HANDLER_PATH);

        assertTrue(handler.contains("@RestControllerAdvice"));
        assertTrue(handler.contains("@ExceptionHandler(BusinessException.class)"));
        assertTrue(handler.contains("@ResponseStatus(HttpStatus.BAD_REQUEST)"));
        assertTrue(handler.contains("ApiResponse.failure(error.code(), error.getMessage())"));
    }
}
