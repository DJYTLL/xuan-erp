package com.xuan.erp.tenant.domain.model.type;

public enum OutboxEventStatus implements CodeEnum {
    PENDING,
    PUBLISHING,
    PUBLISHED,
    FAILED,
    DEAD_LETTERED;

    @Override
    public String code() {
        return name();
    }

    public static OutboxEventStatus fromCode(String code) {
        return EnumCodes.fromCode(OutboxEventStatus.class, code);
    }
}
