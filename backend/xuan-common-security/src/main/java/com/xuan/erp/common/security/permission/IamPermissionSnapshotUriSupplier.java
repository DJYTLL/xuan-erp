package com.xuan.erp.common.security.permission;

import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 提供业务服务访问 IAM 当前权限快照接口所需的 URI。
 */
@FunctionalInterface
public interface IamPermissionSnapshotUriSupplier {

    /**
     * 获取当前应访问的 IAM 权限快照 URI。
     *
     * @return IAM 权限快照 URI
     */
    Mono<URI> get();
}
