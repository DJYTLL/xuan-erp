package com.xuan.erp.common.security.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

/**
 * 业务服务权限快照与方法级授权配置。
 */
@ConfigurationProperties(prefix = "xuan.security.permission")
@Validated
public class XuanPermissionSecurityProperties {

    /**
     * 是否启用业务服务权限快照远程加载和统一权限表达式。
     */
    private boolean enabled = false;

    /**
     * IAM 当前权限快照接口地址。
     */
    private URI iamSnapshotUri;

    /**
     * IAM 服务名；业务服务可通过服务发现解析真实实例地址。
     */
    private String iamServiceName = "xuan-iam";

    /**
     * IAM 当前权限快照接口标准路径。
     */
    private String iamSnapshotPath = "/api/iam/permissions/current";

    /**
     * 权限快照缓存配置。
     */
    private Cache cache = new Cache();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public URI getIamSnapshotUri() {
        return iamSnapshotUri;
    }

    public void setIamSnapshotUri(URI iamSnapshotUri) {
        this.iamSnapshotUri = iamSnapshotUri;
    }

    public String getIamServiceName() {
        return iamServiceName;
    }

    public void setIamServiceName(String iamServiceName) {
        this.iamServiceName = iamServiceName;
    }

    public String getIamSnapshotPath() {
        return iamSnapshotPath;
    }

    public void setIamSnapshotPath(String iamSnapshotPath) {
        this.iamSnapshotPath = iamSnapshotPath;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache == null ? new Cache() : cache;
    }

    /**
     * 权限快照缓存配置。
     */
    public static class Cache {

        /**
         * 权限快照本地缓存 TTL。
         */
        private Duration ttl = Duration.ofSeconds(30);

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}
