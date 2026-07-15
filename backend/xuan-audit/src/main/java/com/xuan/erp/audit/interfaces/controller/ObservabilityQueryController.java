package com.xuan.erp.audit.interfaces.controller;

import com.xuan.erp.audit.application.query.InterfaceTraceDetail;
import com.xuan.erp.audit.application.query.InterfaceTraceSummary;
import com.xuan.erp.audit.application.query.SqlRankingEntry;
import com.xuan.erp.audit.application.service.ObservabilityQueryApplicationService;
import com.xuan.erp.audit.interfaces.dto.InterfaceTraceQueryRequest;
import com.xuan.erp.audit.interfaces.dto.SqlRankingQueryRequest;
import com.xuan.erp.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/observability")
public class ObservabilityQueryController {

    private final ObservabilityQueryApplicationService service;

    public ObservabilityQueryController(ObservabilityQueryApplicationService service) {
        this.service = service;
    }

    @GetMapping("/interface-traces")
    public ApiResponse<List<InterfaceTraceSummary>> listInterfaceTraces(InterfaceTraceQueryRequest request) {
        return ApiResponse.success(service.listInterfaceTraces(request.toQuery()));
    }

    @GetMapping("/interface-traces/{traceId}")
    public ApiResponse<InterfaceTraceDetail> getInterfaceTrace(@PathVariable("traceId") String traceId) {
        return ApiResponse.success(service.getInterfaceTrace(traceId));
    }

    @GetMapping("/sql-rankings")
    public ApiResponse<List<SqlRankingEntry>> listSqlRankings(SqlRankingQueryRequest request) {
        return ApiResponse.success(service.listSqlRankings(request.toQuery()));
    }
}
