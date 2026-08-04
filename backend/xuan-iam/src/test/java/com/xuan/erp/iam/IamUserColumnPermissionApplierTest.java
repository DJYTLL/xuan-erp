package com.xuan.erp.iam;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.iam.application.query.IamCurrentPermissionSnapshotView;
import com.xuan.erp.iam.application.service.IamCurrentAuthorizationApplicationService;
import com.xuan.erp.iam.interfaces.dto.IamUserResponse;
import com.xuan.erp.iam.interfaces.security.IamUserColumnPermissionApplier;
import java.lang.reflect.Constructor;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class IamUserColumnPermissionApplierTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void marksProductionConstructorAsAutowiredBecauseSpringCannotChooseAmongMultipleConstructors() throws Exception {
        Constructor<IamUserColumnPermissionApplier> constructor = IamUserColumnPermissionApplier.class
                .getConstructor(IamCurrentAuthorizationApplicationService.class);

        assertThat(constructor.isAnnotationPresent(Autowired.class)).isTrue();
    }

    @Test
    void appliesIamUserColumnPermissionsBeforeReturningUserListRows() {
        CurrentUser currentUser = new CurrentUser(1001L, 7L, "tenant-admin", Set.of("tenant_admin"), 8L, Set.of("iam-user:view"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                currentUser,
                "access-token",
                Set.of()));
        IamUserColumnPermissionApplier applier = new IamUserColumnPermissionApplier(
                user -> new IamCurrentPermissionSnapshotView(
                        List.of(),
                        List.of("iam-user:view"),
                        List.of("iam-user:view"),
                        Map.of("iam-user", Map.of(
                                "username", "VISIBLE",
                                "displayName", "HIDDEN",
                                "phone", "MASKED",
                                "email", "HIDDEN",
                                "authVersion", "HIDDEN",
                                "status", "HIDDEN")),
                        Map.of(),
                        List.of(),
                        Map.of(),
                        user.authVersion()));

        IamUserResponse response = applier.applyToUserListRow(row());

        assertThat(response.username()).isEqualTo("buyer");
        assertThat(response.displayName()).isNull();
        assertThat(response.phone()).isEqualTo("138****0000");
        assertThat(response.email()).isNull();
        assertThat(response.authVersion()).isNull();
        assertThat(response.enabled()).isNull();
        assertThat(response.accountNonLocked()).isNull();
    }

    private static IamUserResponse row() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-19T10:00:00Z");
        return new IamUserResponse(
                42L,
                7L,
                "buyer",
                "采购员",
                "buyer@example.com",
                "13800000000",
                true,
                true,
                9L,
                "内部备注",
                now,
                now);
    }
}
