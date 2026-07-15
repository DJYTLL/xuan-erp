package com.xuan.erp.audit.infrastructure.rpc;

import com.xuan.erp.audit.application.query.InterfaceTraceDetail;
import com.xuan.erp.audit.application.query.InterfaceTraceQuery;
import com.xuan.erp.audit.application.query.InterfaceTraceSummary;
import java.util.List;

public interface SkyWalkingOapClient {

    List<InterfaceTraceSummary> listTraces(InterfaceTraceQuery query);

    InterfaceTraceDetail getTrace(String traceId);
}
