package com.xuan.erp.tenant.domain.model.type;

public enum TenantDomainStatus implements CodeEnum {
    PENDING,
    VERIFIED,
    FAILED,
    DISABLED;

    @Override
    public String code() {
        return name();
    }

    public static TenantDomainStatus fromCode(String code) {
        return EnumCodes.fromCode(TenantDomainStatus.class, code);
    }
}
