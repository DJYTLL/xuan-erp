package com.xuan.erp.audit;

import com.xuan.erp.audit.application.query.InterfaceTraceQuery;
import com.xuan.erp.audit.application.query.InterfaceTraceSummary;
import com.xuan.erp.audit.infrastructure.config.AuditObservabilityProperties;
import com.xuan.erp.audit.infrastructure.rpc.HttpSkyWalkingOapClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpSkyWalkingOapClientTest {

    @Test
    void resolvesServiceNameToSkyWalkingServiceIdBeforeQueryingTraces() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpSkyWalkingOapClient client = new HttpSkyWalkingOapClient(properties(), builder);

        server.expect(requestTo("http://oap.example/graphql"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("findService")))
                .andRespond(withSuccess("""
                        {"data":{"findService":{"id":"service-id-1","name":"xuan-iam","shortName":"xuan-iam"}}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://oap.example/graphql"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"serviceId\":\"service-id-1\"")))
                .andRespond(withSuccess("""
                        {"data":{"queryBasicTraces":{"traces":[{"traceIds":["trace-001"],"endpointNames":["/api/iam/auth/login"],"duration":42,"start":"2026-07-12 0101","isError":false}]}}}
                        """, MediaType.APPLICATION_JSON));

        List<InterfaceTraceSummary> traces = client.listTraces(new InterfaceTraceQuery(
                "xuan-iam",
                null,
                "2026-07-12 0100",
                "2026-07-12 0200",
                20
        ));

        assertEquals(1, traces.size());
        assertEquals("trace-001", traces.getFirst().traceId());
        assertEquals("xuan-iam", traces.getFirst().serviceName());
        server.verify();
    }

    private AuditObservabilityProperties properties() {
        AuditObservabilityProperties properties = new AuditObservabilityProperties();
        properties.setSkywalkingOapHttpUrl("http://oap.example");
        return properties;
    }
}
