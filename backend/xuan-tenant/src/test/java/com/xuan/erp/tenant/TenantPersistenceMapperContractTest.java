package com.xuan.erp.tenant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantPersistenceMapperContractTest {

    private static final Path MAPPER_DIR = Path.of("src/main/resources/mapper/tenant");

    @Test
    void exposesTenantMapperXmlResources() {
        List<String> xmlFiles = List.of(
                "TenantPersistenceMapper.xml",
                "TenantConfigPersistenceMapper.xml",
                "TenantProvisionTaskPersistenceMapper.xml"
        );

        for (String fileName : xmlFiles) {
            assertTrue(Files.exists(MAPPER_DIR.resolve(fileName)), "缺少 XML Mapper: " + fileName);
        }
    }

    @Test
    void tenantMapperXmlDefinesCrudAndPagingStatements() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("TenantPersistenceMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 TenantPersistenceMapper.xml");

        String xml = Files.readString(xmlPath);
        assertTrue(xml.contains("namespace=\"com.xuan.erp.tenant.infrastructure.persistence.mapper.TenantPersistenceMapper\""));
        assertTrue(xml.contains("<select id=\"findPage\""));
        assertTrue(xml.contains("<select id=\"countActive\""));
        assertTrue(xml.contains("<insert id=\"insert\""));
        assertTrue(xml.contains("<update id=\"update\""));
    }

    @Test
    void applicationConfigLoadsMybatisXmlMappers() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertTrue(applicationYaml.contains("mybatis:"), "缺少 MyBatis 配置段");
        assertTrue(applicationYaml.contains("mapper-locations: classpath*:mapper/**/*.xml"),
                "缺少 XML Mapper 扫描配置");
    }

    @Test
    void recordMappersUseConstructorResultMaps() throws IOException {
        List<String> xmlFiles = List.of(
                "TenantPersistenceMapper.xml",
                "TenantConfigPersistenceMapper.xml",
                "TenantProvisionTaskPersistenceMapper.xml",
                "TenantProvisionTaskStepPersistenceMapper.xml",
                "TenantOutboxEventPersistenceMapper.xml"
        );

        for (String fileName : xmlFiles) {
            String xml = Files.readString(MAPPER_DIR.resolve(fileName));
            assertTrue(xml.contains("<constructor>"), "record mapper 必须使用构造器映射: " + fileName);
        }
    }
}
