package com.xuan.erp.common.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.jwt.jwk.CachingJwkKeyProvider;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 JWK 公钥的 JWT 访问令牌解析器。
 *
 * <p>这是第一阶段默认实现：只接受使用 {@code RS256} 算法签名的 JWS，并通过本地传入的
 * {@link JWKSet} 中的 RSA 公钥完成验签。解析器不会从网络下载 JWK，也不会接受对称密钥或其它
 * 非 RSA 算法，便于在系统早期保持安全边界清晰、行为可预测。</p>
 *
 * <p>完整校验流程包括：解析 JWT、检查 header 中的 {@code alg} 和 {@code kid}、按 {@code kid}
 * 选择 JWK 公钥、验签、校验过期时间、issuer、audience 和 {@code tokenType=ACCESS}，最后把
 * 允许进入业务层的轻量身份字段映射为 {@link CurrentUser}。</p>
 *
 * <p>所有校验失败都会统一转换为 {@link JwtValidationException}，调用方可以通过
 * {@link JwtValidationException#reason()} 获取标准化原因。</p>
 */
public class JwkJwtTokenParser {

    /**
     * JWT 中标识令牌类型的 claim 名称。
     *
     * <p>业务接口只接受 {@link TokenType#ACCESS}。如果刷新令牌误传到业务接口，应被明确拒绝。</p>
     */
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    private final JWKSet jwkSet;
    private final CachingJwkKeyProvider jwkProvider;
    private final String issuer;
    private final String audience;
    private final Clock clock;

    /**
     * 创建 JWK JWT 解析器。
     *
     * @param jwkSet 可信 JWK 公钥集合，必须包含用于验签的 RSA 公钥
     * @param issuer 期望的 JWT 签发方
     * @param audience 期望的 JWT 受众
     */
    public JwkJwtTokenParser(JWKSet jwkSet, String issuer, String audience) {
        this(jwkSet, issuer, audience, Clock.systemUTC());
    }

    /**
     * 创建 JWK JWT 解析器，并允许注入时钟以便测试过期时间。
     *
     * @param jwkSet 可信 JWK 公钥集合，必须包含用于验签的 RSA 公钥
     * @param issuer 期望的 JWT 签发方
     * @param audience 期望的 JWT 受众
     * @param clock 当前时间来源；生产环境通常使用 UTC 系统时钟
     */
    public JwkJwtTokenParser(JWKSet jwkSet, String issuer, String audience, Clock clock) {
        this.jwkSet = Objects.requireNonNull(jwkSet, "jwkSet must not be null").toPublicJWKSet();
        this.jwkProvider = null;
        this.issuer = requireText(issuer, "issuer must not be blank");
        this.audience = requireText(audience, "audience must not be blank");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 创建基于缓存 JWK Provider 的 JWT 解析器。
     *
     * <p>业务服务自动配置使用该构造器。解析器会先从 JWT header 读取 {@code kid}，再通过
     * {@link CachingJwkKeyProvider} 获取包含目标公钥的 JWK Set，因此服务启动时不需要立即访问 IAM。</p>
     *
     * @param jwkProvider 可按 kid 获取 JWK Set 的缓存 Provider
     * @param issuer 期望的 JWT 签发方
     * @param audience 期望的 JWT 受众
     */
    public JwkJwtTokenParser(CachingJwkKeyProvider jwkProvider, String issuer, String audience) {
        this(jwkProvider, issuer, audience, Clock.systemUTC());
    }

    /**
     * 创建基于缓存 JWK Provider 的 JWT 解析器，并允许注入时钟以便测试过期时间。
     *
     * @param jwkProvider 可按 kid 获取 JWK Set 的缓存 Provider
     * @param issuer 期望的 JWT 签发方
     * @param audience 期望的 JWT 受众
     * @param clock 当前时间来源；生产环境通常使用 UTC 系统时钟
     */
    public JwkJwtTokenParser(CachingJwkKeyProvider jwkProvider, String issuer, String audience, Clock clock) {
        this.jwkSet = null;
        this.jwkProvider = Objects.requireNonNull(jwkProvider, "jwkProvider must not be null");
        this.issuer = requireText(issuer, "issuer must not be blank");
        this.audience = requireText(audience, "audience must not be blank");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 解析并校验访问令牌。
     *
     * <p>入参应为已经从 {@code Authorization: Bearer xxx} 中提取出的原始 token 字符串。该方法会
     * 完成第一阶段要求的 RS256/JWK 公钥验签和标准 claim 校验。校验通过后只返回 {@link CurrentUser}，
     * 不把 JWT 原始声明直接暴露给业务层。</p>
     *
     * @param token 原始 JWT access token
     * @return 当前登录用户上下文
     */
    public CurrentUser parseAccessToken(String token) {
        SignedJWT signedJwt = parse(token);
        JWSHeader header = signedJwt.getHeader();
        validateAlgorithm(header);
        String kid = requireKid(header);
        verifySignature(signedJwt, kid);
        JWTClaimsSet claimsSet = claims(signedJwt);
        validateExpiresAt(claimsSet.getExpirationTime());
        validateIssuer(claimsSet.getIssuer());
        validateAudience(claimsSet);
        validateTokenType(claimsSet);
        return currentUser(signedJwt, claimsSet);
    }

    /**
     * 把原始字符串解析为 Nimbus 的 SignedJWT。
     *
     * <p>这里不做任何信任判断，只负责格式解析。签名算法、kid、公钥和业务 claim 会在后续步骤中
     * 分别校验，方便把失败原因归类到更明确的 {@link JwtValidationException.Reason}。</p>
     */
    private SignedJWT parse(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.MALFORMED_TOKEN,
                    "JWT token must not be blank");
        }
        try {
            if (JWTParser.parse(token) instanceof SignedJWT signedJWT) {
                return signedJWT;
            }
            throw new JwtValidationException(
                    JwtValidationException.Reason.UNSUPPORTED_ALGORITHM,
                    "Unsigned JWT tokens are not supported");
        } catch (ParseException ex) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.MALFORMED_TOKEN,
                    "JWT token format is invalid",
                    ex);
        }
    }

    /**
     * 校验签名算法。
     *
     * <p>第一阶段只允许 RS256。拒绝其它算法可以避免降级到未评审的验签路径，例如 HS256、RS512
     * 或 {@code none}。未来如果要支持更多算法，应先扩展测试和配置模型。</p>
     */
    private void validateAlgorithm(JWSHeader header) {
        if (!JWSAlgorithm.RS256.equals(header.getAlgorithm())) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.UNSUPPORTED_ALGORITHM,
                    "Only RS256 JWT tokens are supported");
        }
    }

    /**
     * 读取并校验 JWT header 中的 kid。
     *
     * <p>{@code kid} 是从 JWK Set 中选择公钥的索引。缺少 kid 时即使 JWK Set 只有一把公钥，也不
     * 自动猜测，避免密钥轮换期间出现不可预期的验签行为。</p>
     */
    private String requireKid(JWSHeader header) {
        String kid = header.getKeyID();
        if (kid == null || kid.isBlank()) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.MISSING_KID,
                    "JWT header must contain kid");
        }
        return kid;
    }

    /**
     * 使用 kid 对应的 RSA 公钥验证 JWT 签名。
     *
     * <p>构造器会把传入的 JWK Set 转成 public-only 视图，因此这里不会保留或使用私钥。找不到
     * kid 对应公钥、JWK 不是 RSA 公钥、或者验签失败，都会归类为签名错误。</p>
     */
    private void verifySignature(SignedJWT signedJwt, String kid) {
        JWK jwk = jwkSet(kid).getKeyByKeyId(kid);
        if (!(jwk instanceof RSAKey rsaKey)) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.SIGNATURE_INVALID,
                    "No RSA public key found for JWT kid");
        }
        try {
            if (!signedJwt.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()))) {
                throw new JwtValidationException(
                        JwtValidationException.Reason.SIGNATURE_INVALID,
                        "JWT signature is invalid");
            }
        } catch (JOSEException ex) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.SIGNATURE_INVALID,
                    "JWT signature verification failed",
                    ex);
        }
    }

    /**
     * 根据 kid 取得本次验签使用的 JWK Set。
     *
     * <p>静态构造器直接使用本地 JWK Set；Provider 构造器则通过缓存 Provider 获取。这里保持同步接口，
     * 让 Servlet 业务服务和现有调用方都可以继续使用 {@link #parseAccessToken(String)}。</p>
     */
    private JWKSet jwkSet(String kid) {
        if (jwkProvider == null) {
            return jwkSet;
        }
        return jwkProvider.jwkSetForKid(kid).block();
    }

    /**
     * 读取 JWT payload 中的声明集合。
     *
     * <p>只有格式解析成功且签名已经校验通过后，才读取 claims 进入业务校验流程。</p>
     */
    private JWTClaimsSet claims(SignedJWT signedJwt) {
        try {
            return signedJwt.getJWTClaimsSet();
        } catch (ParseException ex) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.MALFORMED_TOKEN,
                    "JWT claims are invalid",
                    ex);
        }
    }

    /**
     * 校验过期时间。
     *
     * <p>访问令牌必须显式携带 exp，并且 exp 必须晚于当前时钟时间。这里不内置时钟偏移宽限，
     * 如果部署环境需要容忍少量时钟漂移，应在后续配置模型中显式加入。</p>
     */
    private void validateExpiresAt(Date expiresAt) {
        if (expiresAt == null || !expiresAt.toInstant().isAfter(Instant.now(clock))) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.TOKEN_EXPIRED,
                    "JWT token is expired");
        }
    }

    /**
     * 校验 issuer。
     *
     * <p>issuer 用于确认 token 来自当前系统信任的 IAM 签发方。不同环境或不同租户体系如果使用
     * 不同 issuer，应通过构造器传入明确值，而不是在这里做模糊匹配。</p>
     */
    private void validateIssuer(String actualIssuer) {
        if (!issuer.equals(actualIssuer)) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.ISSUER_INVALID,
                    "JWT issuer is invalid");
        }
    }

    /**
     * 校验 audience。
     *
     * <p>audience 用于确认 token 是签给当前资源服务或网关的。JWT 可以包含多个受众，只要包含
     * 当前配置值即可通过。</p>
     */
    private void validateAudience(JWTClaimsSet claimsSet) {
        if (claimsSet.getAudience() == null || !claimsSet.getAudience().contains(audience)) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.AUDIENCE_INVALID,
                    "JWT audience is invalid");
        }
    }

    /**
     * 校验 tokenType。
     *
     * <p>业务接口只能接受访问令牌。刷新令牌即使签名、issuer、audience 都正确，也必须在这里拒绝，
     * 防止刷新凭证被误用为接口访问凭证。</p>
     */
    private void validateTokenType(JWTClaimsSet claimsSet) {
        Object tokenType = claimsSet.getClaim(CLAIM_TOKEN_TYPE);
        if (!TokenType.ACCESS.name().equals(tokenType)) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.TOKEN_TYPE_INVALID,
                    "JWT tokenType must be ACCESS");
        }
    }

    /**
     * 把 Nimbus JWT 转成 Spring Security Jwt。
     *
     * <p>项目已有的 {@link JwtClaims#from(Jwt)} 基于 Spring Security 的 Jwt 类型提取身份字段。
     * 这里做一次轻量适配，可以复用同一套 claim 名称和类型转换规则，避免 JWK 解析器重复维护
     * 用户 ID、租户 ID、用户名和权限集合的解析逻辑。</p>
     */
    private Jwt toSpringJwt(SignedJWT signedJwt, JWTClaimsSet claimsSet) {
        Map<String, Object> headers = new LinkedHashMap<>(signedJwt.getHeader().toJSONObject());
        Map<String, Object> claims = new LinkedHashMap<>(claimsSet.getClaims());
        Instant issuedAt = claimsSet.getIssueTime() == null ? null : claimsSet.getIssueTime().toInstant();
        Instant expiresAt = claimsSet.getExpirationTime() == null ? null : claimsSet.getExpirationTime().toInstant();
        try {
            return new Jwt(signedJwt.serialize(), issuedAt, expiresAt, headers, claims);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.CLAIM_INVALID,
                    "JWT claims cannot be converted to current user",
                    ex);
        }
    }

    /**
     * 提取当前用户上下文。
     *
     * <p>签名和标准 claim 校验通过后，仍然可能因为缺少 userId、tenantId、username 或权限字段类型
     * 不合法而失败。这类错误统一归类为 {@link JwtValidationException.Reason#CLAIM_INVALID}。</p>
     */
    private CurrentUser currentUser(SignedJWT signedJwt, JWTClaimsSet claimsSet) {
        try {
            return JwtClaims.from(toSpringJwt(signedJwt, claimsSet)).toCurrentUser();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtValidationException(
                    JwtValidationException.Reason.CLAIM_INVALID,
                    "JWT identity claims are invalid",
                    ex);
        }
    }

    /**
     * 校验构造参数中的必填文本。
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
