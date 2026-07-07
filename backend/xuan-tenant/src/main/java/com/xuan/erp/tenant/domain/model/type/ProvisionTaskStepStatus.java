package com.xuan.erp.tenant.domain.model.type;

public enum ProvisionTaskStepStatus implements CodeEnum {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    SKIPPED,
    CANCELLED;

    @Override
    public String code() {
        return name();
    }

    public static ProvisionTaskStepStatus fromCode(String code) {
        return EnumCodes.fromCode(ProvisionTaskStepStatus.class, code);
    }
}
