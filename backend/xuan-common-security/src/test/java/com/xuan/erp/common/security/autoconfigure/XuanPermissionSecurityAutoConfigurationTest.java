package com.xuan.erp.common.security.autoconfigure;

import com.xuan.erp.common.security.permission.CachedPermissionSnapshotProvider;
import com.xuan.erp.common.security.permission.PermissionSnapshotProvider;
import com.xuan.erp.common.security.permission.RemoteIamPermissionSnapshotProvider;
import com.xuan.erp.common.security.permission.XuanPermissionExpression;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权限快照自动配置测试，验证业务服务启用配置后可获得远程加载、缓存和表达式 Bean。
 */
class XuanPermissionSecurityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(XuanPermissionSecurityAutoConfiguration.class));

    // 测试配置完整时自动创建 IAM 远程快照 Provider、缓存 Provider 和 @xuanPermission 表达式。
    @Test
    void createsPermissionSnapshotBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "xuan.security.permission.enabled=true",
                        "xuan.security.permission.iam-snapshot-uri=http://127.0.0.1:18080/api/iam/permissions/current",
                        "xuan.security.permission.cache.ttl=PT30S")
                .run(context -> {
                    assertThat(context).hasSingleBean(RemoteIamPermissionSnapshotProvider.class);
                    assertThat(context).hasSingleBean(CachedPermissionSnapshotProvider.class);
                    assertThat(context).hasSingleBean(PermissionSnapshotProvider.class);
                    assertThat(context).hasBean("xuanPermission");
                    assertThat(context).hasSingleBean(XuanPermissionExpression.class);
                });
    }

    // 测试未配置 IAM 快照地址时启动失败，避免业务服务误以为已启用远程授权。
    @Test
    void failsWhenIamSnapshotUriIsMissing() {
        contextRunner
                .withPropertyValues("xuan.security.permission.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }
}
