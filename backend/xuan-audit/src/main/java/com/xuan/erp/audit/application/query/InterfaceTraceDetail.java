package com.xuan.erp.audit.application.query;

import java.util.List;

public record InterfaceTraceDetail(
        String traceId,
        List<InterfaceSpanDetail> spans
) {
}
