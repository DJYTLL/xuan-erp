package com.xuan.erp.audit.application.query;

public record InterfaceTraceSummary(
        String traceId,
        String serviceName,
        String endpointName,
        long durationMs,
        String startTime,
        boolean error
) {
}
