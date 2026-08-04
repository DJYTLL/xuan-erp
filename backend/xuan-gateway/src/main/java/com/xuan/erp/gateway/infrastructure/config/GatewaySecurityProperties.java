package com.xuan.erp.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 网关 JWT / JWKS 安全配置属性。
 *
 * <p>该配置用于控制网关是否开启 Bearer Token 验签，以及验签时
 * 需要使用的签发方、受众和 IAM 服务名。</p>
 */
@ConfigurationProperties(prefix = "xuan.gateway.security")
public class GatewaySecurityProperties {

    /**
     * 是否启用网关认证。
     *
     * <p>true 表示启用 JWT / JWKS 验签；
     * false 表示关闭这套认证链路。</p>
     */
    private boolean enabled = true;

    /**
     * 期望的 JWT 签发方。
     *
     * <p>网关验签时会校验 token 中的 iss 是否与该值一致。</p>
     */
    private String issuer = "xuan-iam";

    /**
     * 期望的 JWT 受众。
     *
     * <p>网关验签时会校验 token 中的 aud 是否包含该值。</p>
     */
    private String audience = "xuan-gateway";

    /**
     * IAM 服务名。
     *
     * <p>网关会通过服务发现使用该服务名定位 xuan-iam，
     * 然后拉取 `/.well-known/jwks.json` 公钥集合。</p>
     */
    private String iamServiceName = "xuan-iam";

    /**
     * 网关侧 JWK 本地缓存配置。
     */
    private JwkCache jwkCache = new JwkCache();

