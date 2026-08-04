package com.xuan.erp.tenant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantMigrationContractTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Test
    void v3SeedsStandardAndFullTenantPlansWithoutDatabaseConflictConstraint() throws IOException {
        Path v3Path = MIGRATION_DIR.resolve("V3__seed_standard_full_tenant_plans.sql");
        assertTrue(Files.exists(v3Path), "必须通过 V3 追加标准版和完整版套餐，不能改写 V1/V2 历史迁移");

        String v3 = Files.readString(v3Path);

        assertTrue(v3.contains("'STANDARD'"));
        assertTrue(v3.contains("'标准版'"));
        assertTrue(v3.contains("'FULL'"));
        assertTrue(v3.contains("'完整版'"));
        assertTrue(v3.contains("\"iamInitTemplateCode\":\"standard\""));
        assertTrue(v3.contains("\"iamInitTemplateCode\":\"full\""));
        assertFalse(v3.contains("ON CONFLICT"), "V3 不能依赖后续 V5 才补充的租户编码唯一索引");
        assertFalse(v3.matches("(?s).*;\\s+updated_by\\s*=.*"), "V3 不能在语句结束后残留 DO UPDATE 赋值片段");
        assertTrue(v3.contains("WHERE NOT EXISTS"));
        assertTrue(v3.contains("UPDATE tenant_plan"));
    }

    @Test
    void v4BackfillsIamTemplateSyncOutboxForActiveTenantPlanAssignments() throws IOException {
        Path v4Path = MIGRATION_DIR.resolve("V4__backfill_iam_template_sync_outbox.sql");
        assertTrue(Files.exists(v4Path), "必须通过 V4 补偿历史租户套餐到 IAM 模板同步事件，不能改写 V1-V3 历史迁移");

        String v4 = Files.readString(v4Path);

        assertTrue(v4.contains("tenant_plan_assignment"));
        assertTrue(v4.contains("tenant_plan"));
        assertTrue(v4.contains("feature_flags ->> 'iamInitTemplateCode'"));
        assertTrue(v4.contains("TenantIamBootstrapRequested"));
        assertTrue(v4.contains("IAM_BOOTSTRAP"));
        assertTrue(v4.contains("tenant-plan-assignment:"));
        assertTrue(v4.contains("callbackRequired', false"));
        assertTrue(v4.contains("sourceService', 'xuan-tenant'"));
        assertTrue(v4.contains("WHERE NOT EXISTS"));
        assertFalse(v4.contains("ON CONFLICT"), "V4 不能依赖后续 V5 才补充的租户编码唯一索引");
    }

    @Test
    void v5AddsUniqueActiveTenantCodeForLoginContract() throws IOException {
        Path v5Path = MIGRATION_DIR.resolve("V5__add_unique_tenant_normalized_code_active.sql");
        assertTrue(Files.exists(v5Path), "必须通过 V5 追加活动租户编码唯一约束，不能改写 V1-V4 历史迁移");

        String v5 = Files.readString(v5Path);

        assertTrue(v5.contains("CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_normalized_code_active"));
        assertTrue(v5.contains("ON tenant (normalized_code)"));
        assertTrue(v5.contains("WHERE deleted_at IS NULL"));
    }

    @Test
    void v7AddsTenantPermissionSyncStateWithoutRewritingHistory() throws IOException {
        Path v7Path = MIGRATION_DIR.resolve("V7__add_tenant_permission_sync_state.sql");
        assertTrue(Files.exists(v7Path), "必须通过 V7 追加租户权限同步状态字段，不能改写 V1-V6 历史迁移");

        String v7 = Files.readString(v7Path);

        assertTrue(v7.contains("ALTER TABLE tenant_plan_assignment"));
        assertTrue(v7.contains("permission_sync_expected_hash"));
        assertTrue(v7.contains("permission_sync_status"));
        assertTrue(v7.contains("permission_sync_last_checked_at"));
        assertTrue(v7.contains("permission_sync_last_synced_at"));
        assertTrue(v7.contains("permission_sync_last_error_code"));
        assertTrue(v7.contains("permission_sync_last_error_message"));
        assertTrue(v7.contains("CHECK (permission_sync_status IN ('SYNCED', 'PENDING_REPAIR', 'REPAIRING', 'FAILED'))"));
        assertTrue(v7.contains("idx_tenant_plan_assignment_permission_sync_status"));
        assertFalse(v7.contains("DROP TABLE"), "V7 只能追加同步状态字段，不能删除历史表");
    }
}
