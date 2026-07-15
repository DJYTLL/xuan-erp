package com.xuan.erp.audit.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "xuan.audit.observability")
public class AuditObservabilityProperties {

    private String skywalkingOapHttpUrl = "http://duaoyunxuan.top:9051";

    private String postgresqlJdbcUrlTemplate = "jdbc:postgresql://duaoyunxuan.top:9042/%s";

    private String postgresqlUsername = "";

    private String postgresqlPassword = "";

    public String getSkywalkingOapHttpUrl() {
        return skywalkingOapHttpUrl;
    }

    public void setSkywalkingOapHttpUrl(String skywalkingOapHttpUrl) {
        this.skywalkingOapHttpUrl = skywalkingOapHttpUrl;
    }

    public String getPostgresqlJdbcUrlTemplate() {
        return postgresqlJdbcUrlTemplate;
    }

    public void setPostgresqlJdbcUrlTemplate(String postgresqlJdbcUrlTemplate) {
        this.postgresqlJdbcUrlTemplate = postgresqlJdbcUrlTemplate;
    }

    public String getPostgresqlUsername() {
        return postgresqlUsername;
    }

    public void setPostgresqlUsername(String postgresqlUsername) {
        this.postgresqlUsername = postgresqlUsername;
    }

    public String getPostgresqlPassword() {
        return postgresqlPassword;
    }

    public void setPostgresqlPassword(String postgresqlPassword) {
        this.postgresqlPassword = postgresqlPassword;
    }
}
