package com.xuan.erp.tenant.domain.model.type;

public enum TenantStatus implements CodeEnum {
    PROVISIONING,
    PROVISIONED,
    ENABLED,
    SUSPENDED,
    DISABLED;

    @Override
    public String code() {
        return name();
    }

    public static TenantStatus fromCode(String code) {
        return EnumCodes.fromCode(TenantStatus.class, code);
    }
}
