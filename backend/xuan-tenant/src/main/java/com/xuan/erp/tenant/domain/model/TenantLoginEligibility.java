package com.xuan.erp.tenant.domain.model;

/**
 * 租户登录资格，表达租户生命周期和套餐有效期共同推导出的登录门禁结果。
 */
public record TenantLoginEligibility(
        boolean allowed,
        String deniedReason
) {

    public static TenantLoginEligibility allow() {
        return new TenantLoginEligibility(true, null);
    }

    public static TenantLoginEligibility denied(String reason) {
        return new TenantLoginEligibility(false, reason);
    }
}
