package com.xuan.erp.iam.infrastructure.persistence.repository;

import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
/**
 * IAM 授权快照 JDBC 仓储适配器，承接领域仓储端口与数据库访问。
 */
public class JdbcIamAuthorizationSnapshotRepository implements IamAuthorizationSnapshotRepository {

    @Override
    public Optional<IamAuthorizationSnapshot> findByTenantIdAndUserId(Long tenantId, Long userId) {
        return Optional.empty();
    }

    @Override
    public IamAuthorizationSnapshot save(IamAuthorizationSnapshot snapshot) {
        OffsetDateTime now = snapshot.builtAt() == null ? OffsetDateTime.now() : snapshot.builtAt();
        return new IamAuthorizationSnapshot(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.userId(),
                snapshot.authVersion(),
                snapshot.roleIds() == null ? List.of() : snapshot.roleIds(),
                snapshot.permissionCodes() == null ? List.of() : snapshot.permissionCodes(),
                snapshot.menuCodes() == null ? List.of() : snapshot.menuCodes(),
                snapshot.columnSettings() == null ? Map.of() : snapshot.columnSettings(),
                snapshot.snapshotHash(),
                snapshot.expiresAt(),
                now,
                snapshot.createdBy(),
                snapshot.createdAt(),
                snapshot.updatedBy(),
                snapshot.updatedAt());
    }
}
