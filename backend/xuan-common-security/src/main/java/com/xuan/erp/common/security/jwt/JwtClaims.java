package com.xuan.erp.common.security.jwt;

import com.xuan.erp.common.security.CurrentUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * JWT 中允许携带的轻量身份字段。
 *
 * <p>JWT 会在多个服务之间传递，因此这里只保留后端鉴权和租户隔离所需的最小身份信息：
 * 用户 ID、租户 ID、用户名、角色码、授权版本号和权限码集合。不要把手机号、邮箱、真实姓名、组织结构、角色详情、
 * 菜单树、列权限明细等可变或敏感信息放入本对象，也不要把它们写入访问令牌。</p>
 *
 * <p>本对象是公共安全模块和 Spring Security JWT 对象之间的边界模型。外部认证组件可以继续使用
 * {@link Jwt} 完成签名、过期时间和标准声明校验；业务代码只需要消费这里暴露的轻量身份字段。</p>
 *
 * @param userId 当前登录用户 ID，由 IAM 签发并由 Gateway/资源服务校验后使用
 * @param tenantId 当前访问租户 ID，用于多租户数据隔离和租户状态校验
 * @param username 当前登录用户名，用于日志、审计和轻量展示，不作为权限判断的唯一依据
 * @param roles 当前用户在当前租户下的角色码快照，只保存角色编码，不保存角色名称或授权明细
 * @param authVersion 当前用户授权快照版本，用于后续识别 token 是否落后于服务端最新授权状态
 * @param permissions 当前用户在当前租户下的接口权限码快照，只保存权限码字符串，不保存角色或菜单详情
 */
public record JwtClaims(Long userId, Long tenantId, String username, Set<String> roles, Long authVersion, Set<String> permissions) {

    /**
     * 用户 ID 的 JWT claim 名称。
     */
    public static final String CLAIM_USER_ID = "userId";

    /**
     * 租户 ID 的 JWT claim 名称。
     */
    public static final String CLAIM_TENANT_ID = "tenantId";

    /**
     * 用户名的 JWT claim 名称。
     */
    public static final String CLAIM_USERNAME = "username";

    /**
     * 角色码集合的 JWT claim 名称。
     */
    public static final String CLAIM_ROLES = "roles";

    /**
     * 授权快照版本号的 JWT claim 名称。
     */
    public static final String CLAIM_AUTH_VERSION = "authVersion";

    /**
     * 权限码集合的 JWT claim 名称。
     */
    public static final String CLAIM_PERMISSIONS = "permissions";

    /**
     * 规范化构造入口，确保核心身份字段不能为空，并把权限集合固定为不可变集合。
     */
    public JwtClaims {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        roles = normalizeTextSet(roles);
        Objects.requireNonNull(authVersion, "authVersion must not be null");
        permissions = normalizePermissions(permissions);
    }

    public JwtClaims(Long userId, Long tenantId, String username, Set<String> permissions) {
        this(userId, tenantId, username, Set.of(), 0L, permissions);
    }

    /**
     * 允许调用方使用任意集合类型创建权限快照，内部统一转换为不可变 Set。
     *
     * @param userId 当前登录用户 ID
     * @param tenantId 当前访问租户 ID
     * @param username 当前登录用户名
     * @param permissions 权限码集合，可为空；空值会被规范化为空集合
     */
    public JwtClaims(Long userId, Long tenantId, String username, Collection<String> permissions) {
        this(userId, tenantId, username, Set.of(), 0L, normalizePermissions(permissions));
    }

    public JwtClaims(Long userId, Long tenantId, String username, Collection<String> roles, Long authVersion, Collection<String> permissions) {
        this(userId, tenantId, username, normalizeTextSet(roles), authVersion, normalizePermissions(permissions));
    }

