-- Add dedicated xuan-tenant provisioning callback permission.
-- This migration is append-only: V1-V10 history remains unchanged.

WITH callback_permission(code, name, service_name, menu_code, description) AS (
    VALUES
        ('tenant-provision:callback', '租户初始化回调', 'xuan-tenant', 'system', '接收 IAM 和业务服务的租户初始化步骤回执')
)
INSERT INTO iam_permission (code, name, service_name, menu_code, description, is_enabled, created_by, updated_by)
SELECT callback_permission.code,
       callback_permission.name,
       callback_permission.service_name,
       callback_permission.menu_code,
       callback_permission.description,
       true,
       'system',
       'system'
FROM callback_permission
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_permission existing
    WHERE existing.code = callback_permission.code
      AND existing.deleted_at IS NULL
);
