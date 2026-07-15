package com.xuan.erp.common.security;

/**
 * Gateway-authenticated identity headers consumed by servlet business services.
 */
public final class GatewayIdentityHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String USERNAME = "X-Username";
    public static final String ROLES = "X-Roles";
    public static final String AUTH_VERSION = "X-Auth-Version";
    public static final String PERMISSIONS = "X-Permissions";

    private GatewayIdentityHeaders() {
    }
}
