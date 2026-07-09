package com.xuan.erp.tenant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantProvisionPersistenceMapperContractTest {

    private static final Path MAPPER_DIR = Path.of("src/main/resources/mapper/tenant");

    @Test
    void exposesProvisioningMapperXmlResources() {
        List<String> xmlFiles = List.of(
                "TenantProvisionTaskPersistenceMapper.xml",
                "TenantProvisionTaskStepPersistenceMapper.xml",
                "TenantOutboxEventPersistenceMapper.xml"
        );

        for (String fileName : xmlFiles) {
            assertTrue(Files.exists(MAPPER_DIR.resolve(fileName)), "缺少 XML Mapper: " + fileName);
        }
    }

    @Test
    void stepMapperXmlDefinesLookupAndWriteStatements() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("TenantProvisionTaskStepPersistenceMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 TenantProvisionTaskStepPersistenceMapper.xml");

        String xml = Files.readString(xmlPath);
        assertTrue(xml.contains("namespace=\"com.xuan.erp.tenant.infrastructure.persistence.mapper.TenantProvisionTaskStepPersistenceMapper\""));
        assertTrue(xml.contains("<select id=\"findByTaskId\""));
        assertTrue(xml.contains("<select id=\"findActiveByTaskIdAndStepKey\""));
        assertTrue(xml.contains("<insert id=\"insert\""));
        assertTrue(xml.contains("<update id=\"update\""));
    }

    @Test
    void outboxMapperXmlDefinesLookupAndWriteStatements() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("TenantOutboxEventPersistenceMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 TenantOutboxEventPersistenceMapper.xml");

        String xml = Files.readString(xmlPath);
        assertTrue(xml.contains("namespace=\"com.xuan.erp.tenant.infrastructure.persistence.mapper.TenantOutboxEventPersistenceMapper\""));
        assertTrue(xml.contains("<select id=\"findById\""));
        assertTrue(xml.contains("<insert id=\"insert\""));
        assertTrue(xml.contains("<update id=\"update\""));
    }
}
