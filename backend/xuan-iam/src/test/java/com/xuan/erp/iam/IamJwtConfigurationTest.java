package com.xuan.erp.iam;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.xuan.erp.common.security.jwt.JwkJwtTokenParser;
import com.xuan.erp.iam.application.port.IamAccessTokenIssuer;
import com.xuan.erp.iam.infrastructure.config.IamJwtConfiguration;
import com.xuan.erp.iam.infrastructure.config.IamJwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class IamJwtConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(IamJwtConfigurationTestConfiguration.class);

    // 测试未配置固定 signing-jwk-json 时，会生成临时 RSAKey 并打印重启失效告警。
    @Test
    void warnsWhenUsingEphemeralSigningKey(CapturedOutput output) {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RSAKey.class);
            assertThat(context).hasSingleBean(JwkJwtTokenParser.class);
            assertThat(context).hasSingleBean(IamAccessTokenIssuer.class);
            assertThat(context.getStartupFailure()).isNull();
            assertThat(context.getBean(IamJwtProperties.class).getSigningJwkJson()).isNull();
            assertThat(context.getBean(RSAKey.class).getKeyID()).isEqualTo("xuan-iam-local");
            assertThat(output.getOut())
                    .contains("xuan.iam.jwt.signing-jwk-json 未配置")
                    .contains("服务重启后旧 token 会失效")
                    .contains("xuan-iam.yaml");
        });
    }

    // 测试配置固定 signing-jwk-json 时，不再依赖临时生成的 JWK。
    @Test
    void acceptsFixedSigningJwkJson() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("fixed-kid").generate();
        contextRunner
                .withPropertyValues(
                        "xuan.iam.jwt.signing-jwk-json=" + rsaKey.toJSONString(),
                        "xuan.iam.jwt.key-id=fixed-kid")
                .run(context -> {
                    assertThat(context).hasSingleBean(RSAKey.class);
                    assertThat(context).hasSingleBean(JwkJwtTokenParser.class);
                    assertThat(context).hasSingleBean(IamAccessTokenIssuer.class);
                    assertThat(context.getBean(RSAKey.class).getKeyID()).isEqualTo("fixed-kid");
                    assertThat(context.getBean(IamJwtProperties.class).getSigningJwkJson()).isNotBlank();
                });
    }

    @EnableConfigurationProperties(IamJwtProperties.class)
    @Import(IamJwtConfiguration.class)
    static class IamJwtConfigurationTestConfiguration {
    }
}
