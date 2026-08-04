package com.xuan.erp.common.security.jwt.jwk;

import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 提供业务服务访问 IAM JWKS Endpoint 所需的 URI。
 */
@FunctionalInterface
public interface JwkSetUriSupplier {

    /**
     * 获取当前应访问的 JWKS URI。
     *
     * @return JWKS URI
     */
    Mono<URI> get();
}
