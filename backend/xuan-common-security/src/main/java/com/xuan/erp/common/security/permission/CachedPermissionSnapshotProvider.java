package com.xuan.erp.common.security.permission;

import com.xuan.erp.common.security.CurrentUser;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 带本地 TTL 缓存的权限快照 Provider。
 *
 * <p>缓存 key 包含租户、用户和 {@code authVersion}，用户权限版本变化后会自然重新加载。</p>
 */
public class CachedPermissionSnapshotProvider implements PermissionSnapshotProvider {

    private final PermissionSnapshotLoader delegate;
    private final Duration ttl;
    private final Clock clock;
    private final Map<Key, Entry> cache = new ConcurrentHashMap<>();

    public CachedPermissionSnapshotProvider(PermissionSnapshotLoader delegate, Duration ttl, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("permission snapshot cache ttl must be positive");
        }
        this.ttl = ttl;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public PermissionSnapshot load(CurrentUser currentUser, String accessToken) {
        Key key = Key.of(currentUser);
        Instant now = clock.instant();
        Entry existing = cache.get(key);
        if (existing != null && existing.expiresAt().isAfter(now)) {
            return existing.snapshot();
        }
        PermissionSnapshot snapshot = delegate.load(currentUser, accessToken);
        cache.put(key, new Entry(snapshot, now.plus(ttl)));
        return snapshot;
    }

    private record Key(Long tenantId, Long userId, Long authVersion) {

        private static Key of(CurrentUser currentUser) {
            return new Key(currentUser.tenantId(), currentUser.userId(), currentUser.authVersion());
        }
    }

    private record Entry(PermissionSnapshot snapshot, Instant expiresAt) {
    }
}
