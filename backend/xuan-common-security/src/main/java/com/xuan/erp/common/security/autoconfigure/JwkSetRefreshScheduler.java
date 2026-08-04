package com.xuan.erp.common.security.autoconfigure;

import com.xuan.erp.common.security.jwt.jwk.CachingJwkKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JWKS 后台刷新调度器。
 *
 * <p>该调度器只负责预热和续刷新本地 JWK 缓存。刷新失败不会清空旧缓存，也不会让应用退出，
 * 由 {@link CachingJwkKeyProvider} 在请求链路中决定是否还能使用陈旧缓存兜底。</p>
 */
public class JwkSetRefreshScheduler implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(JwkSetRefreshScheduler.class);

    private final CachingJwkKeyProvider jwkKeyProvider;
    private final Duration refreshInterval;
    private volatile boolean running;
    private ScheduledExecutorService executor;

    public JwkSetRefreshScheduler(CachingJwkKeyProvider jwkKeyProvider, Duration refreshInterval) {
        this.jwkKeyProvider = Objects.requireNonNull(jwkKeyProvider, "jwkKeyProvider must not be null");
        this.refreshInterval = requirePositive(refreshInterval);
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "xuan-jwk-refresh");
            thread.setDaemon(true);
            return thread;
        });
        long refreshMillis = Math.max(1L, refreshInterval.toMillis());
        executor.scheduleWithFixedDelay(this::refreshSafely, refreshMillis, refreshMillis, TimeUnit.MILLISECONDS);
        running = true;
    }

    @Override
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        running = false;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    private void refreshSafely() {
        try {
            jwkKeyProvider.refreshIfDue().block();
        } catch (RuntimeException exception) {
            log.warn("定期刷新 IAM JWKS 失败，将继续保留本地缓存: {}", exception.getMessage());
        }
    }

    private static Duration requirePositive(Duration duration) {
        Objects.requireNonNull(duration, "refreshInterval must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("refreshInterval must be positive");
        }
        return duration;
    }
}
