package com.xuan.erp.common.security.permission;

/**
 * 业务服务最终使用的权限快照 Provider。
 *
 * <p>该接口通常由缓存实现承载，对上屏蔽远程 IAM 调用细节。</p>
 */
@FunctionalInterface
public interface PermissionSnapshotProvider extends PermissionSnapshotLoader {
}
