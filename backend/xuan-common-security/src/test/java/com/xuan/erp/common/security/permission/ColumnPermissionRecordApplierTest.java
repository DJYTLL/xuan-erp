package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColumnPermissionRecordApplierTest {

    @Test
    void appliesColumnPermissionsToRecordResponseWithoutReturningRawHiddenValues() {
        PermissionSnapshot snapshot = new PermissionSnapshot(
                7L,
                1001L,
                "tenant-admin",
                Set.of("tenant_admin"),
                Set.of("iam-user:view"),
                Map.of("iam-user", Map.of(
                        "username", ColumnAccess.VISIBLE,
                        "phone", ColumnAccess.MASKED,
                        "email", ColumnAccess.HIDDEN,
                        "authVersion", ColumnAccess.HIDDEN,
                        "status", ColumnAccess.HIDDEN)),
                3L);
        ColumnPermissionRecordApplier applier = new ColumnPermissionRecordApplier();

        DemoUserResponse response = applier.apply(
                snapshot,
                "iam-user",
                new DemoUserResponse("buyer", "13800000000", "buyer@example.com", 9L, Boolean.TRUE),
                List.of(
                        new ColumnPermissionField("username", "username", ColumnMaskType.NONE),
                        new ColumnPermissionField("phone", "phone", ColumnMaskType.PHONE),
                        new ColumnPermissionField("email", "email", ColumnMaskType.EMAIL),
                        new ColumnPermissionField("authVersion", "authVersion", ColumnMaskType.NONE),
                        new ColumnPermissionField("status", "enabled", ColumnMaskType.NONE)));

        assertThat(response.username()).isEqualTo("buyer");
        assertThat(response.phone()).isEqualTo("138****0000");
        assertThat(response.email()).isNull();
        assertThat(response.authVersion()).isNull();
        assertThat(response.enabled()).isNull();
    }

    @Test
    void keepsResponseUnchangedWhenResourceHasNoColumnRules() {
        PermissionSnapshot snapshot = PermissionSnapshot.empty(new CurrentUser(
                7L,
                1001L,
                "tenant-admin",
                Set.of("tenant_admin"),
                3L,
                Set.of("iam-user:view")));
        DemoUserResponse source = new DemoUserResponse("buyer", "13800000000", "buyer@example.com", 9L, Boolean.TRUE);
        ColumnPermissionRecordApplier applier = new ColumnPermissionRecordApplier();

        DemoUserResponse response = applier.apply(
                snapshot,
                "iam-user",
                source,
                List.of(new ColumnPermissionField("phone", "phone", ColumnMaskType.PHONE)));

        assertThat(response).isSameAs(source);
    }

    private record DemoUserResponse(
            String username,
            String phone,
            String email,
            Long authVersion,
            Boolean enabled) {
    }
}
