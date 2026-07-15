package com.xuan.erp.audit.application.query;

public record InterfaceTraceQuery(
        String serviceName,
        String endpointName,
        String startTime,
        String endTime,
        int limit
) {
}