    /**
     * 从 Spring Security 已解析出的 JWT 对象中提取项目允许使用的轻量身份字段。
     *
     * <p>该方法只读取 {@link #CLAIM_USER_ID}、{@link #CLAIM_TENANT_ID}、
     * {@link #CLAIM_USERNAME} 和 {@link #CLAIM_PERMISSIONS} 四类字段。JWT 中即使存在其它
     * 自定义 claim，也不会进入本模型，避免业务代码误依赖令牌中的敏感或易变化信息。</p>
     *
     * @param jwt 已通过解码器解析和基础校验的 JWT 对象
     * @return 轻量身份字段对象
     */
    public static JwtClaims from(Jwt jwt) {
        Objects.requireNonNull(jwt, "jwt must not be null");
        return new JwtClaims(
                requiredLong(jwt, CLAIM_USER_ID),
                requiredLong(jwt, CLAIM_TENANT_ID),
                requiredString(jwt, CLAIM_USERNAME),
                textSet(jwt.getClaim(CLAIM_ROLES), CLAIM_ROLES),
                requiredLong(jwt, CLAIM_AUTH_VERSION),
                permissions(jwt.getClaim(CLAIM_PERMISSIONS)));
    }

    /**
     * 转换为公共安全上下文使用的当前用户对象。
     *
     * <p>{@link CurrentUser} 是业务服务读取当前登录人时使用的稳定类型，转换时不附带任何 JWT
     * 原始信息，避免把 token 细节泄漏到应用层。</p>
     *
     * @return 当前用户上下文
     */
    public CurrentUser toCurrentUser() {
        return new CurrentUser(userId, tenantId, username, roles, authVersion, permissions);
    }

    /**
     * 读取必须存在的长整型 claim。
     *
     * <p>不同 JWT 签发组件可能把 ID 写成 JSON number，也可能写成字符串。这里同时兼容两种形式，
     * 但不接受空字符串、非数字字符串或其它复杂结构，避免把错误的身份字段静默带入业务层。</p>
     *
     * @param jwt 已解析的 JWT 对象
     * @param claimName claim 名称
     * @return 转换后的长整型值
     */
    private static Long requiredLong(Jwt jwt, String claimName) {
        Object value = jwt.getClaim(claimName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ex) {
                throw new JwtException("Invalid JWT claim: " + claimName, ex);
            }
        }
        throw new JwtException("Missing JWT claim: " + claimName);
    }

    /**
     * 读取必须存在的字符串 claim。
     *
     * <p>用户名用于日志、审计和展示，必须明确存在且不能为空白字符串。这里不做用户名格式校验，
     * 用户名合法性由 IAM 用户域模型负责。</p>
     *
     * @param jwt 已解析的 JWT 对象
     * @param claimName claim 名称
     * @return 非空字符串值
     */
    private static String requiredString(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        throw new JwtException("Missing JWT claim: " + claimName);
    }

    /**
     * 从 JWT 原始 claim 中读取权限码集合。
     *
     * <p>权限 claim 可以缺省，缺省时表示当前 token 不携带任何接口权限；也可以是字符串集合。
     * 单个字符串形式主要用于兼容简单签发器。其它结构会被视为无效 claim，避免权限数据格式不一致
     * 造成鉴权误判。</p>
     *
     * @param value JWT 中 {@link #CLAIM_PERMISSIONS} 对应的原始值
     * @return 不可变权限码集合
     */
    private static Set<String> permissions(Object value) {
        return textSet(value, CLAIM_PERMISSIONS);
    }

    private static Set<String> textSet(Object value, String claimName) {
        if (value == null) {
            return Set.of();
        }
        if (value instanceof Collection<?> collection) {
            Set<String> values = new LinkedHashSet<>();
            for (Object item : collection) {
                if (item instanceof String text && !text.isBlank()) {
                    values.add(text);
                }
            }
            return Collections.unmodifiableSet(values);
        }
        if (value instanceof String text && !text.isBlank()) {
            return Set.of(text);
        }
        throw new JwtException("Invalid JWT claim: " + claimName);
    }

    /**
     * 规范化构造器传入的权限码集合。
     *
     * <p>空集合和空值统一转为空不可变集合；集合中的 null 或空白权限码会被丢弃。这样可以让
     * {@link JwtClaims} 一旦构造完成，就始终处于适合业务层读取的稳定状态。</p>
     *
     * @param permissions 调用方传入的权限码集合
     * @return 不可变、去重后的权限码集合
     */
    private static Set<String> normalizePermissions(Collection<String> permissions) {
        return normalizeTextSet(permissions);
    }

    private static Set<String> normalizeTextSet(Collection<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String permission : permissions) {
            if (permission != null && !permission.isBlank()) {
                normalized.add(permission);
            }
        }
        return Collections.unmodifiableSet(normalized);
    }
}
