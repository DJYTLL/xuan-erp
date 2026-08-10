package com.xuan.erp.tenant;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantPermissionDraftContractTest {

    private static final Path COLUMNS_DRAFT_PATH = Path.of("src/main/resources/permissions/columns.yaml");
    private static final Path TENANT_BROWSE_TABLE_SCHEMA_PATH = Path.of("..", "..", "frontend", "src", "config", "tenantBrowseTableSchema.ts");
    private static final Path COLUMN_PERMISSION_PAGES_PATH = Path.of("..", "..", "frontend", "src", "config", "columnPermissionPages.ts");
    private static final Path IAM_V33_PATH = Path.of("..", "xuan-iam", "src", "main", "resources", "db", "migration", "V33__add_column_permission_templates.sql");
    private static final Path IAM_V35_PATH = Path.of("..", "xuan-iam", "src", "main", "resources", "db", "migration", "V35__add_tenant_plan_expiry_column_permission.sql");
    private static final Path IAM_V40_PATH = Path.of("..", "xuan-iam", "src", "main", "resources", "db", "migration", "V40__add_tenant_provisioned_column_permission_resource.sql");
    private static final Path IAM_V46_PATH = Path.of("..", "xuan-iam", "src", "main", "resources", "db", "migration", "V46__add_tenant_permission_sync_status_column_resource.sql");

    @Test
    void tenantColumnsDraftUsesResourceKeyShapeInsteadOfLegacyPageKeySketch() throws IOException {
        String draft = Files.readString(COLUMNS_DRAFT_PATH);

        assertTrue(draft.contains("resourceKey: tenant"), "tenant 列权限草稿必须切换到 resourceKey 语义，不能继续使用旧 pageKey 草稿");
        assertTrue(draft.contains("menuCode: tenant-management"), "tenant 列权限草稿必须声明对应页面菜单编码");
        assertTrue(draft.contains("browseFields:"), "tenant 列权限草稿必须区分列表字段");
        assertTrue(draft.contains("detailFields:"), "tenant 列权限草稿必须区分详情字段");
        assertFalse(draft.contains("pageKey:"), "tenant 列权限草稿不能继续保留旧 pageKey 结构");
        assertFalse(draft.contains("tenant-config"), "tenant-config 不是当前 tenant-management 列权限事实源，不能继续留在草稿里");
        assertFalse(draft.contains("contactEmail"), "已废弃字段不能继续保留在列权限草稿里");
        assertFalse(draft.contains("disableReason"), "已废弃字段不能继续保留在列权限草稿里");
        assertFalse(draft.contains("sensitiveConfigSummary"), "已废弃字段不能继续保留在列权限草稿里");
    }

    @Test
    void tenantColumnsDraftMatchesFrontendSchemaAndIamRegisteredColumns() throws IOException {
        String draft = Files.readString(COLUMNS_DRAFT_PATH);
        String tenantBrowseTableSchema = Files.readString(TENANT_BROWSE_TABLE_SCHEMA_PATH);
        String columnPermissionPages = Files.readString(COLUMN_PERMISSION_PAGES_PATH);

        List<String> expectedBrowseFields = extractSingleQuotedValues(tenantBrowseTableSchema, "columnKey:\\s*'([^']+)'");
        List<String> expectedDetailFields = extractTenantDetailColumnKeys(columnPermissionPages);

        TenantColumnsDraft tenantDraft = parseTenantColumnsDraft(draft);

        assertEquals(expectedBrowseFields, tenantDraft.browseFields(), "tenant 列权限草稿的列表字段必须和 tenantBrowseTableSchema.ts 完全一致");
        assertEquals(expectedDetailFields, tenantDraft.detailFields(), "tenant 列权限草稿的详情字段必须和 columnPermissionPages.ts 完全一致");

        Set<String> expectedIamColumns = new LinkedHashSet<>();
        expectedIamColumns.addAll(extractTenantColumnsFromMigration(Files.readString(IAM_V33_PATH)));
        expectedIamColumns.addAll(extractTenantColumnsFromMigration(Files.readString(IAM_V35_PATH)));
        expectedIamColumns.addAll(extractTenantColumnsFromMigration(Files.readString(IAM_V40_PATH)));
        expectedIamColumns.addAll(extractTenantColumnsFromMigration(Files.readString(IAM_V46_PATH)));

        Set<String> draftColumns = new LinkedHashSet<>(tenantDraft.browseFields());
        draftColumns.addAll(tenantDraft.detailFields());

        assertEquals(expectedIamColumns, draftColumns, "tenant 列权限草稿必须和 IAM tenant 列资源迁移保持一致");
    }

    private static TenantColumnsDraft parseTenantColumnsDraft(String draftSource) {
        List<String> browseFields = new ArrayList<>();
        List<String> detailFields = new ArrayList<>();
        boolean inTenantBlock = false;
        String currentSection = null;

        for (String rawLine : draftSource.split("\\R")) {
            String line = rawLine.replace("\t", "    ");
            String trimmed = line.trim();

            if (trimmed.startsWith("- resourceKey:")) {
                inTenantBlock = "tenant".equals(trimmed.substring("- resourceKey:".length()).trim());
                currentSection = null;
                continue;
            }
            if (!inTenantBlock) {
                continue;
            }
            if (trimmed.startsWith("menuCode:") || trimmed.startsWith("note:")) {
                continue;
            }
            if ("browseFields:".equals(trimmed)) {
                currentSection = "browse";
                continue;
            }
            if ("detailFields:".equals(trimmed)) {
                currentSection = "detail";
                continue;
            }
            if (trimmed.startsWith("- ")) {
                String value = trimmed.substring(2).trim();
                if ("browse".equals(currentSection)) {
                    browseFields.add(value);
                } else if ("detail".equals(currentSection)) {
                    detailFields.add(value);
                }
            }
        }

        return new TenantColumnsDraft(browseFields, detailFields);
    }

    private static List<String> extractTenantDetailColumnKeys(String source) {
        Matcher matcher = Pattern.compile("menuCode:\\s*'tenant-management'[\\s\\S]*?detailColumnKeys:\\s*\\[([^\\]]*)\\]").matcher(source);
        assertTrue(matcher.find(), "columnPermissionPages.ts 必须声明 tenant-management 的 detailColumnKeys");
        return extractSingleQuotedValues(matcher.group(1), "'([^']+)'");
    }

    private static List<String> extractTenantColumnsFromMigration(String source) {
        List<String> result = new ArrayList<>();
        result.addAll(extractSingleQuotedValues(source, "\\('tenant',\\s*'([^']+)'"));
        result.addAll(extractSingleQuotedValues(source, "SELECT\\s+'tenant',\\s*'([^']+)'"));
        return result;
    }

    private static List<String> extractSingleQuotedValues(String source, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(source);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private record TenantColumnsDraft(List<String> browseFields, List<String> detailFields) {
    }
}
