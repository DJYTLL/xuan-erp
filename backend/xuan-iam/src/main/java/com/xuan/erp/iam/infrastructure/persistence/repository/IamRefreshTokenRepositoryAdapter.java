package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamRefreshToken;
import com.xuan.erp.iam.domain.repository.IamRefreshTokenRepository;
import com.xuan.erp.iam.infrastructure.persistence.assembler.IamRefreshTokenPersistenceAssembler;
import com.xuan.erp.iam.infrastructure.persistence.entity.IamRefreshTokenRecord;
import com.xuan.erp.iam.infrastructure.persistence.mapper.IamRefreshTokenPersistenceMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * IAM refresh token 仓储适配器，协调领域仓储端口、MyBatis Mapper 与持久化装配器。
 */
@Repository
public class IamRefreshTokenRepositoryAdapter implements IamRefreshTokenRepository {

    private final IamRefreshTokenPersistenceMapper mapper;

    public IamRefreshTokenRepositoryAdapter(IamRefreshTokenPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<IamRefreshToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(mapper.findByTokenHash(tokenHash))
                .map(IamRefreshTokenPersistenceAssembler::toDomain);
    }

    @Override
    public IamRefreshToken save(IamRefreshToken refreshToken) {
        IamRefreshTokenRecord record = IamRefreshTokenPersistenceAssembler.toRecord(refreshToken);
        if (refreshToken.id() == null) {
            mapper.insert(record);
            return findByTokenHash(refreshToken.tokenHash()).orElse(refreshToken);
        }
        mapper.update(record);
        return Optional.ofNullable(mapper.findById(refreshToken.id()))
                .map(IamRefreshTokenPersistenceAssembler::toDomain)
                .orElse(refreshToken);
    }
}
