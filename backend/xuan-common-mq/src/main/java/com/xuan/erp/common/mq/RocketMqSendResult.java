package com.xuan.erp.common.mq;

/**
 * 统一发送结果，避免业务方直接依赖底层 RocketMQ 返回对象。
 *
 * @param sendStatus 发送状态
 * @param messageId  消息 ID
 * @param queueId    队列编号
 * @param queueOffset 队列偏移量
 */
public record RocketMqSendResult(
        String sendStatus,
        String messageId,
        int queueId,
        long queueOffset) {
}
