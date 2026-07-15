package com.xuan.erp.audit.infrastructure.rpc;

import com.xuan.erp.audit.application.query.InterfaceSpanDetail;
import com.xuan.erp.audit.application.query.InterfaceTraceDetail;
import com.xuan.erp.audit.application.query.InterfaceTraceQuery;
import com.xuan.erp.audit.application.query.InterfaceTraceSummary;
import com.xuan.erp.audit.infrastructure.config.AuditObservabilityProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpSkyWalkingOapClient implements SkyWalkingOapClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public HttpSkyWalkingOapClient(AuditObservabilityProperties properties, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(properties.getSkywalkingOapHttpUrl()).build();
    }

    @Override
    public List<InterfaceTraceSummary> listTraces(InterfaceTraceQuery query) {
        String serviceId = resolveServiceId(query.serviceName());
        if (hasText(query.serviceName()) && !hasText(serviceId)) {
            return List.of();
        }
        String endpointId = resolveEndpointId(serviceId, query.endpointName());
        if (hasText(query.endpointName()) && !hasText(endpointId)) {
            return List.of();
        }
        Map<String, Object> response = graphQl("""
                query queryBasicTraces($condition: TraceQueryCondition) {
                  queryBasicTraces(condition: $condition) {
                    traces {
                      traceIds
                      endpointNames
                      duration
                      start
                      isError
                    }
                  }
                }
                """, Map.of("condition", traceCondition(query, serviceId, endpointId)));
        Map<String, Object> data = asMap(response.get("data"));
        Map<String, Object> basicTraces = asMap(data.get("queryBasicTraces"));
        List<Object> traces = asList(basicTraces.get("traces"));
        List<InterfaceTraceSummary> summaries = new ArrayList<>();
        for (Object item : traces) {
            Map<String, Object> trace = asMap(item);
            summaries.add(new InterfaceTraceSummary(
                    firstString(trace.get("traceIds")),
                    query.serviceName(),
                    firstString(trace.get("endpointNames")),
                    longValue(trace.get("duration")),
                    stringValue(trace.get("start")),
                    booleanValue(trace.get("isError"))
            ));
        }
        return summaries;
    }

    @Override
    public InterfaceTraceDetail getTrace(String traceId) {
        Map<String, Object> response = graphQl("""
                query queryTrace($traceId: ID!) {
                  queryTrace(traceId: $traceId) {
                    spans {
                      spanId
                      parentSpanId
                      serviceCode
                      endpointName
                      type
                      startTime
                      endTime
                      isError
                    }
                  }
                }
                """, Map.of("traceId", traceId));
        Map<String, Object> data = asMap(response.get("data"));
        Map<String, Object> trace = asMap(data.get("queryTrace"));
        List<Object> spans = asList(trace.get("spans"));
        List<InterfaceSpanDetail> details = new ArrayList<>();
        for (Object item : spans) {
            Map<String, Object> span = asMap(item);
            long duration = Math.max(0, longValue(span.get("endTime")) - longValue(span.get("startTime")));
            details.add(new InterfaceSpanDetail(
                    stringValue(span.get("spanId")),
                    stringValue(span.get("parentSpanId")),
                    stringValue(span.get("serviceCode")),
                    stringValue(span.get("endpointName")),
                    stringValue(span.get("type")),
                    duration,
                    booleanValue(span.get("isError"))
            ));
        }
        return new InterfaceTraceDetail(traceId, details);
    }

    private String resolveServiceId(String serviceName) {
        if (!hasText(serviceName)) {
            return "";
        }
        Map<String, Object> response = graphQl("""
                query findService($serviceName: String!) {
                  findService(serviceName: $serviceName) {
                    id
                    name
                    shortName
                  }
                }
                """, Map.of("serviceName", serviceName));
        Map<String, Object> data = asMap(response.get("data"));
        Map<String, Object> service = asMap(data.get("findService"));
        return stringValue(service.get("id"));
    }

    private String resolveEndpointId(String serviceId, String endpointName) {
        if (!hasText(endpointName)) {
            return "";
        }
        if (!hasText(serviceId)) {
            return "";
        }
        Map<String, Object> response = graphQl("""
                query findEndpoint($serviceId: ID!, $keyword: String!, $limit: Int!) {
                  findEndpoint(serviceId: $serviceId, keyword: $keyword, limit: $limit) {
                    id
                    name
                  }
                }
                """, Map.of("serviceId", serviceId, "keyword", endpointName, "limit", 1));
        Map<String, Object> data = asMap(response.get("data"));
        List<Object> endpoints = asList(data.get("findEndpoint"));
        return endpoints.isEmpty() ? "" : stringValue(asMap(endpoints.getFirst()).get("id"));
    }

    private Map<String, Object> graphQl(String query, Map<String, Object> variables) {
        return restClient.post()
                .uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", query, "variables", variables))
                .retrieve()
                .body(MAP_TYPE);
    }

    private Map<String, Object> traceCondition(InterfaceTraceQuery query, String serviceId, String endpointId) {
        Map<String, Object> condition = new LinkedHashMap<>();
        condition.put("queryDuration", Map.of("start", query.startTime(), "end", query.endTime(), "step", "MINUTE"));
        condition.put("traceState", "ALL");
        condition.put("queryOrder", "BY_DURATION");
        condition.put("paging", Map.of("pageNum", 1, "pageSize", Math.max(1, query.limit())));
        if (hasText(serviceId)) {
            condition.put("serviceId", serviceId);
        }
        if (hasText(endpointId)) {
            condition.put("endpointId", endpointId);
        }
        return condition;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private List<Object> asList(Object value) {
        return value instanceof List<?> list ? new ArrayList<>(list) : List.of();
    }

    private String firstString(Object value) {
        List<Object> list = asList(value);
        return list.isEmpty() ? "" : stringValue(list.getFirst());
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(stringValue(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private boolean booleanValue(Object value) {
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(stringValue(value));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
