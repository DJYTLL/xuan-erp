package com.xuan.erp.iam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamProvisionEventContractTest {

    private static final Path IAM_EVENTS_DOC = Path.of(
            "..", "..", "docs-site", "src", "content", "docs", "backend", "services", "xuan-iam", "events.md");

    @Test
    void iamProvisionDocsLockBootstrapEventContract() throws IOException {
        String events = Files.readString(IAM_EVENTS_DOC);
        String bootstrappedRow = findTableRow(events, "| `IamTenantBootstrapped` |");
        String completedRow = findTableRow(events, "| `TenantIamProvisionStepCompleted` |");
        String failedRow = findTableRow(events, "| `TenantIamProvisionStepFailed` |");

        assertAll(
                () -> assertTrue(events.contains("| `IamTenantBootstrapped` |")),
                () -> assertTrue(events.contains("| `TenantIamProvisionStepCompleted` |")),
                () -> assertTrue(events.contains("| `TenantIamProvisionStepFailed` |")),
                () -> assertTrue(events.contains("`eventId`、`tenantId`、`occurredAt`、`traceId`、`sourceService`")),
                () -> assertTrue(events.contains("`provisionStep`")),
                () -> assertTrue(events.contains("`IAM_BOOTSTRAP`")),
                () -> assertTrue(bootstrappedRow.contains("领域事实")),
                () -> assertFalse(bootstrappedRow.contains("编排成功回执")),
                () -> assertTrue(completedRow.contains("编排成功回执")),
                () -> assertFalse(completedRow.contains("领域事实")),
                () -> assertTrue(failedRow.contains("编排失败回执"))
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
