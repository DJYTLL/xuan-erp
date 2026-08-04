package com.xuan.erp.tenant.interfaces.security;

import com.xuan.erp.common.security.CurrentUser;
import com.xuan.erp.common.security.CurrentUserHolder;
import com.xuan.erp.common.security.permission.ColumnMaskType;
import com.xuan.erp.common.security.permission.ColumnPermissionField;
import com.xuan.erp.common.security.permission.ColumnPermissionRecordApplier;
import com.xuan.erp.common.security.permission.PermissionSnapshot;
import com.xuan.erp.common.security.permission.PermissionSnapshotProvider;
import com.xuan.erp.tenant.interfaces.dto.TenantResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 租户列表列权限裁剪器，负责在接口返回前应用 IAM 授权快照中的列权限规则。
 */
@Component
public class TenantColumnPermissionApplier {

    private static final String TENANT_RESOURCE = "tenant";
    private static final List<ColumnPermissionField> TENANT_LIST_FIELDS = List.of(
            new ColumnPermissionField("code", "code", ColumnMaskType.NONE),
            new ColumnPermissionField("name", "name", ColumnMaskType.NONE),
            new ColumnPermissionField("status", "status", ColumnMaskType.NONE),
            new ColumnPermissionField("currentPlanName", "currentPlanAssignmentId", ColumnMaskType.NONE),
            new ColumnPermissionField("currentPlanName", "currentPlanId", ColumnMaskType.NONE),
            new ColumnPermissionField("currentPlanName", "currentPlanCode", ColumnMaskType.NONE),
            new ColumnPermissionField("currentPlanName", "currentPlanName", ColumnMaskType.NONE),
            new ColumnPermissionField("currentPlanExpiresAt", "currentPlanExpiresAt", ColumnMaskType.NONE),
            new ColumnPermissionField("primaryDomain", "primaryDomainId", ColumnMaskType.NONE),
            new ColumnPermissionField("primaryDomain", "primaryDomain", ColumnMaskType.NONE),
            new ColumnPermissionField("contactName", "contactName", ColumnMaskType.NONE),
            new ColumnPermissionField("contactPhone", "contactPhone", ColumnMaskType.PHONE),
            new ColumnPermissionField("provisionedAt", "provisionedAt", ColumnMaskType.NONE),
            new ColumnPermissionField("remark", "remark", ColumnMaskType.NONE));

    private final PermissionSnapshotProvider permissionSnapshotProvider;
    private final ColumnPermissionRecordApplier recordApplier;

    @Autowired
    public TenantColumnPermissionApplier(PermissionSnapshotProvider permissionSnapshotProvider) {
        this(permissionSnapshotProvider, new ColumnPermissionRecordApplier());
    }

    TenantColumnPermissionApplier(
            PermissionSnapshotProvider permissionSnapshotProvider,
            ColumnPermissionRecordApplier recordApplier) {
        this.permissionSnapshotProvider = Objects.requireNonNull(permissionSnapshotProvider, "permissionSnapshotProvider must not be null");
        this.recordApplier = Objects.requireNonNull(recordApplier, "recordApplier must not be null");
    }

    public TenantResponse applyToTenantListRow(TenantResponse row) {
        if (row == null) {
            return null;
        }
        return CurrentUserHolder.current()
                .map(currentUser -> apply(row, permissionSnapshotProvider.load(currentUser, accessToken())))
                .orElse(row);
    }

    private TenantResponse apply(TenantResponse row, PermissionSnapshot snapshot) {
        return recordApplier.apply(snapshot, TENANT_RESOURCE, row, TENANT_LIST_FIELDS);
    }

    private String accessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getCredentials() instanceof String token)) {
            return null;
        }
        return token;
    }
}
