package com.xuan.erp.common.mq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Xuan ERP 统一 RocketMQ 基础配置。
 */
@ConfigurationProperties(prefix = "xuan.rocketmq")
public class XuanRocketMqProperties {

    /**
     * 是否启用 RocketMQ 相关公共能力。
     */
    private boolean enabled;

    /**
     * RocketMQ NameServer 地址。
     */
    private String nameServer;

    /**
     * AccessKey，后续接入 ACL 或云上鉴权时复用。
     */
    private String accessKey;

    /**
     * SecretKey，后续接入 ACL 或云上鉴权时复用。
     */
    private String secretKey;

    /**
     * 生产者基础配置。
     */
    private final Producer producer = new Producer();

    /**
     * 统一 topic 配置。
     */
    private final Topics topics = new Topics();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Producer getProducer() {
        return producer;
    }

    public Topics getTopics() {
        return topics;
    }

    /**
     * RocketMQ 生产者公共配置。
     */
    public static class Producer {

        /**
         * 统一生产者组名。
         */
        private String group;

        /**
         * 同步发送超时时间，单位毫秒。
         */
        private Integer sendTimeout;

        /**
         * 允许发送的最大消息体大小，单位字节。
         */
        private Integer maxMessageSize;

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public Integer getSendTimeout() {
            return sendTimeout;
        }

        public void setSendTimeout(Integer sendTimeout) {
            this.sendTimeout = sendTimeout;
        }

        public Integer getMaxMessageSize() {
            return maxMessageSize;
        }

        public void setMaxMessageSize(Integer maxMessageSize) {
            this.maxMessageSize = maxMessageSize;
        }
    }

    /**
     * RocketMQ 示例 topic 配置。
     */
    public static class Topics {

        /**
         * 租户事件 topic。
         */
        private String tenantEvents;

        /**
         * IAM 事件 topic。
         */
        private String iamEvents;

        public String getTenantEvents() {
            return tenantEvents;
        }

        public void setTenantEvents(String tenantEvents) {
            this.tenantEvents = tenantEvents;
        }

        public String getIamEvents() {
            return iamEvents;
        }

        public void setIamEvents(String iamEvents) {
            this.iamEvents = iamEvents;
        }
    }
}
