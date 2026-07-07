package com.xuan.erp.iam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamMigrationContractTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Test
    void v1CreatesFifteenBaselineTables() throws IOException {
        String v1 = Files.readString(MIGRATION_DIR.resolve("V1__init_iam_database.sql"));

        long tableCount = v1.lines()
                .filter(line -> line.startsWith("CREATE TABLE IF NOT EXISTS "))
                .count();

        assertEquals(15, tableCount);
    }

    @Test
    void v2AddsIamTenantBootstrappedOutboxContract() throws IOException {
        Path v2Path = MIGRATION_DIR.resolve("V2__add_iam_tenant_bootstrapped_outbox.sql");
        assertTrue(Files.exists(v2Path), "必须通过 V2 新增 IAM outbox，不能改写 V1 历史迁移");

        String v2 = Files.readString(v2Path);

        assertTrue(v2.contains("CREATE TABLE IF NOT EXISTS iam_outbox_event"));
        assertTrue(v2.contains("CREATE OR REPLACE FUNCTION bootstrap_iam_tenant"));
        assertTrue(v2.contains("IamTenantBootstrapped"));
        assertTrue(v2.contains("iam.tenant.bootstrapped"));
        assertTrue(v2.contains("ON CONFLICT (event_id) DO NOTHING"));
    }
}
