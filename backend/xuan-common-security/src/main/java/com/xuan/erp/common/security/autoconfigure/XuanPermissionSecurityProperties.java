package com.xuan.erp.common.security.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * 业务服务权限快照与方法级授权配置。
 */
@ConfigurationProperties(prefix = "xuan.security.permission")
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
