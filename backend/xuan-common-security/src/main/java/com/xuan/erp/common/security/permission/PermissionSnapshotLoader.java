package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.security.CurrentUser;

/**
 * 权限快照加载器，负责从 IAM 或其它权限事实源拉取当前权限状态。
 */
@FunctionalInterface
public interface PermissionSnapshotLoader {

    /**
     * 加载当前用户权限快照。
     *
     * @param currentUser 当前用户
     * @param accessToken 当前访问令牌
     * @return 权限快照
     */
    PermissionSnapshot load(CurrentUser currentUser, String accessToken);
}
