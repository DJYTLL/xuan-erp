package com.xuan.erp.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.MessageQueue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultRocketMqMessageSenderTest {

    // 测试默认发送实现会把统一消息模型转换成 RocketMQ Message，并映射返回结果。
    @Test
    void convertsUnifiedMessageAndDelegatesToProducer() throws Exception {
        DefaultMQProducer producer = mock(DefaultMQProducer.class);
        SendResult sendResult = new SendResult(
                SendStatus.SEND_OK,
                "msg-001",
                new MessageQueue("xuan-tenant-event", "broker-a", 1),
                12L,
                "offset-001",
                "tx-001",
                "region-a");
        when(producer.send(any(org.apache.rocketmq.common.message.Message.class))).thenReturn(sendResult);

        DefaultRocketMqMessageSender sender = new DefaultRocketMqMessageSender(producer, new ObjectMapper());
        RocketMqMessage<Map<String, Object>> message = new RocketMqMessage<>(
                "xuan-tenant-event",
                "tenant.created",
                "tenant-001",
                Map.of("tenantId", 1L),
                Map.of("traceId", "trace-001"));

        RocketMqSendResult result = sender.send(message);

        ArgumentCaptor<org.apache.rocketmq.common.message.Message> messageCaptor =
                ArgumentCaptor.forClass(org.apache.rocketmq.common.message.Message.class);
        verify(producer).send(messageCaptor.capture());
        org.apache.rocketmq.common.message.Message sentMessage = messageCaptor.getValue();

        assertThat(sentMessage.getTopic()).isEqualTo("xuan-tenant-event");
        assertThat(sentMessage.getTags()).isEqualTo("tenant.created");
        assertThat(sentMessage.getKeys()).isEqualTo("tenant-001");
        assertThat(new String(sentMessage.getBody())).contains("\"tenantId\":1");
        assertThat(sentMessage.getUserProperty("traceId")).isEqualTo("trace-001");

        assertThat(result.messageId()).isEqualTo("msg-001");
        assertThat(result.sendStatus()).isEqualTo(SendStatus.SEND_OK.name());
        assertThat(result.queueId()).isEqualTo(1);
        assertThat(result.queueOffset()).isEqualTo(12L);
    }
}
