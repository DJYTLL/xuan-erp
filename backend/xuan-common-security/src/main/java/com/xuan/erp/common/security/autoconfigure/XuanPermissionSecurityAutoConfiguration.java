package com.xuan.erp.common.security.autoconfigure;

import com.xuan.erp.common.security.permission.CachedPermissionSnapshotProvider;
import com.xuan.erp.common.security.permission.PermissionSnapshotProvider;
import com.xuan.erp.common.security.permission.RemoteIamPermissionSnapshotProvider;
import com.xuan.erp.common.security.permission.XuanPermissionExpression;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

/**
 * 业务服务权限快照和方法级权限自动配置。
 */
@AutoConfiguration(after = XuanJwtSecurityAutoConfiguration.class)
@ConditionalOnClass({WebClient.class, EnableMethodSecurity.class})
@EnableConfigurationProperties(XuanPermissionSecurityProperties.class)
@EnableMethodSecurity
@ConditionalOnProperty(prefix = "xuan.security.permission", name = "enabled", havingValue = "true")
public class XuanPermissionSecurityAutoConfiguration {

    /**
     * 提供权限快照远程加载使用的 WebClient Builder。
     *
     * @return WebClient 构建器
     */
    @Bean
    @ConditionalOnMissingBean
    WebClient.Builder xuanPermissionWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * 创建 IAM 远程权限快照加载器。
     *
     * @param webClientBuilder WebClient 构建器
     * @param properties 权限配置
     * @return IAM 远程权限快照加载器
     */
    @Bean
    @ConditionalOnMissingBean
    RemoteIamPermissionSnapshotProvider remoteIamPermissionSnapshotProvider(
            WebClient.Builder webClientBuilder,
            XuanPermissionSecurityProperties properties) {
        return new RemoteIamPermissionSnapshotProvider(webClientBuilder.build(), requiredIamSnapshotUri(properties));
    }

    /**
     * 创建业务服务最终使用的缓存权限快照 Provider。
     *
     * @param remoteProvider IAM 远程权限快照加载器
     * @param properties 权限配置
     * @return 缓存权限快照 Provider
     */
    @Bean
    @ConditionalOnMissingBean(PermissionSnapshotProvider.class)
    CachedPermissionSnapshotProvider permissionSnapshotProvider(
            RemoteIamPermissionSnapshotProvider remoteProvider,
            XuanPermissionSecurityProperties properties) {
        return new CachedPermissionSnapshotProvider(remoteProvider, requiredCacheTtl(properties), Clock.systemUTC());
    }

    /**
     * 创建 SpEL 中使用的统一权限表达式 Bean。
     *
     * @param permissionSnapshotProvider 权限快照 Provider
     * @return 权限表达式 Bean
     */
    @Bean("xuanPermission")
    @ConditionalOnMissingBean(name = "xuanPermission")
    XuanPermissionExpression xuanPermission(PermissionSnapshotProvider permissionSnapshotProvider) {
        return new XuanPermissionExpression(permissionSnapshotProvider);
    }

    private URI requiredIamSnapshotUri(XuanPermissionSecurityProperties properties) {
        URI uri = properties.getIamSnapshotUri();
        if (uri == null || uri.toString().isBlank()) {
            throw new IllegalStateException("xuan.security.permission.iam-snapshot-uri 未配置");
        }
        return uri;
    }

    private Duration requiredCacheTtl(XuanPermissionSecurityProperties properties) {
        Duration ttl = properties.getCache().getTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("xuan.security.permission.cache.ttl 必须大于 0");
        }
        return ttl;
    }
}
