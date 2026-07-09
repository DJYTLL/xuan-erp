package com.xuan.erp.common.mq;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RocketMqMessageTest {

    // 测试统一消息模型会保留主题、标签、业务键、负载和消息头。
    @Test
    void keepsAllMessageFields() {
        RocketMqMessage<Map<String, Object>> message = new RocketMqMessage<>(
                "xuan-tenant-event",
                "tenant.created",
                "tenant-001",
                Map.of("tenantId", 1L),
                new LinkedHashMap<>(Map.of("traceId", "trace-001")));

        assertThat(message.topic()).isEqualTo("xuan-tenant-event");
        assertThat(message.tag()).isEqualTo("tenant.created");
        assertThat(message.key()).isEqualTo("tenant-001");
        assertThat(message.payload()).containsEntry("tenantId", 1L);
        assertThat(message.headers()).containsEntry("traceId", "trace-001");
    }

    // 测试未传 headers 时统一消息模型会回落为空 Map，避免业务方判空。
    @Test
    void fallsBackToEmptyHeadersWhenHeadersMissing() {
        RocketMqMessage<String> message = new RocketMqMessage<>(
                "xuan-tenant-event",
                null,
                null,
                "payload",
                null);

        assertThat(message.headers()).isEmpty();
    }
}
