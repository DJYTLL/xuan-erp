package com.xuan.erp.tenant;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantResilienceConfigurationTest {

    @Test
    void declaresSentinelNacosDatasourceAndSeataClientDependencies() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));

        assertTrue(pom.contains("spring-cloud-starter-alibaba-sentinel"));
        assertTrue(pom.contains("spring-cloud-alibaba-sentinel-datasource"));
        assertTrue(pom.contains("spring-cloud-starter-alibaba-seata"));
    }

    @Test
    void documentsTenantSentinelAndSeataNacosConfiguration() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/application.yml"));

        assertTrue(config.contains("sentinel:"));
        assertTrue(config.contains("dashboard: ${XUAN_SENTINEL_DASHBOARD:127.0.0.1:9022}"));
        assertTrue(config.contains("client-ip: ${XUAN_SENTINEL_CLIENT_IP:127.0.0.1}"));
        assertTrue(config.contains("xuan-tenant-sentinel-flow-rules.json"));
        assertTrue(config.contains("xuan-tenant-sentinel-degrade-rules.json"));
        assertTrue(config.contains("xuan-tenant-sentinel-param-flow-rules.json"));
        assertTrue(config.contains("feign:"));
        assertTrue(config.contains("sentinel:\n    enabled: true"));
        assertTrue(config.contains("tx-service-group: xuan-erp-tx-group"));
        assertTrue(config.contains("xuan-erp-tx-group"));
        assertTrue(config.contains("type: file"));
        assertTrue(config.contains("default: duaoyunxuan.com:9045"));
    }

    @Test
    void addsSeataUndoLogMigrationAfterCurrentTenantV1() {
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/db/migration/V2__add_seata_undo_log.sql")));
    }

    @Test
    void loadsSentinelNacosDatasourceClassAtRuntime() {
        assertDoesNotThrow(() -> Class.forName("com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource"));
    }
}