    /**
     * 匿名放行路径。
     *
     * <p>这些路径会在 Gateway Security 链路中直接 permitAll，适合健康检查、
     * 登录续期入口、JWKS、Swagger UI 和 OpenAPI 聚合文档等非业务资源。</p>
     */
    private List<String> publicPaths = List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/api/iam/auth/login",
            "/api/iam/auth/refresh",
            "/.well-known/jwks.json");

    /**
     * 网关入口 CORS 配置。
     *
     * <p>该配置会接入 WebFlux Security 链路，确保浏览器预检请求和实际
     * API 请求都能在认证前得到正确的跨域响应头。</p>
     */
    private Cors cors = new Cors();

    /**
     * 网关侧安全审计配置。
     */
    private SecurityAudit securityAudit = new SecurityAudit();

    /**
     * 返回是否启用网关认证。
     *
     * @return true 表示启用；false 表示关闭
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用网关认证。
     *
     * @param enabled true 表示启用；false 表示关闭
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回期望的 JWT 签发方。
     *
     * @return JWT issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * 设置期望的 JWT 签发方。
     *
     * @param issuer JWT issuer
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * 返回期望的 JWT 受众。
     *
     * @return JWT audience
     */
    public String getAudience() {
        return audience;
    }

    /**
     * 设置期望的 JWT 受众。
     *
     * @param audience JWT audience
     */
    public void setAudience(String audience) {
        this.audience = audience;
    }

    /**
     * 返回 IAM 服务名。
     *
     * @return 服务发现中的 IAM 服务名
     */
    public String getIamServiceName() {
        return iamServiceName;
    }

    /**
     * 设置 IAM 服务名。
     *
     * @param iamServiceName 服务发现中的 IAM 服务名
     */
    public void setIamServiceName(String iamServiceName) {
        this.iamServiceName = iamServiceName;
    }

    public JwkCache getJwkCache() {
        return jwkCache;
    }

    public void setJwkCache(JwkCache jwkCache) {
        this.jwkCache = jwkCache == null ? new JwkCache() : jwkCache;
    }

    /**
     * 返回匿名放行路径。
     *
     * @return Gateway Security permitAll 路径
     */
    public List<String> getPublicPaths() {
        return publicPaths;
    }

    /**
     * 设置匿名放行路径。
     *
     * @param publicPaths Gateway Security permitAll 路径
     */
    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths == null ? List.of() : publicPaths;
    }

    /**
     * 返回网关 CORS 配置。
     *
     * @return CORS 配置
     */
    public Cors getCors() {
        return cors;
    }

    /**
     * 设置网关 CORS 配置。
     *
     * @param cors CORS 配置
     */
    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public SecurityAudit getSecurityAudit() {
        return securityAudit;
    }

    public void setSecurityAudit(SecurityAudit securityAudit) {
        this.securityAudit = securityAudit;
    }

    /**
     * 网关入口 CORS 配置项。
     */
    public static class Cors {

        /**
         * 允许访问网关的前端 Origin。
         */
        private List<String> allowedOrigins = List.of("http://127.0.0.1:5173", "http://localhost:5173");

        /**
         * 允许的 HTTP 方法。
         */
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

        /**
         * 允许的请求头。
         */
        private List<String> allowedHeaders = List.of("*");

        /**
         * 允许浏览器读取的响应头。
         */
        private List<String> exposedHeaders = List.of("Authorization", "Content-Disposition");

        /**
         * 是否允许跨域请求携带凭证。
         */
        private boolean allowCredentials = true;

        /**
         * 预检请求缓存秒数。
         */
        private long maxAge = 1800;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    /**
     * 网关侧 JWKS 缓存配置项。
     */
    public static class JwkCache {

        /**
         * 正常 JWK 缓存时间。
         */
        private Duration positiveCacheTtl = Duration.ofMinutes(10);

        /**
         * IAM JWKS 不可用时，允许旧 JWK 继续兜底的时间。
         */
        private Duration staleCacheTtl = Duration.ofMinutes(30);

        /**
         * 未知 kid 的负缓存时间。
         */
        private Duration negativeCacheTtl = Duration.ofSeconds(30);

        /**
         * 后台定期刷新 JWKS 的时间间隔。
         */
        private Duration refreshInterval = Duration.ofMinutes(5);

        public Duration getPositiveCacheTtl() {
            return positiveCacheTtl;
        }

        public void setPositiveCacheTtl(Duration positiveCacheTtl) {
            this.positiveCacheTtl = positiveCacheTtl;
        }

        public Duration getStaleCacheTtl() {
            return staleCacheTtl;
        }

        public void setStaleCacheTtl(Duration staleCacheTtl) {
            this.staleCacheTtl = staleCacheTtl;
        }

        public Duration getNegativeCacheTtl() {
            return negativeCacheTtl;
        }

        public void setNegativeCacheTtl(Duration negativeCacheTtl) {
            this.negativeCacheTtl = negativeCacheTtl;
        }

        public Duration getRefreshInterval() {
            return refreshInterval;
        }

        public void setRefreshInterval(Duration refreshInterval) {
            this.refreshInterval = refreshInterval;
        }
    }

    /**
     * 网关安全异常审计配置项。
     */
    public static class SecurityAudit {

        /**
         * 是否启用网关侧安全审计。
         */
        private boolean enabled = true;

        /**
         * 审计服务在注册中心中的服务名。
         */
        private String auditServiceName = "xuan-audit";

        /**
         * 审计写入后台线程数。
         */
        private int poolSize = 2;

        /**
         * 审计写入 HTTP 超时时间，单位秒。
         */
        private long writeTimeoutSeconds = 2;

        /**
         * 无法从请求或 token 中解析租户时使用的平台系统租户 ID。
         */
        private Long systemTenantId = 0L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAuditServiceName() {
            return auditServiceName;
        }

        public void setAuditServiceName(String auditServiceName) {
            this.auditServiceName = auditServiceName;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public long getWriteTimeoutSeconds() {
            return writeTimeoutSeconds;
        }

        public void setWriteTimeoutSeconds(long writeTimeoutSeconds) {
            this.writeTimeoutSeconds = writeTimeoutSeconds;
        }

        public Long getSystemTenantId() {
            return systemTenantId;
        }

        public void setSystemTenantId(Long systemTenantId) {
            this.systemTenantId = systemTenantId;
        }
    }

    /**
     * 网关侧权限规则。
     */
    private List<PermissionRule> permissionRules = List.of();

    public List<PermissionRule> getPermissionRules() {
        return permissionRules;
    }

    public void setPermissionRules(List<PermissionRule> permissionRules) {
        this.permissionRules = permissionRules == null ? List.of() : permissionRules;
    }

    public static class PermissionRule {

        /**
         * 需要保护的路径表达式。
         */
        private List<String> paths = List.of();

        /**
         * 访问这些路径需要的权限码。
         */
        private String authority;

        /**
         * 访问这些路径允许使用的权限码列表。
         */
        private List<String> authorities = List.of();

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths == null ? List.of() : paths;
        }

        public String getAuthority() {
            return authority;
        }

        public void setAuthority(String authority) {
            this.authority = authority;
        }

        public List<String> getAuthorities() {
            if (authorities != null && !authorities.isEmpty()) {
                return authorities;
            }
            return authority == null || authority.isBlank() ? List.of() : List.of(authority);
        }

        public void setAuthorities(List<String> authorities) {
            this.authorities = authorities == null ? List.of() : authorities;
        }
    }
}
