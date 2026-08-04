-- Ensure tenant code can be used as a login business identifier.
CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_normalized_code_active
    ON tenant (normalized_code)
    WHERE deleted_at IS NULL;
