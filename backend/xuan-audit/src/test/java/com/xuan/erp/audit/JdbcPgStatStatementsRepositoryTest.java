package com.xuan.erp.audit;

import com.xuan.erp.audit.application.query.SqlRankingQuery;
import com.xuan.erp.audit.infrastructure.config.AuditObservabilityProperties;
import com.xuan.erp.audit.infrastructure.persistence.repository.JdbcPgStatStatementsRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcPgStatStatementsRepositoryTest {

    @Test
    void rejectsSqlRankingQueryWhenObservabilityPostgresqlUsernameIsMissing() {
        AuditObservabilityProperties properties = new AuditObservabilityProperties();
        properties.setPostgresqlJdbcUrlTemplate("jdbc:postgresql://127.0.0.1:1/%s");
        properties.setPostgresqlUsername("");
        JdbcPgStatStatementsRepository repository = new JdbcPgStatStatementsRepository(properties);

        assertThatThrownBy(() -> repository.findRankings(new SqlRankingQuery(
                List.of("xuan_iam"),
                "total_exec_time",
                1
        )))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
