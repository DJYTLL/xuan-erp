package com.xuan.erp.common.security.jwt;

import java.util.Objects;

/**
 * JWT 校验失败的统一异常。
 *
 * <p>解析 access token 时可能遇到多种失败场景，例如令牌已经过期、签名不可信、header 中缺少
 * {@code kid}、签发方不符合预期、受众不匹配，或者令牌类型不是访问令牌。业务层和网关层不应该
 * 依赖底层 JWT 库抛出的各种异常类型，而应该统一捕获本异常，并根据 {@link #reason()} 转换为
 * 认证失败响应、审计日志或监控指标。</p>
 *
 * <p>异常消息面向服务端排查使用，不建议原样返回给外部客户端，避免泄漏验签细节。对外响应可以
 * 统一返回“Token 无效”或“登录已过期”。</p>
 */
public class JwtValidationException extends RuntimeException {

    private final Reason reason;

    public JwtValidationException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public JwtValidationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    /**
     * 返回标准化失败原因。
     *
     * <p>调用方可以基于该枚举做日志分类、告警聚合或安全审计，而不需要解析异常消息文本。</p>
     *
     * @return JWT 校验失败原因
     */
    public Reason reason() {
        return reason;
    }

    /**
     * JWT 校验失败原因枚举。
     *
     * <p>枚举值尽量保持稳定，作为安全模块对外暴露的错误分类契约。新增失败场景时优先追加枚举值，
     * 不要随意重命名已有值，避免影响调用方的监控和异常处理逻辑。</p>
     */
    public enum Reason {
        /**
         * Token 字符串为空、格式不是 JWT，或无法被底层 JWT 库解析。
         */
        MALFORMED_TOKEN,

        /**
         * JWT header 中缺少 {@code kid}，无法从 JWK Set 中选择用于验签的公钥。
         */
        MISSING_KID,

        /**
         * JWT 使用了当前阶段不支持的签名算法。第一阶段只接受 RS256。
         */
        UNSUPPORTED_ALGORITHM,

        /**
         * 根据 {@code kid} 找不到对应公钥，或者签名验签失败。
         */
        SIGNATURE_INVALID,

        /**
         * Token 已过期。
         */
        TOKEN_EXPIRED,

        /**
         * Token 的 issuer 与当前服务信任的签发方不一致。
         */
        ISSUER_INVALID,

        /**
         * Token 的 audience 不包含当前资源服务或网关要求的受众。
         */
        AUDIENCE_INVALID,

        /**
         * Token 类型不是访问令牌，例如把刷新令牌误用于业务接口访问。
         */
        TOKEN_TYPE_INVALID,

        /**
         * Token 缺少用户 ID、租户 ID、用户名等业务层必需的身份字段，或字段类型不合法。
         */
        CLAIM_INVALID
    }
}
