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

    @Test
    void v3ExtendsBootstrapWithTenantAdminAndPlatformSuperAdmin() throws IOException {
        Path v3Path = MIGRATION_DIR.resolve("V3__extend_iam_tenant_bootstrap_admin_account.sql");
        assertTrue(Files.exists(v3Path), "必须通过 V3 新增登录账号初始化，不能改写 V1/V2 历史迁移");

        String v3 = Files.readString(v3Path);

        assertTrue(v3.contains("p_admin_username"));
        assertTrue(v3.contains("p_admin_password_hash"));
        assertTrue(v3.contains("'tenant_admin'"));
        assertTrue(v3.contains("'super_admin'"));
        assertTrue(v3.contains("tenant_id, username"));
        assertTrue(v3.contains("iam_authorization_snapshot"));
        assertTrue(v3.contains("PLATFORM_ADMIN"));
        assertTrue(v3.contains("tenant_id = 0"));
    }
}
