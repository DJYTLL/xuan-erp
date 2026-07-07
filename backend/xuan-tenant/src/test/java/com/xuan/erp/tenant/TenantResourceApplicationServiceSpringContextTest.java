package com.xuan.erp.tenant;

import com.xuan.erp.tenant.application.service.TenantResourceApplicationService;
import com.xuan.erp.tenant.domain.repository.TenantResourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class TenantResourceApplicationServiceSpringContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TenantResourceApplicationServiceContextConfiguration.class);

    @Test
    void createsTenantResourceApplicationServiceWithRepositoryDependency() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(TenantResourceApplicationService.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TenantResourceApplicationService.class)
    static class TenantResourceApplicationServiceContextConfiguration {

        @Bean
        TenantResourceRepository tenantResourceRepository() {
            return new RecordingTenantResourceRepository();
        }
    }
}
