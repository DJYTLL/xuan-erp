CREATE TABLE IF NOT EXISTS iam_user_preference (
    id BIGSERIAL NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    preference_key VARCHAR(128) NOT NULL,
    preference_json JSONB DEFAULT '{}'::jsonb NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_iam_user_preference PRIMARY KEY (id),
    CONSTRAINT uk_iam_user_preference_user_key UNIQUE (tenant_id, user_id, preference_key)
);

COMMENT ON TABLE iam_user_preference IS 'IAM 用户通用偏好配置。';
COMMENT ON COLUMN iam_user_preference.id IS '主键';
COMMENT ON COLUMN iam_user_preference.tenant_id IS '租户 ID，来源于 xuan-tenant';
COMMENT ON COLUMN iam_user_preference.user_id IS '用户 ID';
COMMENT ON COLUMN iam_user_preference.preference_key IS '偏好键，例如 shell.layout、shell.tabs、table.product.list';
COMMENT ON COLUMN iam_user_preference.preference_json IS '偏好内容 JSON';
COMMENT ON COLUMN iam_user_preference.created_by IS '创建人';
COMMENT ON COLUMN iam_user_preference.created_at IS '创建时间';
COMMENT ON COLUMN iam_user_preference.updated_by IS '更新人';
COMMENT ON COLUMN iam_user_preference.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_iam_user_preference_user
    ON iam_user_preference (tenant_id, user_id);
