-- 补齐租户管理页的套餐到期字段资源，和现有 tenant 列权限模板保持一致。

INSERT INTO iam_resource_column (
    resource_key, column_key, column_name, data_type, mask_type, is_enabled, sort_no, created_by, updated_by
)
SELECT 'tenant', 'currentPlanExpiresAt', '套餐到期时间', 'DATETIME', NULL, true, 75, 'system-v35', 'system-v35'
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_resource_column existing
    WHERE existing.resource_key = 'tenant'
      AND existing.column_key = 'currentPlanExpiresAt'
      AND existing.deleted_at IS NULL
);

WITH template_items(template_code, column_key, access_mode) AS (
    VALUES
    ('tenant_admin_default', 'currentPlanExpiresAt', 'VISIBLE'),
    ('tenant_readonly_masked', 'currentPlanExpiresAt', 'VISIBLE')
)
INSERT INTO iam_column_permission_template_item (
    template_id, resource_column_id, access_mode, created_by, updated_by
)
SELECT template.id, resource_column.id, template_items.access_mode, 'system-v35', 'system-v35'
FROM template_items
JOIN iam_column_permission_template template
  ON template.tenant_id = 0
 AND template.code = template_items.template_code
 AND template.deleted_at IS NULL
JOIN iam_resource_column resource_column
  ON resource_column.resource_key = 'tenant'
 AND resource_column.column_key = template_items.column_key
 AND resource_column.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_column_permission_template_item existing
    WHERE existing.template_id = template.id
      AND existing.resource_column_id = resource_column.id
      AND existing.deleted_at IS NULL
);
