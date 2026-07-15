package com.xuan.erp.audit;

import com.xuan.erp.audit.application.service.ObservabilityQueryApplicationService;
import com.xuan.erp.audit.infrastructure.persistence.mapper.AuditLogPersistenceMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=test",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
        })
class AuditSecurityConfigurationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private ObservabilityQueryApplicationService observabilityQueryApplicationService;

    @MockitoBean
    private AuditLogPersistenceMapper auditLogPersistenceMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private List<SecurityFilterChain> securityFilterChains;

    @Test
    void observabilityApiUsesDedicatedSecurityFilterChain() {
        assertThat(securityFilterChains).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void observabilityApiDoesNotTriggerBrowserBasicAuthPrompt() throws Exception {
        when(observabilityQueryApplicationService.listSqlRankings(any())).thenReturn(List.of());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/audit/observability/sql-rankings"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.headers().firstValue(HttpHeaders.WWW_AUTHENTICATE)).isEmpty();
    }

    @Test
    void observabilityApiKeepsServiceUnavailableWhenSqlRankingBackendIsNotConfigured() throws Exception {
        when(observabilityQueryApplicationService.listSqlRankings(any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "SQL 排名查询需要配置 xuan.audit.observability.postgresql-username"
                ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/audit/observability/sql-rankings"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.headers().firstValue(HttpHeaders.WWW_AUTHENTICATE)).isEmpty();
    }
}
