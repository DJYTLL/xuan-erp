package com.xuan.erp.iam.domain.model.type;

/**
 * IAM 租户初始化任务状态枚举，区分成功和失败的初始化结果。
 */
public enum IamBootstrapTaskStatus implements CodeEnum {
    SUCCEEDED,
    FAILED;

    @Override
    public String code() {
        return name();
    }

    public static IamBootstrapTaskStatus fromCode(String code) {
        return EnumCodes.fromCode(IamBootstrapTaskStatus.class, code);
    }
}
