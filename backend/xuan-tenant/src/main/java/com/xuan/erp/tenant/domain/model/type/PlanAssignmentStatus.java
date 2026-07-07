package com.xuan.erp.tenant.domain.model.type;

public enum PlanAssignmentStatus implements CodeEnum {
    ACTIVE,
    EXPIRED,
    CANCELLED;

    @Override
    public String code() {
        return name();
    }

    public static PlanAssignmentStatus fromCode(String code) {
        return EnumCodes.fromCode(PlanAssignmentStatus.class, code);
    }
}
