package com.xuan.erp.iam.application.query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * IAM 授权快照查询视图，用于向接口层返回用户当前授权结果。
 */
public record IamAuthorizationSnapshotView(
        Long id,
        Long tenantId,
        Long userId,
        Long authVersion,
        List<Long> roleIds,
        List<String> permissionCodes,
        List<String> menuCodes,
        Map<String, Map<String, String>> columnSettings,
        String snapshotHash,
        OffsetDateTime builtAt
) {
}
