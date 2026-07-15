package com.xuan.erp.common.audit;

public record AuditWriteSubmission(
        AuditWriteSubmissionStatus status,
        String reason
) {

    public static AuditWriteSubmission accepted() {
        return new AuditWriteSubmission(AuditWriteSubmissionStatus.ACCEPTED, null);
    }

    public static AuditWriteSubmission degraded(String reason) {
        return new AuditWriteSubmission(AuditWriteSubmissionStatus.DEGRADED, reason);
    }
}
