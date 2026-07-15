package com.xuan.erp.audit.infrastructure.persistence.repository;

import com.xuan.erp.audit.application.query.SqlRankingEntry;
import com.xuan.erp.audit.application.query.SqlRankingQuery;
import com.xuan.erp.audit.infrastructure.config.AuditObservabilityProperties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class JdbcPgStatStatementsRepository implements PgStatStatementsRepository {

    private final AuditObservabilityProperties properties;

    public JdbcPgStatStatementsRepository(AuditObservabilityProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<SqlRankingEntry> findRankings(SqlRankingQuery query) {
        if (properties.getPostgresqlUsername() == null || properties.getPostgresqlUsername().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SQL 排名查询需要配置 xuan.audit.observability.postgresql-username"
            );
        }
        List<SqlRankingEntry> result = new ArrayList<>();
        for (String databaseName : query.databaseNames()) {
            result.addAll(findDatabaseRankings(databaseName, query.sortBy(), query.limit()));
        }
        return result;
    }

    private List<SqlRankingEntry> findDatabaseRankings(String databaseName, String sortBy, int limit) {
        String sql = """
                select query, calls, total_exec_time, mean_exec_time, max_exec_time, rows
                from pg_stat_statements
                order by %s desc
                limit ?
                """.formatted(sortBy);
        try (Connection connection = openConnection(databaseName);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SqlRankingEntry> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new SqlRankingEntry(
                            databaseName,
                            resultSet.getString("query"),
                            resultSet.getLong("calls"),
                            resultSet.getDouble("total_exec_time"),
                            resultSet.getDouble("mean_exec_time"),
                            resultSet.getDouble("max_exec_time"),
                            resultSet.getLong("rows")
                    ));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "查询 pg_stat_statements 失败：" + databaseName,
                    ex
            );
        }
    }

    private Connection openConnection(String databaseName) throws SQLException {
        String url = properties.getPostgresqlJdbcUrlTemplate().formatted(databaseName);
        return DriverManager.getConnection(url, properties.getPostgresqlUsername(), properties.getPostgresqlPassword());
    }
}
