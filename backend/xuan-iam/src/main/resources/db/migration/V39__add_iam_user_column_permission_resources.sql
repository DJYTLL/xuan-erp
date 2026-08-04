-- 登记用户授权页表格字段，供列权限模板和角色列权限规则配置使用。

INSERT INTO iam_resource_column (
    resource_key, column_key, column_name, data_type, mask_type, is_enabled, sort_no, created_by, updated_by
)
SELECT seed.resource_key,
       seed.column_key,
       seed.column_name,
       seed.data_type,
       seed.mask_type,
       true,
       seed.sort_no,
       'system-v39',
       'system-v39'
FROM (
    VALUES
        ('iam-user', 'username', '用户名', 'STRING', NULL, 10),
        ('iam-user', 'displayName', '显示名', 'STRING', NULL, 20),
        ('iam-user', 'phone', '手机号', 'STRING', 'PHONE', 30),
        ('iam-user', 'email', '邮箱', 'STRING', NULL, 40),
        ('iam-user', 'authVersion', '权限版本', 'NUMBER', NULL, 50),
        ('iam-user', 'status', '状态', 'ENUM', NULL, 60)
) AS seed(resource_key, column_key, column_name, data_type, mask_type, sort_no)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_resource_column existing
    WHERE existing.resource_key = seed.resource_key
      AND existing.column_key = seed.column_key
      AND existing.deleted_at IS NULL
);
