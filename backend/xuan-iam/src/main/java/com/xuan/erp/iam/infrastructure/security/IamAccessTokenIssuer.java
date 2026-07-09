package com.xuan.erp.iam.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.JwtClaims;
import com.xuan.erp.common.security.jwt.TokenType;
import com.xuan.erp.iam.application.port.IamIssuedAccessToken;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 基于 RSA/JWK 的 IAM 访问令牌签发器。
 */
public class IamAccessTokenIssuer implements com.xuan.erp.iam.application.port.IamAccessTokenIssuer {

    private final RSAKey signingKey;
    private final String issuer;
    private final String audience;
    private final Duration accessTokenTtl;

    public IamAccessTokenIssuer(RSAKey signingKey, String issuer, String audience, Duration accessTokenTtl) {
        this.signingKey = signingKey;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtl = accessTokenTtl;
    }

    @Override
    public IamIssuedAccessToken issue(CurrentUser currentUser) {
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = issuedAt.plus(accessTokenTtl);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(issuedAt.toInstant()))
                .expirationTime(Date.from(expiresAt.toInstant()))
                .claim(JwkJwtTokenParser.CLAIM_TOKEN_TYPE, TokenType.ACCESS.name())
                .claim(JwtClaims.CLAIM_USER_ID, currentUser.userId())
                .claim(JwtClaims.CLAIM_TENANT_ID, currentUser.tenantId())
                .claim(JwtClaims.CLAIM_USERNAME, currentUser.username())
                .claim(JwtClaims.CLAIM_ROLES, currentUser.roles().stream().sorted().toList())
                .claim(JwtClaims.CLAIM_AUTH_VERSION, currentUser.authVersion())
                .claim(JwtClaims.CLAIM_PERMISSIONS, currentUser.permissions().stream().sorted().toList())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(signingKey.getKeyID())
                .build(), claims);
        try {
            jwt.sign(new RSASSASigner(signingKey));
        } catch (JOSEException ex) {
            throw new IllegalStateException("IAM access token 签名失败", ex);
        }
        return new IamIssuedAccessToken(jwt.serialize(), expiresAt.withOffsetSameInstant(ZoneOffset.ofHours(8)));
    }

    @Override
    public Map<String, Object> publicJwkSet() {
        return new JWKSet(signingKey.toPublicJWK()).toJSONObject();
    }
}
