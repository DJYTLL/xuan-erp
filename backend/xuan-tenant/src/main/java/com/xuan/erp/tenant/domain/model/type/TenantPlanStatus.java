package com.xuan.erp.tenant.domain.model.type;

public enum TenantPlanStatus implements CodeEnum {
    ENABLED,
    DISABLED;

    @Override
    public String code() {
        return name();
    }

    public static TenantPlanStatus fromCode(String code) {
        return EnumCodes.fromCode(TenantPlanStatus.class, code);
    }
}
