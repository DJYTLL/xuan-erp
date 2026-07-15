package com.xuan.erp.common.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeAuditWritePublisherTest {

    @Test
    void acceptsAuditWriteWithoutCallingGatewayInline() {
        RecordingExecutor executor = new RecordingExecutor();
        SafeAuditWritePublisher publisher = new SafeAuditWritePublisher(
                event -> {
                    throw new AssertionError("审计远程调用不应该阻塞业务线程");
                },
                executor
        );

        AuditWriteSubmission submission = publisher.publish(event());

        assertEquals(AuditWriteSubmissionStatus.ACCEPTED, submission.status());
        assertEquals(1, executor.tasks.size());
    }

    @Test
    void swallowsGatewayFailureWhenAuditServiceIsUnavailable() {
        AtomicReference<Throwable> capturedFailure = new AtomicReference<>();
        SafeAuditWritePublisher publisher = new SafeAuditWritePublisher(
                event -> {
                    throw new IllegalStateException("xuan-audit unavailable");
                },
                Runnable::run,
                (event, failure) -> capturedFailure.set(failure)
        );

        assertDoesNotThrow(() -> publisher.publish(event()));

        assertEquals("xuan-audit unavailable", capturedFailure.get().getMessage());
    }

    @Test
    void degradesWhenExecutorRejectsAuditTask() {
        SafeAuditWritePublisher publisher = new SafeAuditWritePublisher(
                event -> new AuditWriteReceipt(1L),
                command -> {
                    throw new RejectedExecutionException("queue full");
                }
        );

        AuditWriteSubmission submission = publisher.publish(event());

        assertEquals(AuditWriteSubmissionStatus.DEGRADED, submission.status());
        assertEquals("queue full", submission.reason());
    }

    @Test
    void swallowsFailureHandlerExceptionToo() {
        SafeAuditWritePublisher publisher = new SafeAuditWritePublisher(
                event -> {
                    throw new IllegalStateException("audit down");
                },
                Runnable::run,
                (event, failure) -> {
                    throw new IllegalStateException("fallback handler down");
                }
        );

        assertDoesNotThrow(() -> publisher.publish(event()));
    }

    private AuditWriteEvent event() {
        return new AuditWriteEvent(
                1L,
                "admin",
                100L,
                "iam:user:create",
                "IamUser",
                "100",
                "{}",
                AuditWriteOutcome.SUCCESS,
                "req-001",
                "127.0.0.1",
                "Mozilla",
                42L,
                "POST",
                "/api/iam/users",
                200,
                null,
                null,
                1L,
                "default",
                false
        );
    }

    private static final class RecordingExecutor implements java.util.concurrent.Executor {

        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }
    }
}
