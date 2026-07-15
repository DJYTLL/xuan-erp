package com.xuan.erp.audit.application.result;

import com.xuan.erp.audit.domain.model.type.AuditWriteStatus;

public record AuditWriteResult(
        Long id,
        AuditWriteStatus status
) {
}
