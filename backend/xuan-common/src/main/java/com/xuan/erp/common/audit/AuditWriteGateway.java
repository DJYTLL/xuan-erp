package com.xuan.erp.common.audit;

@FunctionalInterface
public interface AuditWriteGateway {

    AuditWriteReceipt write(AuditWriteEvent event);
}
