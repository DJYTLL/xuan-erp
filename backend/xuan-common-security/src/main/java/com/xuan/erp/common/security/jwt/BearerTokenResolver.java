package com.xuan.erp.common.security.jwt;

import java.util.Optional;

/**
 * Bearer Token 提取器。
 *
 * <p>HTTP 规范中访问令牌通常放在 {@code Authorization} 请求头里，格式为
 * {@code Authorization: Bearer xxx}。本类只负责从请求头字符串中剥离 {@code Bearer} 前缀，
 * 返回原始 token 字符串，不负责验签、过期时间校验或权限解析。</p>
 *
 * <p>把提取逻辑独立出来，可以避免过滤器、拦截器和测试代码重复编写字符串处理规则。无效或空白
 * 请求头会返回 {@link Optional#empty()}，由调用方决定是否返回 401。</p>
 */
public class BearerTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 从 Authorization 请求头中提取 Bearer token。
     *
     * <p>当前实现要求前缀大小写和标准格式一致，即必须以 {@code Bearer } 开头，并且前缀后必须
     * 有非空 token。这样可以保持入口解析规则简单明确，避免把不规范的认证头静默当作有效凭证。</p>
     *
     * @param authorizationHeader HTTP Authorization 请求头原始值
     * @return 提取到的 token；请求头为空、不是 Bearer 格式或 token 为空时返回空
     */
    public Optional<String> resolve(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(token);
    }
}
