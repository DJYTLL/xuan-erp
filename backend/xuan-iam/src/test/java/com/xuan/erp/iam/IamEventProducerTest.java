package com.xuan.erp.iam;

import com.xuan.erp.common.mq.RocketMqMessage;
import com.xuan.erp.common.mq.RocketMqMessageSender;
import com.xuan.erp.common.mq.RocketMqSendResult;
import com.xuan.erp.common.mq.config.XuanRocketMqProperties;
import com.xuan.erp.iam.infrastructure.mq.IamEventProducer;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IamEventProducerTest {

    // 测试 IAM 示例 Producer 会把“用户创建”事件发送到配置的 iam topic，并带上固定 tag、key 和 header。
    @Test
    void publishesUserCreatedEventToConfiguredIamTopic() {
        XuanRocketMqProperties properties = new XuanRocketMqProperties();
        properties.getTopics().setIamEvents("xuan-iam-event");
        RecordingRocketMqMessageSender sender = new RecordingRocketMqMessageSender();
        IamEventProducer producer = new IamEventProducer(sender, properties);

        producer.publishUserCreatedEvent(1001L);

        assertThat(sender.lastMessage).isNotNull();
        assertThat(sender.lastMessage.topic()).isEqualTo("xuan-iam-event");
        assertThat(sender.lastMessage.tag()).isEqualTo("iam.user-created");
        assertThat(sender.lastMessage.key()).isEqualTo("iam-user:1001");
        assertThat(sender.lastMessage.headers()).containsEntry("source", "xuan-iam");
        assertThat(sender.lastMessage.payload())
                .isInstanceOf(Map.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("userId", 1001L)
                .containsEntry("eventType", "iam.user-created");
    }

    static class RecordingRocketMqMessageSender implements RocketMqMessageSender {

        private RocketMqMessage<?> lastMessage;

        @Override
        public <T> RocketMqSendResult send(RocketMqMessage<T> message) {
            this.lastMessage = message;
            return new RocketMqSendResult("SEND_OK", "msg-iam-001", 0, 0L);
        }
    }
}
