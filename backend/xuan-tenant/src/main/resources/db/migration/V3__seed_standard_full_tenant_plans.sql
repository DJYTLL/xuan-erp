-- Seed tenant plans that mirror IAM initialization templates.
-- This migration is append-only: V1-V2 history remains unchanged.

WITH seed_plan (
    code,
    name,
    status,
    billing_cycle,
    price_amount,
    currency,
    max_user_count,
    max_warehouse_count,
    max_storage_gb,
    feature_flags,
    sort_no,
    remark
) AS (
    VALUES
    (
        'FREE',
        '基础版',
        'ENABLED',
        'MONTHLY',
        0,
        'CNY',
        10,
        3,
        10,
        '{"modules":["product","party","warehouse","inventory"],"iamInitTemplateCode":"basic"}'::jsonb,
        10,
        '基础版套餐，默认绑定 basic 初始化模板。'
    ),
    (
        'STANDARD',
        '标准版',
        'ENABLED',
        'MONTHLY',
        199,
        'CNY',
        50,
        10,
        100,
        '{"modules":["product","party","warehouse","inventory","sales","procurement"],"iamInitTemplateCode":"standard"}'::jsonb,
        20,
        '标准版套餐，默认绑定 standard 初始化模板。'
    ),
    (
        'FULL',
        '完整版',
        'ENABLED',
        'MONTHLY',
        499,
        'CNY',
        NULL,
        NULL,
        NULL,
        '{"modules":["product","party","warehouse","inventory","sales","procurement","finance","audit"],"iamInitTemplateCode":"full"}'::jsonb,
        30,
        '完整版套餐，默认绑定 full 初始化模板。'
    )
),
updated AS (
    UPDATE tenant_plan
    SET name = seed_plan.name,
        status = seed_plan.status,
        billing_cycle = seed_plan.billing_cycle,
        price_amount = seed_plan.price_amount,
        currency = seed_plan.currency,
        max_user_count = seed_plan.max_user_count,
        max_warehouse_count = seed_plan.max_warehouse_count,
        max_storage_gb = seed_plan.max_storage_gb,
        feature_flags = seed_plan.feature_flags,
        sort_no = seed_plan.sort_no,
        remark = seed_plan.remark,
        updated_by = 'system',
        updated_at = now()
    FROM seed_plan
    WHERE tenant_plan.code = seed_plan.code
      AND tenant_plan.deleted_at IS NULL
    RETURNING tenant_plan.code
)
INSERT INTO tenant_plan (
    code,
    name,
    status,
    billing_cycle,
    price_amount,
    currency,
    max_user_count,
    max_warehouse_count,
    max_storage_gb,
    feature_flags,
    sort_no,
    remark,
    created_by,
    updated_by
)
SELECT
    seed_plan.code,
    seed_plan.name,
    seed_plan.status,
    seed_plan.billing_cycle,
    seed_plan.price_amount,
    seed_plan.currency,
    seed_plan.max_user_count,
    seed_plan.max_warehouse_count,
    seed_plan.max_storage_gb,
    seed_plan.feature_flags,
    seed_plan.sort_no,
    seed_plan.remark,
    'system',
    'system'
FROM seed_plan
WHERE NOT EXISTS (
    SELECT 1
    FROM tenant_plan existing
    WHERE existing.code = seed_plan.code
      AND existing.deleted_at IS NULL
);
