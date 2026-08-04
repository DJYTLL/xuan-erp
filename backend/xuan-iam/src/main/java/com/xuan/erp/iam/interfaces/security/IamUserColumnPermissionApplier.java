package com.xuan.erp.iam.interfaces.security;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.CurrentUserHolder;
import com.xuan.erp.common.security.permission.ColumnAccess;
import com.xuan.erp.common.security.permission.ColumnMaskType;
import com.xuan.erp.common.security.permission.ColumnPermissionField;
import com.xuan.erp.common.security.permission.ColumnPermissionRecordApplier;
import com.xuan.erp.common.security.permission.PermissionSnapshot;
import com.xuan.erp.iam.application.query.IamCurrentPermissionSnapshotView;
import com.xuan.erp.iam.application.service.IamCurrentAuthorizationApplicationService;
import com.xuan.erp.iam.interfaces.dto.IamUserResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IAM 用户列表列权限裁剪器，负责在接口返回前应用当前用户最终列权限快照。
 */
@Component
public class IamUserColumnPermissionApplier {

    private static final String IAM_USER_RESOURCE = "iam-user";
    private static final List<ColumnPermissionField> IAM_USER_LIST_FIELDS = List.of(
            new ColumnPermissionField("username", "username", ColumnMaskType.NONE),
            new ColumnPermissionField("displayName", "displayName", ColumnMaskType.NONE),
            new ColumnPermissionField("phone", "phone", ColumnMaskType.PHONE),
            new ColumnPermissionField("email", "email", ColumnMaskType.EMAIL),
            new ColumnPermissionField("authVersion", "authVersion", ColumnMaskType.NONE),
            new ColumnPermissionField("status", "enabled", ColumnMaskType.NONE),
            new ColumnPermissionField("status", "accountNonLocked", ColumnMaskType.NONE));

    private final Function<CurrentUser, IamCurrentPermissionSnapshotView> snapshotLoader;
    private final ColumnPermissionRecordApplier recordApplier;

    @Autowired
    public IamUserColumnPermissionApplier(IamCurrentAuthorizationApplicationService currentAuthorizationApplicationService) {
        this(currentAuthorizationApplicationService::getCurrentPermissionSnapshot, new ColumnPermissionRecordApplier());
    }

    public IamUserColumnPermissionApplier(Function<CurrentUser, IamCurrentPermissionSnapshotView> snapshotLoader) {
        this(snapshotLoader, new ColumnPermissionRecordApplier());
    }

    IamUserColumnPermissionApplier(
            Function<CurrentUser, IamCurrentPermissionSnapshotView> snapshotLoader,
            ColumnPermissionRecordApplier recordApplier) {
        this.snapshotLoader = Objects.requireNonNull(snapshotLoader, "snapshotLoader must not be null");
        this.recordApplier = Objects.requireNonNull(recordApplier, "recordApplier must not be null");
    }

    public IamUserResponse applyToUserListRow(IamUserResponse row) {
        return applyToUserResponse(row);
    }

    public IamUserResponse applyToUserResponse(IamUserResponse row) {
        if (row == null) {
            return null;
        }
        return CurrentUserHolder.current()
                .map(currentUser -> recordApplier.apply(
                        toPermissionSnapshot(currentUser, snapshotLoader.apply(currentUser)),
                        IAM_USER_RESOURCE,
                        row,
                        IAM_USER_LIST_FIELDS))
                .orElse(row);
    }

    private PermissionSnapshot toPermissionSnapshot(CurrentUser currentUser, IamCurrentPermissionSnapshotView view) {
        if (view == null) {
            return PermissionSnapshot.empty(currentUser);
        }
        return new PermissionSnapshot(
                currentUser.tenantId(),
                currentUser.userId(),
                currentUser.username(),
                currentUser.roles(),
                currentUser.permissions() == null ? Set.of() : currentUser.permissions(),
                columnPermissions(view.columnPermissions()),
                view.authVersion() == null ? currentUser.authVersion() : view.authVersion());
    }

    private Map<String, Map<String, ColumnAccess>> columnPermissions(Map<String, Map<String, String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Map<String, ColumnAccess>> result = new LinkedHashMap<>();
        values.forEach((resourceKey, columns) -> {
            if (resourceKey == null || resourceKey.isBlank() || columns == null || columns.isEmpty()) {
                return;
            }
            Map<String, ColumnAccess> parsedColumns = new LinkedHashMap<>();
            columns.forEach((columnKey, access) -> {
                if (columnKey != null && !columnKey.isBlank()) {
                    parsedColumns.put(columnKey.trim(), ColumnAccess.parse(access));
                }
            });
            if (!parsedColumns.isEmpty()) {
                result.put(resourceKey.trim(), Map.copyOf(parsedColumns));
            }
        });
        return Map.copyOf(result);
    }
}
