package com.xuan.erp.common.security.jwt.jwk;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteJwkSetFetcherTest {

    // 测试公共安全模块可以从远程 JWKS Endpoint 拉取并解析 IAM 发布的公钥集合。
    @Test
    void fetchesJwkSetFromRemoteEndpoint() throws Exception {
        RSAKey rsaKey = rsaKey("kid-1");
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        RemoteJwkSetFetcher fetcher = new RemoteJwkSetFetcher(
                uri -> {
                    requestedUrl.set(uri.toString());
                    return Mono.just(new JWKSet(rsaKey).toString());
                },
                URI.create("http://xuan-iam/.well-known/jwks.json"));

        JWKSet jwkSet = fetcher.fetch().block();

        assertThat(requestedUrl.get()).isEqualTo("http://xuan-iam/.well-known/jwks.json");
        assertThat(jwkSet).isNotNull();
        assertThat(jwkSet.getKeyByKeyId("kid-1")).isNotNull();
        assertThat(jwkSet.getKeyByKeyId("kid-1").isPrivate()).isFalse();
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
}
