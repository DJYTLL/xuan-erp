package com.xuan.erp.audit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogPersistenceMapperContractTest {

    private static final Path MAPPER_XML = Path.of(
            "src/main/resources/mapper/audit/AuditLogPersistenceMapper.xml"
    );

    @Test
    void mapperInsertsAuditLogIntoPostgresqlAuditLogTable() throws IOException {
        String xml = Files.readString(MAPPER_XML);

        assertTrue(xml.contains("INSERT INTO audit_log"), "A15 必须写入 audit_log 表");
        assertTrue(xml.contains("tenant_id"), "必须写入租户字段");
        assertTrue(xml.contains("actor_username"), "必须写入操作者字段");
        assertTrue(xml.contains("action"), "必须写入动作字段");
        assertTrue(xml.contains("status"), "必须写入状态字段");
        assertTrue(xml.contains("is_cross_tenant"), "必须写入跨租户字段");
        assertTrue(xml.contains("RETURNING id"), "必须返回 PostgreSQL 生成的审计日志 ID");
    }
}
