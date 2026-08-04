package com.xuan.erp.iam;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamMenuRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamPermissionRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamRefreshTokenRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamRoleRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamUserRecord;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamUserPreferenceRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IamPersistenceMapperContractTest {

    private static final Path MAPPER_DIR = Path.of("src/main/resources/mapper/iam");

    @Test
    void exposesAllIamMapperXmlResources() {
        List<String> xmlFiles = List.of(
                "IamUserPersistenceMapper.xml",
                "IamTenantBootstrapMapper.xml",
                "IamRolePersistenceMapper.xml",
                "IamRolePermissionPersistenceMapper.xml",
                "IamPermissionPersistenceMapper.xml",
                "IamMenuPersistenceMapper.xml",
                "IamUserPreferencePersistenceMapper.xml",
                "IamAuthorizationSnapshotPersistenceMapper.xml",
                "IamRefreshTokenPersistenceMapper.xml",
                "IamTenantPermissionEntitlementPersistenceMapper.xml",
                "IamColumnPermissionManagementPersistenceMapper.xml",
                "IamColumnPermissionPersistenceMapper.xml"
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
    void rolePermissionMapperXmlDefinesGrantReplacementStatements() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("IamRolePermissionPersistenceMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 IamRolePermissionPersistenceMapper.xml");

        String xml = Files.readString(xmlPath);
        assertTrue(xml.contains("namespace=\"com.xuan.erp.iam.infrastructure.persistence.mapper.IamRolePermissionPersistenceMapper\""));
        assertTrue(xml.contains("findPermissionCodesByRoleId"));
        assertTrue(xml.contains("disableRolePermissions"));
        assertTrue(xml.contains("insertRolePermission"));
        assertTrue(xml.contains("iam_role_permission"));
        assertTrue(xml.contains("iam_tenant_permission_entitlement"), "角色权限读取必须受租户权限池约束，避免历史脏授权泄漏到菜单和按钮");
        assertTrue(xml.contains("entitlement.permission_id = role_permission.permission_id"));
        assertTrue(xml.contains("entitlement.is_enabled = true"));
    }

    @Test
    void tenantPermissionEntitlementMapperFindsTenantsFromStableBindingAndFallbackSources() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("IamTenantPermissionEntitlementPersistenceMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 IamTenantPermissionEntitlementPersistenceMapper.xml");

        String xml = Files.readString(xmlPath);

        assertTrue(xml.contains("namespace=\"com.xuan.erp.iam.infrastructure.persistence.mapper.IamTenantPermissionEntitlementPersistenceMapper\""));
        assertTrue(xml.contains("findTenantIdsByInitTemplateCode"));
        assertTrue(xml.contains("iam_tenant_init_template_binding"), "模板变更同步不能只依赖权限池明细，必须有稳定的租户-模板绑定表");
        assertTrue(xml.contains("iam_outbox_event"), "历史租户需要从 bootstrap outbox 的 iamInitTemplateCode 兜底回填同步范围");
        assertTrue(xml.contains("iam_tenant_permission_entitlement"), "已有权限池明细仍可作为兼容兜底来源");
        assertTrue(xml.contains("payload ->> 'iamInitTemplateCode'"));
        assertTrue(xml.contains("jsonb_exists(event.payload, 'iamInitTemplateCode')"), "MyBatis/JDBC 查询里不要直接使用 JSONB ? 操作符，避免被识别为参数占位符");
    }

    @Test
    void columnPermissionManagementMapperXmlDefinesTemplateCrudAndRoleBindingStatements() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("IamColumnPermissionManagementPersistenceMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 IamColumnPermissionManagementPersistenceMapper.xml");

        String xml = Files.readString(xmlPath);

        assertTrue(xml.contains("namespace=\"com.xuan.erp.iam.infrastructure.persistence.mapper.IamColumnPermissionManagementPersistenceMapper\""));
        assertTrue(xml.contains("findEnabledResourceColumns"));
        assertTrue(xml.contains("findTemplates"));
        assertTrue(xml.contains("findTemplateById"));
        assertTrue(xml.contains("findTemplateByTenantIdAndCode"));
        assertTrue(xml.contains("insertTemplate"));
        assertTrue(xml.contains("updateTemplate"));
        assertTrue(xml.contains("setTemplateEnabled"));
        assertTrue(xml.contains("findTemplateItems"));
        assertTrue(xml.contains("disableTemplateItems"));
        assertTrue(xml.contains("insertTemplateItem"));
        assertTrue(xml.contains("findRoleTemplateBinding"));
        assertTrue(xml.contains("disableRoleTemplateBinding"));
        assertTrue(xml.contains("insertRoleTemplateBinding"));
        assertTrue(xml.contains("findResourceColumnById"));
        assertTrue(xml.contains("iam_role_column_permission_template_binding"));
    }

    @Test
    void columnPermissionRuntimeMapperReadsTenantBoundedRoleRules() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("IamColumnPermissionPersistenceMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 IamColumnPermissionPersistenceMapper.xml");

        String xml = Files.readString(xmlPath);

        assertTrue(xml.contains("namespace=\"com.xuan.erp.iam.infrastructure.persistence.mapper.IamColumnPermissionPersistenceMapper\""));
        assertTrue(xml.contains("findColumnPermissionRulesByRoleIds"));
        assertTrue(xml.contains("iam_role_column_permission_rule"),
                "运行时授权快照必须读取角色独立列权限规则表");
        assertTrue(xml.contains("iam_tenant_column_permission_template_assignment"),
                "运行时授权快照必须受租户列权限模板上限约束");
        assertTrue(xml.contains("iam_resource_column"),
                "运行时授权快照必须覆盖已登记字段，租户未授权字段需要返回 HIDDEN");
        assertTrue(xml.contains("tenant_column_upper_bound"),
                "运行时授权快照必须先计算租户字段上限");
        assertTrue(xml.contains("role_column_rule"),
                "运行时授权快照必须再合并角色字段规则");
        assertTrue(xml.contains("roleIds != null and roleIds.size() &gt; 0"),
                "运行时授权快照必须兼容无角色用户，不能拼出 role_id IN ()");
        assertTrue(xml.contains("AND 1 = 0"),
                "无角色用户仍要返回租户上限字段，角色规则 CTE 应显式为空");
        assertFalse(xml.contains("iam_role_column_permission_template_binding"),
                "运行时授权快照不能继续通过角色模板绑定计算列权限");
    }

    @Test
    void userPreferenceMapperXmlDefinesPreferenceUpsertStatements() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("IamUserPreferencePersistenceMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 IamUserPreferencePersistenceMapper.xml");

        String xml = Files.readString(xmlPath);
        assertTrue(xml.contains("namespace=\"com.xuan.erp.iam.infrastructure.persistence.mapper.IamUserPreferencePersistenceMapper\""));
        assertTrue(xml.contains("findByTenantIdAndUserIdAndPreferenceKey"));
        assertTrue(xml.contains("upsert"));
        assertTrue(xml.contains("iam_user_preference"));
    }

    @Test
    void refreshTokenMapperXmlDefinesRotationStatements() throws IOException {
        Path xmlPath = MAPPER_DIR.resolve("IamRefreshTokenPersistenceMapper.xml");
        assertTrue(Files.exists(xmlPath), "缺少 IamRefreshTokenPersistenceMapper.xml");

        String xml = Files.readString(xmlPath);
        assertTrue(xml.contains("namespace=\"com.xuan.erp.iam.infrastructure.persistence.mapper.IamRefreshTokenPersistenceMapper\""));
        assertTrue(xml.contains("findByTokenHash"));
        assertTrue(xml.contains("iam_refresh_token"));
        assertTrue(xml.contains("<insert"));
        assertTrue(xml.contains("<update"));
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
        assertDoesNotThrow(() -> IamUserPreferenceRecord.class.getDeclaredConstructor(
                Long.class, Long.class, Long.class, String.class, String.class, String.class,
                OffsetDateTime.class, String.class, OffsetDateTime.class));
        assertDoesNotThrow(() -> IamRefreshTokenRecord.class.getDeclaredConstructor(
                Long.class, Long.class, Long.class, String.class, String.class, OffsetDateTime.class,
                OffsetDateTime.class, String.class, OffsetDateTime.class, Long.class, String.class, String.class,
                String.class, String.class, OffsetDateTime.class, String.class, OffsetDateTime.class));
    }
}
