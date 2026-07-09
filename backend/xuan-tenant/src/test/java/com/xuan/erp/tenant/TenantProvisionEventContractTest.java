package com.xuan.erp.tenant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantProvisionEventContractTest {

    private static final Path TENANT_EVENTS_DOC = Path.of(
            "..", "..", "docs-site", "src", "content", "docs", "backend", "services", "xuan-tenant", "events.md");
    private static final Path TENANT_API_DOC = Path.of(
            "..", "..", "docs-site", "src", "content", "docs", "backend", "services", "xuan-tenant", "api.md");
    private static final Path TENANT_PERMISSIONS_DOC = Path.of(
            "..", "..", "docs-site", "src", "content", "docs", "backend", "services", "xuan-tenant", "permissions.md");

    @Test
    void tenantProvisionDocsLockAsyncIamBootstrapContract() throws IOException {
        String events = Files.readString(TENANT_EVENTS_DOC);
        String api = Files.readString(TENANT_API_DOC);
        String permissions = Files.readString(TENANT_PERMISSIONS_DOC);
        String postTenantsRow = findTableRow(api, "| POST | /api/tenants |");
        String provisionManageRow = findTableRow(permissions, "| `tenant-provision:manage` |");
        String completedRow = findTableRow(events, "| IAM | TenantIamProvisionStepCompleted |");
        String provisionedRow = findTableRow(events, "| TenantProvisioned |");

        assertAll(
                () -> assertTrue(events.contains("| TenantProvisioningStarted |")),
                () -> assertTrue(events.contains("| TenantIamBootstrapRequested |")),
                () -> assertTrue(events.contains("| TenantProvisioned |")),
                () -> assertTrue(events.contains("`eventId`、`tenantId`、`occurredAt`、`traceId`、`sourceService`")),
                () -> assertTrue(events.contains("`provisionStep`")),
                () -> assertTrue(events.contains("`IAM_BOOTSTRAP`")),
                () -> assertFalse(events.contains("TenantProvisionStepCompleted")),
                () -> assertFalse(events.contains("TenantProvisionStepFailed")),
                () -> assertFalse(events.contains("TenantServiceProvisionStepCompleted")),
                () -> assertFalse(events.contains("TenantServiceProvisionStepFailed")),
                () -> assertTrue(postTenantsRow.contains("异步")),
                () -> assertTrue(postTenantsRow.contains("`PROVISIONING`")),
                () -> assertTrue(postTenantsRow.contains("`IAM_BOOTSTRAP`")),
                () -> assertTrue(permissions.contains("`tenant-provision:view`")),
                () -> assertTrue(permissions.contains("`tenant-provision:manage`")),
                () -> assertTrue(provisionManageRow.contains("重试")),
                () -> assertTrue(provisionManageRow.contains("补偿")),
                () -> assertTrue(provisionManageRow.contains("运维")),
                () -> assertFalse(provisionManageRow.contains("用于发起")),
                () -> assertFalse(provisionManageRow.contains("负责发起")),
                () -> assertFalse(provisionManageRow.contains("创建租户并异步启动")),
                () -> assertTrue(completedRow.contains("编排成功回执")),
                () -> assertTrue(completedRow.contains("唯一权威成功回执")),
                () -> assertFalse(provisionedRow.contains("IamTenantBootstrapped"))
        );
    }

    private static String findTableRow(String content, String marker) {
        return Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(line -> line.startsWith(marker))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到表格行: " + marker));
    }
}
