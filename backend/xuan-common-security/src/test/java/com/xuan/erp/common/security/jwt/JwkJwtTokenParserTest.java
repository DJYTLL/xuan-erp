package com.xuan.erp.common.security.jwt;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.xuan.erp.common.security.CurrentUser;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class JwkJwtTokenParserTest {

    private static final String ISSUER = "https://iam.xuan.local";
    private static final String AUDIENCE = "xuan-erp-api";
    private static final Instant NOW = Instant.parse("2026-07-03T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    // 测试合法 RS256 access token 可以通过 JWK 公钥验签，并解析出当前用户身份字段。
    @Test
    void parsesValidRs256AccessTokenSignedByJwkPublicKey() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(rsaKey);

        CurrentUser currentUser = parser.parseAccessToken(signedToken(rsaKey, JWSAlgorithm.RS256, "kid-1",
                NOW.plusSeconds(300), ISSUER, List.of(AUDIENCE), TokenType.ACCESS.name()));

        assertThat(currentUser.userId()).isEqualTo(1001L);
        assertThat(currentUser.tenantId()).isEqualTo(2002L);
        assertThat(currentUser.username()).isEqualTo("zhangsan");
        assertThat(currentUser.roles()).containsExactlyInAnyOrder("tenant_admin", "sales_manager");
        assertThat(currentUser.authVersion()).isEqualTo(7L);
        assertThat(currentUser.permissions()).containsExactlyInAnyOrder("sales:view", "sales:create");
    }

    // 测试 JWT header 缺少 kid 时拒绝解析，避免无法确定验签公钥。
    @Test
    void rejectsTokenWithoutKid() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(rsaKey);

        JwtValidationException exception = jwtValidationException(
                () -> parser.parseAccessToken(signedToken(rsaKey, JWSAlgorithm.RS256, null,
                        NOW.plusSeconds(300), ISSUER, List.of(AUDIENCE), TokenType.ACCESS.name())));

        assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.MISSING_KID);
    }

    // 测试非 RS256 的 RSA 算法会被拒绝，第一阶段不接受 RS512 等其它算法。
    @Test
    void rejectsNonRs256Algorithm() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(rsaKey);

        JwtValidationException exception = jwtValidationException(
                () -> parser.parseAccessToken(signedToken(rsaKey, JWSAlgorithm.RS512, "kid-1",
                        NOW.plusSeconds(300), ISSUER, List.of(AUDIENCE), TokenType.ACCESS.name())));

        assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.UNSUPPORTED_ALGORITHM);
    }

    // 测试 alg=none 的未签名 token 会被拒绝，不能绕过签名校验。
    @Test
    void rejectsNoneAlgorithm() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(rsaKey);

        JwtValidationException exception = jwtValidationException(
                () -> parser.parseAccessToken(unsignedToken("kid-1", NOW.plusSeconds(300), ISSUER,
                        List.of(AUDIENCE), TokenType.ACCESS.name())));

        assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.UNSUPPORTED_ALGORITHM);
    }

    // 测试 HS256 对称签名 token 会被拒绝，当前解析器只接受 RS256/JWK 公钥验签。
    @Test
    void rejectsHs256Algorithm() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(rsaKey);

        JwtValidationException exception = jwtValidationException(
                () -> parser.parseAccessToken(hs256Token("kid-1", NOW.plusSeconds(300), ISSUER,
                        List.of(AUDIENCE), TokenType.ACCESS.name())));

        assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.UNSUPPORTED_ALGORITHM);
    }

    // 测试已过期 token 会被拒绝，防止过期访问令牌继续访问业务接口。
    @Test
    void rejectsExpiredToken() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(rsaKey);

        JwtValidationException exception = jwtValidationException(
                () -> parser.parseAccessToken(signedToken(rsaKey, JWSAlgorithm.RS256, "kid-1",
                        NOW.minusSeconds(1), ISSUER, List.of(AUDIENCE), TokenType.ACCESS.name())));

        assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.TOKEN_EXPIRED);
    }

    // 测试 issuer 不匹配时拒绝解析，确保 token 来自受信任的 IAM 签发方。
    @Test
    void rejectsInvalidIssuer() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(rsaKey);

        JwtValidationException exception = jwtValidationException(
                () -> parser.parseAccessToken(signedToken(rsaKey, JWSAlgorithm.RS256, "kid-1",
                        NOW.plusSeconds(300), "https://other-issuer.local", List.of(AUDIENCE), TokenType.ACCESS.name())));

        assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.ISSUER_INVALID);
    }

    // 测试 audience 不匹配时拒绝解析，确保 token 是签给当前资源服务或网关的。
    @Test
    void rejectsInvalidAudience() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(rsaKey);

        JwtValidationException exception = jwtValidationException(
                () -> parser.parseAccessToken(signedToken(rsaKey, JWSAlgorithm.RS256, "kid-1",
                        NOW.plusSeconds(300), ISSUER, List.of("other-api"), TokenType.ACCESS.name())));

        assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.AUDIENCE_INVALID);
    }

    // 测试 tokenType 不是 ACCESS 时拒绝解析，避免刷新令牌被当作访问令牌使用。
    @Test
    void rejectsInvalidTokenType() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(rsaKey);

        JwtValidationException exception = jwtValidationException(
                () -> parser.parseAccessToken(signedToken(rsaKey, JWSAlgorithm.RS256, "kid-1",
                        NOW.plusSeconds(300), ISSUER, List.of(AUDIENCE), TokenType.REFRESH.name())));

        assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.TOKEN_TYPE_INVALID);
    }

    // 测试缺少必要身份 claim 时统一包装为 CLAIM_INVALID。
    @Test
    void wrapsMissingIdentityClaimAsInvalidClaim() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(rsaKey);

        JwtValidationException exception = jwtValidationException(
                () -> parser.parseAccessToken(signedToken(rsaKey, JWSAlgorithm.RS256, "kid-1",
                        NOW.plusSeconds(300), ISSUER, List.of(AUDIENCE), TokenType.ACCESS.name(), false)));

        assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.CLAIM_INVALID);
    }

    // 测试签名与 JWK 公钥不匹配时拒绝解析，防止伪造 token 通过验签。
    @Test
    void rejectsInvalidSignature() throws Exception {
        RSAKey trustedKey = rsaKey("kid-1");
        RSAKey signingKey = rsaKey("kid-1");
        JwkJwtTokenParser parser = parser(trustedKey);

        JwtValidationException exception = jwtValidationException(
                () -> parser.parseAccessToken(signedToken(signingKey, JWSAlgorithm.RS256, "kid-1",
                        NOW.plusSeconds(300), ISSUER, List.of(AUDIENCE), TokenType.ACCESS.name())));

        assertThat(exception.reason()).isEqualTo(JwtValidationException.Reason.SIGNATURE_INVALID);
    }

    private static JwkJwtTokenParser parser(RSAKey rsaKey) {
        return new JwkJwtTokenParser(new JWKSet(rsaKey.toPublicJWK()), ISSUER, AUDIENCE, CLOCK);
    }

    private static RSAKey rsaKey(String kid) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .algorithm(JWSAlgorithm.RS256)
                .keyID(kid)
                .build();
    }

    private static JwtValidationException jwtValidationException(ThrowingOperation operation) {
        Throwable throwable = catchThrowable(operation::run);
        assertThat(throwable).isInstanceOf(JwtValidationException.class);
        return (JwtValidationException) throwable;
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws Exception;
    }

    private static String signedToken(
            RSAKey rsaKey,
            JWSAlgorithm algorithm,
            String kid,
            Instant expiresAt,
            String issuer,
            List<String> audience,
            String tokenType) throws Exception {
        return signedToken(rsaKey, algorithm, kid, expiresAt, issuer, audience, tokenType, true);
    }

    private static String signedToken(
            RSAKey rsaKey,
            JWSAlgorithm algorithm,
            String kid,
            Instant expiresAt,
            String issuer,
            List<String> audience,
            String tokenType,
            boolean includeUserId) throws Exception {
        JWTClaimsSet claims = claims(expiresAt, issuer, audience, tokenType, includeUserId);
        JWSHeader.Builder header = new JWSHeader.Builder(algorithm).type(JOSEObjectType.JWT);
        if (kid != null) {
            header.keyID(kid);
        }
        SignedJWT signedJWT = new SignedJWT(header.build(), claims);
        signedJWT.sign(new RSASSASigner(rsaKey));
        return signedJWT.serialize();
    }

    private static String unsignedToken(
            String kid,
            Instant expiresAt,
            String issuer,
            List<String> audience,
            String tokenType) {
        JWTClaimsSet claims = claims(expiresAt, issuer, audience, tokenType, true);
        PlainHeader.Builder header = new PlainHeader.Builder().type(JOSEObjectType.JWT);
        return new PlainJWT(header.build(), claims).serialize();
    }

    private static String hs256Token(
            String kid,
            Instant expiresAt,
            String issuer,
            List<String> audience,
            String tokenType) throws Exception {
        JWTClaimsSet claims = claims(expiresAt, issuer, audience, tokenType, true);
        JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT);
        if (kid != null) {
            header.keyID(kid);
        }
        SignedJWT signedJWT = new SignedJWT(header.build(), claims);
        signedJWT.sign(new MACSigner("01234567890123456789012345678901"));
        return signedJWT.serialize();
    }

    private static JWTClaimsSet claims(
            Instant expiresAt,
            String issuer,
            List<String> audience,
            String tokenType,
            boolean includeUserId) {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(NOW))
                .claim(JwkJwtTokenParser.CLAIM_TOKEN_TYPE, tokenType)
                .claim(JwtClaims.CLAIM_USER_ID, includeUserId ? 1001L : null)
                .claim(JwtClaims.CLAIM_TENANT_ID, 2002L)
                .claim(JwtClaims.CLAIM_USERNAME, "zhangsan")
                .claim(JwtClaims.CLAIM_ROLES, List.of("tenant_admin", "sales_manager"))
                .claim(JwtClaims.CLAIM_AUTH_VERSION, 7L)
                .claim(JwtClaims.CLAIM_PERMISSIONS, List.of("sales:view", "sales:create"))
                .build();
    }
}
