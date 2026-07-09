package com.xuan.erp.iam.application.port;

import com.xuan.erp.common.security.CurrentUser;
import java.util.Map;

/**
 * IAM 访问令牌签发端口，隔离登录用例与具体 JWT/JWK 技术实现。
 */
public interface IamAccessTokenIssuer {

    /**
     * 按当前用户上下文签发访问令牌。
     *
     * @param currentUser 已通过账号校验的当前用户
     * @return 已签发的访问令牌及过期时间
     */
    IamIssuedAccessToken issue(CurrentUser currentUser);

    /**
     * 返回对外发布的公钥 JWK Set。
     *
     * @return 仅包含公钥字段的 JWK Set JSON 结构
     */
    Map<String, Object> publicJwkSet();
}
