package com.xuan.erp.iam.domain.model.type;

/**
 * IAM 用户租户成员状态枚举，表示用户在租户内的成员生命周期。
 */
public enum IamMembershipStatus implements CodeEnum {
    ACTIVE,
    SUSPENDED,
    LEFT;

    @Override
    public String code() {
        return name();
    }

    public static IamMembershipStatus fromCode(String code) {
        return EnumCodes.fromCode(IamMembershipStatus.class, code);
    }
}
