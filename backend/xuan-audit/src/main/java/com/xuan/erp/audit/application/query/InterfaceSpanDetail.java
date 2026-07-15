package com.xuan.erp.audit.application.query;

public record InterfaceSpanDetail(
        String spanId,
        String parentSpanId,
        String serviceName,
        String endpointName,
        String type,
        long durationMs,
        boolean error
) {
}
