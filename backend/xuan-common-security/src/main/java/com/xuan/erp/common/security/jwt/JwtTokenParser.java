package com.xuan.erp.common.security.jwt;

import com.xuan.erp.common.security.CurrentUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Objects;

/**
 * JWT 令牌解析入口。
 *
 * <p>本类只负责调用已配置好的 {@link JwtDecoder} 完成令牌解析，并把访问令牌中的用户上下文
 * 映射为系统内部使用的 {@link CurrentUser}。</p>
 *
 * <p>签名算法、密钥来源、issuer、audience、过期时间等安全校验应由注入的 {@link JwtDecoder}
 * 负责配置和执行。本类不直接读取密钥，也不自行判断 token 是否过期，避免公共解析入口和具体部署
 * 环境绑定过深。</p>
 *
 * <p>解析结果会经过 {@link JwtClaims} 过滤，只保留系统允许进入业务层的轻量身份字段。即使 JWT
 * 中包含其它自定义 claim，也不会通过 {@link #parseAccessToken(String)} 暴露给业务代码。</p>
 */
public class JwtTokenParser {

    private final JwtDecoder jwtDecoder;

    /**
     * 创建 JWT 解析入口。
     *
     * @param jwtDecoder Spring Security JWT 解码器，负责完成签名、过期时间和标准声明校验
     */
    public JwtTokenParser(JwtDecoder jwtDecoder) {
        this.jwtDecoder = Objects.requireNonNull(jwtDecoder, "jwtDecoder must not be null");
    }

    /**
     * 解析访问令牌，并返回当前登录用户上下文。
     *
     * <p>该方法面向 access token 使用场景。调用方传入的 token 应是不带 {@code Bearer } 前缀的
     * 原始令牌字符串；如果上游拿到的是 HTTP Authorization 头，应先在过滤器或网关层剥离前缀。</p>
     *
     * <p>当令牌格式错误、签名不可信、令牌过期或缺少必要身份字段时，会沿用 Spring Security
     * {@code JwtException} 体系抛出异常，由调用方统一转换为 401 或认证失败响应。</p>
     *
     * @param token 原始 access token 字符串
     * @return 当前用户上下文
     */
    public CurrentUser parseAccessToken(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return JwtClaims.from(jwt).toCurrentUser();
    }
}
