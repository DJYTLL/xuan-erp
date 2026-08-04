package com.xuan.erp.iam.application.service;

import com.xuan.erp.common.exception.BusinessException;
import com.xuan.erp.iam.application.command.RebuildAuthorizationSnapshotCommand;
import com.xuan.erp.iam.application.query.IamAuthorizationSnapshotView;
import com.xuan.erp.iam.domain.model.IamAuthorizationSnapshot;
import com.xuan.erp.iam.domain.repository.IamAuthorizationSnapshotRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
/**
 * IAM 授权应用服务，负责查询和重建用户授权快照。
 */
public class IamAuthorizationApplicationService {

    private final IamAuthorizationSnapshotRepository snapshotRepository;

    public IamAuthorizationApplicationService(IamAuthorizationSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public IamAuthorizationSnapshotView getSnapshot(Long tenantId, Long userId) {
        return snapshotRepository.findByTenantIdAndUserId(tenantId, userId)
                .map(this::toView)
                .orElseThrow(() -> new BusinessException("IAM_AUTHORIZATION_SNAPSHOT_NOT_FOUND", "授权快照不存在"));
    }

    public IamAuthorizationSnapshotView rebuildSnapshot(RebuildAuthorizationSnapshotCommand command) {
        requirePositive(command.tenantId(), "租户 ID 不能为空");
        requirePositive(command.userId(), "用户 ID 不能为空");
        List<Long> roleIds = normalizeLongs(command.roleIds());
        List<String> permissionCodes = normalizeStrings(command.permissionCodes());
        List<String> menuCodes = normalizeStrings(command.menuCodes());
        OffsetDateTime now = OffsetDateTime.now();
        IamAuthorizationSnapshot saved = snapshotRepository.save(new IamAuthorizationSnapshot(
                null,
                command.tenantId(),
                command.userId(),
                command.authVersion() == null ? 0L : command.authVersion(),
                roleIds,
                permissionCodes,
                menuCodes,
                Map.of(),
                IamAuthorizationSnapshotHash.from(roleIds, permissionCodes, menuCodes),
                null,
                now,
                "system",
                now,
                "system",
                now));
        return toView(saved);
    }

    private IamAuthorizationSnapshotView toView(IamAuthorizationSnapshot snapshot) {
        return new IamAuthorizationSnapshotView(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.userId(),
                snapshot.authVersion(),
                snapshot.roleIds(),
                snapshot.permissionCodes(),
                snapshot.menuCodes(),
                snapshot.columnSettings(),
                snapshot.snapshotHash(),
                snapshot.builtAt());
    }

    private List<Long> normalizeLongs(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && value > 0)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> normalizeStrings(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(LinkedHashSet<String>::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .stream()
                .sorted()
                .toList();
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException("IAM_INVALID_ARGUMENT", message);
        }
    }
}
