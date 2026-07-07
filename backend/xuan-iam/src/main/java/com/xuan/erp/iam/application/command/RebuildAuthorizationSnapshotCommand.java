package com.xuan.erp.iam.application.command;

import java.util.List;

/**
 * 重建用户授权快照的应用命令，包含角色、权限和菜单授权结果。
 */
public record RebuildAuthorizationSnapshotCommand(
        Long tenantId,
        Long userId,
        Long authVersion,
        List<Long> roleIds,
        List<String> permissionCodes,
        List<String> menuCodes
) {
}
