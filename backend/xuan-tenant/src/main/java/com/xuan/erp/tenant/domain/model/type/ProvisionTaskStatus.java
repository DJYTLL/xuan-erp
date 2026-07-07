package com.xuan.erp.tenant.domain.model.type;

public enum ProvisionTaskStatus implements CodeEnum {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    @Override
    public String code() {
        return name();
    }

    public static ProvisionTaskStatus fromCode(String code) {
        return EnumCodes.fromCode(ProvisionTaskStatus.class, code);
    }
}
