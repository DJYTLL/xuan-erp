package com.xuan.erp.common.audit;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class SafeAuditWritePublisher {

    private final AuditWriteGateway gateway;
    private final Executor executor;
    private final AuditWriteFailureHandler failureHandler;

    public SafeAuditWritePublisher(AuditWriteGateway gateway, Executor executor) {
        this(gateway, executor, (event, failure) -> {
        });
    }

    public SafeAuditWritePublisher(
            AuditWriteGateway gateway,
            Executor executor,
            AuditWriteFailureHandler failureHandler
    ) {
        this.gateway = Objects.requireNonNull(gateway, "审计写入网关不能为空");
        this.executor = Objects.requireNonNull(executor, "审计写入执行器不能为空");
        this.failureHandler = Objects.requireNonNull(failureHandler, "审计降级处理器不能为空");
    }

    public AuditWriteSubmission publish(AuditWriteEvent event) {
        if (event == null) {
            return AuditWriteSubmission.degraded("audit event is null");
        }
        try {
            executor.execute(() -> writeSafely(event));
            return AuditWriteSubmission.accepted();
        } catch (RejectedExecutionException ex) {
            handleFailureSafely(event, ex);
            return AuditWriteSubmission.degraded(ex.getMessage());
        }
    }

    private void writeSafely(AuditWriteEvent event) {
        try {
            gateway.write(event);
        } catch (RuntimeException ex) {
            handleFailureSafely(event, ex);
        }
    }

    private void handleFailureSafely(AuditWriteEvent event, RuntimeException failure) {
        try {
            failureHandler.handle(event, failure);
        } catch (RuntimeException ignored) {
            // 审计降级处理本身也不能影响业务主流程。
        }
    }
}
