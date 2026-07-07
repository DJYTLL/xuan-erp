package com.xuan.erp.tenant.domain.model.type;

public enum BillingCycle implements CodeEnum {
    MONTHLY,
    YEARLY,
    PERMANENT;

    @Override
    public String code() {
        return name();
    }

    public static BillingCycle fromCode(String code) {
        return EnumCodes.fromCode(BillingCycle.class, code);
    }
}
