package com.xuan.erp.common.audit;

@FunctionalInterface
public interface AuditWriteFailureHandler {

    void handle(AuditWriteEvent event, RuntimeException failure);
}
