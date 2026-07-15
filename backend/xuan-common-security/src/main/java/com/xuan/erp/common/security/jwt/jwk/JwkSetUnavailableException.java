package com.xuan.erp.common.security.jwt.jwk;

/**
 * JWKS 认证基础设施不可用异常。
 *
 * <p>当远程 JWK Endpoint 不可访问、返回内容无法解析，且本地没有可用于验签的缓存时，调用方应把
 * 该异常转换为认证基础设施不可用响应，而不是默认放行请求。</p>
 */
public class JwkSetUnavailableException extends RuntimeException {

    public JwkSetUnavailableException(String message) {
        super(message);
    }

    public JwkSetUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
