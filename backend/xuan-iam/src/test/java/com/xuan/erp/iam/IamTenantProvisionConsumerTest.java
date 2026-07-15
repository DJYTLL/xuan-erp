package com.xuan.erp.iam;

import com.xuan.erp.common.mq.RocketMqMessage;
import com.xuan.erp.common.mq.RocketMqMessageSender;
import com.xuan.erp.common.mq.RocketMqSendResult;
import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import com.xuan.erp.iam.application.port.IamTenantBootstrapGateway;
import com.xuan.erp.iam.application.service.IamTenantBootstrapApplicationService;
import com.xuan.erp.iam.infrastructure.mq.IamEventProducer;
import com.xuan.erp.iam.infrastructure.mq.IamTenantProvisionConsumer;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IamTenantProvisionConsumerTest {

    @Test
    void consumesTenantBootstrapRequestAndPublishesCompletedEvent() {
        IamTenantBootstrapGateway gateway = mock(IamTenantBootstrapGateway.class);
        when(gateway.bootstrapTenant(
                eq(101L),
                eq("admin"),
                eq("{noop}secret"),
                eq("租户管理员"),
                eq("admin@example.com"),
                eq("13800000000"),
                eq("xuan-tenant"))).thenReturn(12);
        IamTenantBootstrapApplicationService service = new IamTenantBootstrapApplicationService(
                gateway,
                PasswordEncoderFactories.createDelegatingPasswordEncoder());
        RecordingRocketMqMessageSender sender = new RecordingRocketMqMessageSender();
        XuanRocketMqProperties properties = properties();
        IamTenantProvisionConsumer consumer = new IamTenantProvisionConsumer(service, producer(sender, properties), properties);

        String result = consumer.handleTenantProvisionRequest("""
                {
                  "eventId": "tenant-event-1",
                  "eventType": "TenantIamBootstrapRequested",
                  "tenantId": 101,
                  "taskId": 9001,
                  "taskKey": "tenant:create:acme",
                  "stepId": 7001,
                  "provisionStep": "IAM_BOOTSTRAP",
                  "idempotencyKey": "idem-101",
                  "adminUsername": "admin",
                  "adminPasswordHash": "{noop}secret",
                  "adminDisplayName": "租户管理员",
                  "adminEmail": "admin@example.com",
                  "adminPhone": "13800000000"
                }
                """);

        assertThat(result).isEqualTo("TenantIamProvisionStepCompleted");
        verify(gateway).bootstrapTenant(101L, "admin", "{noop}secret", "租户管理员", "admin@example.com", "13800000000", "xuan-tenant");
        assertThat(sender.lastMessage).isNotNull();
        assertThat(sender.lastMessage.topic()).isEqualTo("xuan-tenant-event");
        assertThat(sender.lastMessage.tag()).isEqualTo("TenantIamProvisionStepCompleted");
        assertThat(sender.lastMessage.key()).isEqualTo("tenant:create:acme");
        assertThat(sender.lastMessage.payload())
                .isInstanceOf(Map.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("eventType", "TenantIamProvisionStepCompleted")
                .containsEntry("tenantId", 101L)
                .containsEntry("taskKey", "tenant:create:acme")
                .containsEntry("idempotencyKey", "idem-101")
                .containsEntry("provisionStep", "IAM_BOOTSTRAP");
        @SuppressWarnings("unchecked")
        Map<String, Object> resultPayload = (Map<String, Object>) ((Map<?, ?>) sender.lastMessage.payload()).get("resultPayload");
        assertThat(resultPayload).containsEntry("menuGrantCount", 12);
    }

    @Test
    void publishesFailedEventWhenIamBootstrapFails() {
        IamTenantBootstrapGateway gateway = mock(IamTenantBootstrapGateway.class);
        when(gateway.bootstrapTenant(eq(101L), eq("admin"), eq("{noop}secret"), eq("租户管理员"), eq(null), eq(null), eq("xuan-tenant")))
                .thenThrow(new IllegalStateException("database unavailable"));
        IamTenantBootstrapApplicationService service = new IamTenantBootstrapApplicationService(
                gateway,
                PasswordEncoderFactories.createDelegatingPasswordEncoder());
        RecordingRocketMqMessageSender sender = new RecordingRocketMqMessageSender();
        XuanRocketMqProperties properties = properties();
        IamTenantProvisionConsumer consumer = new IamTenantProvisionConsumer(service, producer(sender, properties), properties);

        String result = consumer.handleTenantProvisionRequest("""
                {
                  "eventId": "tenant-event-2",
                  "eventType": "TenantIamBootstrapRequested",
                  "tenantId": 101,
                  "taskKey": "tenant:create:acme",
                  "provisionStep": "IAM_BOOTSTRAP",
                  "idempotencyKey": "idem-101",
                  "adminUsername": "admin",
                  "adminPasswordHash": "{noop}secret",
                  "adminDisplayName": "租户管理员"
                }
                """);

        assertThat(result).isEqualTo("TenantIamProvisionStepFailed");
        assertThat(sender.lastMessage.tag()).isEqualTo("TenantIamProvisionStepFailed");
        assertThat(sender.lastMessage.payload())
                .isInstanceOf(Map.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("eventType", "TenantIamProvisionStepFailed")
                .containsEntry("errorCode", "IAM_BOOTSTRAP_FAILED")
                .containsEntry("errorMessage", "database unavailable");
    }

    private XuanRocketMqProperties properties() {
        XuanRocketMqProperties properties = new XuanRocketMqProperties();
        properties.getTopics().setTenantEvents("xuan-tenant-event");
        properties.getTopics().setIamEvents("xuan-iam-event");
        return properties;
    }

    private IamEventProducer producer(RecordingRocketMqMessageSender sender, XuanRocketMqProperties properties) {
        return new IamEventProducer(sender, properties);
    }

    static class RecordingRocketMqMessageSender implements RocketMqMessageSender {

        private RocketMqMessage<?> lastMessage;

        @Override
        public <T> RocketMqSendResult send(RocketMqMessage<T> message) {
            this.lastMessage = message;
            return new RocketMqSendResult("SEND_OK", "msg-iam-provision", 0, 0L);
        }
    }
}
