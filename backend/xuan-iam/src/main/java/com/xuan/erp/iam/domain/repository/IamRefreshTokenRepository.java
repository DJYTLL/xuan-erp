package com.xuan.erp.iam.domain.repository;

import com.xuan.erp.iam.domain.model.IamRefreshToken;
import java.util.Optional;

/**
 * IAM refresh token 仓储端口，隔离哈希查询、持久化和轮换更新细节。
 */
public interface IamRefreshTokenRepository {

    Optional<IamRefreshToken> findByTokenHash(String tokenHash);

    IamRefreshToken save(IamRefreshToken refreshToken);
}
