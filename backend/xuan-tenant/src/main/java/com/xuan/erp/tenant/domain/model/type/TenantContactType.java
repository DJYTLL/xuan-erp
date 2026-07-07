package com.xuan.erp.tenant.domain.model.type;

public enum TenantContactType implements CodeEnum {
    ADMIN,
    BUSINESS,
    TECHNICAL,
    FINANCE,
    OTHER;

    @Override
    public String code() {
        return name();
    }

    public static TenantContactType fromCode(String code) {
        return EnumCodes.fromCode(TenantContactType.class, code);
    }
}
