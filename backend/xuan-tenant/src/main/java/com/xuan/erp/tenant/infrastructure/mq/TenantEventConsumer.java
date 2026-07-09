package com.xuan.erp.tenant.infrastructure.mq;

import org.springframework.stereotype.Component;

/**
 * 租户服务示例 Consumer，演示消息进入业务模块后的最小落点。
 */
@Component
public class TenantEventConsumer {

    /**
     * 处理收到的租户事件原始消息。
     *
     * @param payload 原始消息内容
     * @return 当前示例直接原样返回，供后续真实业务处理替换
     */
    public String handleTenantEvent(String payload) {
        return payload;
    }
}
