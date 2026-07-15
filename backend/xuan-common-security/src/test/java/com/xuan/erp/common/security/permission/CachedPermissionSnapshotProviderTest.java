package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.security.CurrentUser;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权限快照缓存测试，验证业务服务不会每个请求都打 IAM。
 */
class CachedPermissionSnapshotProviderTest {

    // 测试同一租户、用户和权限版本命中缓存，只远程加载一次。
    @Test
    void cachesSnapshotByTenantUserAndAuthVersion() {
        AtomicInteger loads = new AtomicInteger();
        PermissionSnapshotProvider delegate = (currentUser, accessToken) -> {
            loads.incrementAndGet();
            return new PermissionSnapshot(
                    currentUser.tenantId(),
                    currentUser.userId(),
                    currentUser.username(),
                    currentUser.roles(),
                    Set.of("tenant:create"),
                    currentUser.authVersion());
        };
        CachedPermissionSnapshotProvider provider = new CachedPermissionSnapshotProvider(
                delegate,
                Duration.ofMinutes(5),
                Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC));
        CurrentUser currentUser = new CurrentUser(7L, 1001L, "tenant-admin", Set.of("tenant_admin"), 5L, Set.of());

        PermissionSnapshot first = provider.load(currentUser, "token-a");
        PermissionSnapshot second = provider.load(currentUser, "token-a");

        assertThat(first).isSameAs(second);
        assertThat(loads).hasValue(1);
    }

    // 测试 authVersion 变化会重新加载权限快照，避免旧权限继续生效。
    @Test
    void reloadsSnapshotWhenAuthVersionChanges() {
        AtomicInteger loads = new AtomicInteger();
        PermissionSnapshotProvider delegate = (currentUser, accessToken) -> {
            loads.incrementAndGet();
            return new PermissionSnapshot(
                    currentUser.tenantId(),
                    currentUser.userId(),
                    currentUser.username(),
                    currentUser.roles(),
                    Set.of("tenant:create"),
                    currentUser.authVersion());
        };
        CachedPermissionSnapshotProvider provider = new CachedPermissionSnapshotProvider(
                delegate,
                Duration.ofMinutes(5),
                Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC));

        provider.load(new CurrentUser(7L, 1001L, "tenant-admin", Set.of(), 5L, Set.of()), "token-a");
        provider.load(new CurrentUser(7L, 1001L, "tenant-admin", Set.of(), 6L, Set.of()), "token-b");

        assertThat(loads).hasValue(2);
    }
}
