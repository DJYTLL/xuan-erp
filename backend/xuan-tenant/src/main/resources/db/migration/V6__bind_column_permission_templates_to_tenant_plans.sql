-- 将列权限模板纳入套餐能力边界。
-- 套餐仍然归 xuan-tenant 管理；这里仅在 feature_flags 中保存 IAM 平台列权限模板编码，
-- 实际租户列权限模板池由 xuan-tenant 调用 xuan-iam 同步落地。
UPDATE tenant_plan
SET feature_flags = jsonb_set(
        jsonb_set(
                COALESCE(feature_flags, '{}'::jsonb),
                '{columnPermissionTemplateCodes}',
                '["tenant_readonly_masked"]'::jsonb,
                true
        ),
        '{defaultColumnPermissionTemplateCode}',
        '"tenant_readonly_masked"'::jsonb,
        true
    ),
    updated_by = 'migration',
    updated_at = now()
WHERE code IN ('FREE', 'free')
  AND deleted_at IS NULL;

UPDATE tenant_plan
SET feature_flags = jsonb_set(
        jsonb_set(
                COALESCE(feature_flags, '{}'::jsonb),
                '{columnPermissionTemplateCodes}',
                '["tenant_readonly_masked","tenant_admin_default"]'::jsonb,
                true
        ),
        '{defaultColumnPermissionTemplateCode}',
        '"tenant_readonly_masked"'::jsonb,
        true
    ),
    updated_by = 'migration',
    updated_at = now()
WHERE code IN ('STANDARD', 'standard', 'FULL', 'full')
  AND deleted_at IS NULL;
