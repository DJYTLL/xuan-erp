package com.xuan.erp.iam.infrastructure.persistence.mapper;

import com.xuan.erp.iam.infrastructure.persistence.entity.IamRefreshTokenRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * IAM refresh token 持久化 Mapper，负责 iam_refresh_token 表的读写。
 */
@Mapper
public interface IamRefreshTokenPersistenceMapper {

    IamRefreshTokenRecord findByTokenHash(@Param("tokenHash") String tokenHash);

    IamRefreshTokenRecord findById(@Param("id") Long id);

    int insert(IamRefreshTokenRecord record);

    int update(IamRefreshTokenRecord record);
}
