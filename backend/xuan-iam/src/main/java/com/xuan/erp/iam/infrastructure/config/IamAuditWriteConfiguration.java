package com.xuan.erp.iam.infrastructure.config;

import com.xuan.erp.common.audit.AuditWriteGateway;
import com.xuan.erp.common.audit.SafeAuditWritePublisher;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IAM 审计写入配置，负责提供异步审计发布器和执行线程池。
 */
@Configuration
public class IamAuditWriteConfiguration {

    @Bean(destroyMethod = "shutdown")
    ExecutorService iamAuditWriteExecutor() {
        AtomicInteger sequence = new AtomicInteger();
        return Executors.newFixedThreadPool(2, task -> {
            Thread thread = new Thread(task, "iam-audit-write-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    SafeAuditWritePublisher iamAuditWritePublisher(
            AuditWriteGateway gateway,
            @Qualifier("iamAuditWriteExecutor") Executor executor
    ) {
        return new SafeAuditWritePublisher(gateway, executor);
    }
}
