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
        assertFalse(v3.contains("ON CONFLICT"), "tenant 库不使用唯一索引，V3 不能依赖 ON CONFLICT");
        assertFalse(v3.matches("(?s).*;\\s+updated_by\\s*=.*"), "V3 不能在语句结束后残留 DO UPDATE 赋值片段");
        assertTrue(v3.contains("WHERE NOT EXISTS"));
        assertTrue(v3.contains("UPDATE tenant_plan"));
    }
}
