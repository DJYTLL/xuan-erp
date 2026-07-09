package com.xuan.erp.iam.infrastructure.mq;

import org.springframework.stereotype.Component;

/**
 * IAM 服务示例 Consumer，演示消息进入业务模块后的最小落点。
 */
@Component
public class IamEventConsumer {

    /**
     * 处理收到的 IAM 事件原始消息。
     *
     * @param payload 原始消息内容
     * @return 当前示例直接原样返回，供后续真实业务处理替换
     */
    public String handleIamEvent(String payload) {
        return payload;
    }
}
