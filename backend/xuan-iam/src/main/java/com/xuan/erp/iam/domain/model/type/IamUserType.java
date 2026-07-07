package com.xuan.erp.iam.domain.model.type;

/**
 * IAM 用户类型枚举，区分租户用户和平台管理员。
 */
public enum IamUserType implements CodeEnum {
    TENANT_USER,
    PLATFORM_ADMIN;

    @Override
    public String code() {
        return name();
    }

    public static IamUserType fromCode(String code) {
        return EnumCodes.fromCode(IamUserType.class, code);
    }
}
