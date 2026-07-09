package com.xuan.erp.iam;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.common.security.jwt.TokenType;
import com.xuan.erp.iam.infrastructure.security.IamBearerTokenAuthenticationFilter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class IamBearerTokenAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesBearerTokenIntoSecurityContext() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        Clock clock = Clock.fixed(Instant.parse("2026-07-08T01:00:00Z"), ZoneOffset.UTC);
        IamBearerTokenAuthenticationFilter filter = new IamBearerTokenAuthenticationFilter(
                new JwkJwtTokenParser(new JWKSet(rsaKey.toPublicJWK()), "xuan-iam", "xuan-gateway", clock));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + signedToken(rsaKey));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        CurrentUser currentUser = assertInstanceOf(CurrentUser.class, authentication.getPrincipal());
        assertEquals(1L, currentUser.userId());
        assertEquals("admin", currentUser.username());
        assertEquals(1, authentication.getAuthorities().size());
        assertEquals("iam:view", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void skipsRequestWithoutBearerToken() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        Clock clock = Clock.fixed(Instant.parse("2026-07-08T01:00:00Z"), ZoneOffset.UTC);
        IamBearerTokenAuthenticationFilter filter = new IamBearerTokenAuthenticationFilter(
                new JwkJwtTokenParser(new JWKSet(rsaKey.toPublicJWK()), "xuan-iam", "xuan-gateway", clock));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private static String signedToken(RSAKey rsaKey) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("xuan-iam")
                .audience("xuan-gateway")
                .issueTime(Date.from(Instant.parse("2026-07-08T00:55:00Z")))
                .expirationTime(Date.from(Instant.parse("2026-07-08T02:00:00Z")))
                .claim("tokenType", TokenType.ACCESS.name())
                .claim("userId", 1L)
                .claim("tenantId", 1001L)
                .claim("username", "admin")
                .claim("roles", java.util.List.of())
                .claim("authVersion", 7L)
                .claim("permissions", java.util.List.of("iam:view"))
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("kid-1").build(), claims);
        signedJWT.sign(new RSASSASigner(rsaKey));
        return signedJWT.serialize();
    }
}
