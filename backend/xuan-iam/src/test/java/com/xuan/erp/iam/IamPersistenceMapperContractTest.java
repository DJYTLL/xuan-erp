package com.xuan.erp.iam;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamMenuRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamPermissionRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamRoleRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamUserRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamPersistenceMapperContractTest {

    private static final Path MAPPER_DIR = Path.of("src/main/resources/mapper/iam");

    @Test
    void exposesAllIamMapperXmlResources() {
        List<String> xmlFiles = List.of(
                "IamUserPersistenceMapper.xml",
                "IamTenantBootstrapMapper.xml",
                "IamRolePersistenceMapper.xml",
                "IamPermissionPersistenceMapper.xml",
                "IamMenuPersistenceMapper.xml",
                "IamAuthorizationSnapshotPersistenceMapper.xml"
        );

        for (String fileName : xmlFiles) {
            assertTrue(Files.exists(MAPPER_DIR.resolve(fileName)), "缺少 XML Mapper: " + fileName);
        }
    }

    @Test
    void bootstrapMapperXmlDefinesFunctionCallNamespace() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("IamTenantBootstrapMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 IamTenantBootstrapMapper.xml");

        String xml = Files.readString(xmlPath);
        assertTrue(xml.contains("namespace=\"com.xuan.erp.iam.infrastructure.persistence.mapper.IamTenantBootstrapMapper\""));
        assertTrue(xml.contains("bootstrap_iam_tenant"));
    }

    @Test
    void applicationConfigLoadsXmlMapperResources() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertTrue(applicationYaml.contains("mapper-locations:"), "缺少 MyBatis XML mapper 扫描配置");
        assertTrue(applicationYaml.contains("classpath*:mapper/**/*.xml"), "必须扫描 classpath*:mapper/**/*.xml");
    }

    @Test
    void userMapperXmlDefinesUserResultMapAndCrudStatements() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("IamUserPersistenceMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 IamUserPersistenceMapper.xml");

        String xml = Files.readString(xmlPath);
        assertTrue(xml.contains("namespace=\"com.xuan.erp.iam.infrastructure.persistence.mapper.IamUserPersistenceMapper\""));
        assertTrue(xml.contains("<resultMap"));
        assertTrue(xml.contains("iam_user"));
        assertTrue(xml.contains("<insert"));
        assertTrue(xml.contains("<update"));
        assertTrue(xml.contains("<select"));
    }

    @Test
    void constructorMappedRecordsAcceptMybatisWrapperTypes() {
        assertDoesNotThrow(() -> IamUserRecord.class.getDeclaredConstructor(
                Long.class, Long.class, String.class, String.class, String.class, String.class, String.class,
                String.class, Boolean.class, Boolean.class, Boolean.class, Boolean.class, OffsetDateTime.class,
                OffsetDateTime.class, Integer.class, OffsetDateTime.class, OffsetDateTime.class, Boolean.class,
                Long.class, String.class, String.class, OffsetDateTime.class, String.class, OffsetDateTime.class,
                String.class, String.class, OffsetDateTime.class));
        assertDoesNotThrow(() -> IamRoleRecord.class.getDeclaredConstructor(
                Long.class, Long.class, String.class, String.class, String.class, Boolean.class, String.class,
                OffsetDateTime.class, String.class, OffsetDateTime.class, String.class, String.class,
                OffsetDateTime.class));
        assertDoesNotThrow(() -> IamPermissionRecord.class.getDeclaredConstructor(
                Long.class, String.class, String.class, String.class, String.class, String.class, Boolean.class,
                String.class, OffsetDateTime.class, String.class, OffsetDateTime.class, String.class, String.class,
                OffsetDateTime.class));
        assertDoesNotThrow(() -> IamMenuRecord.class.getDeclaredConstructor(
                Long.class, String.class, Long.class, String.class, String.class, String.class, String.class,
                String.class, Integer.class, Boolean.class, String.class, OffsetDateTime.class, String.class,
                OffsetDateTime.class, String.class, String.class, OffsetDateTime.class));
    }
}
