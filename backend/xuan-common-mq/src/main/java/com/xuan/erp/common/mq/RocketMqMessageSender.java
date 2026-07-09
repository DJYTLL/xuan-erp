package com.xuan.erp.common.mq;

/**
 * 统一消息发送接口，业务模块只依赖该接口即可发送 RocketMQ 消息。
 */
@FunctionalInterface
public interface RocketMqMessageSender {

    /**
     * 发送统一消息模型并返回归一化结果。
     *
     * @param message 待发送消息
     * @param <T>     负载类型
     * @return 归一化发送结果
     */
    <T> RocketMqSendResult send(RocketMqMessage<T> message);
}
