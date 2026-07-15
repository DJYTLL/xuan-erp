package com.xuan.erp.common.security.jwt.jwk;

import com.nimbusds.jose.jwk.JWKSet;
import reactor.core.publisher.Mono;

/**
 * JWK Set 获取入口。
 *
 * <p>实现类可以从远程 IAM Endpoint、本地文件或其它安全密钥源加载公钥集合。调用方只依赖本接口，
 * 不需要关心公钥来自 HTTP、服务发现还是后续的密钥管理平台。</p>
 */
public interface JwkSetFetcher {

    /**
     * 获取当前可信的 JWK 公钥集合。
     *
     * @return 当前 JWK 公钥集合
     */
    Mono<JWKSet> fetch();
}
