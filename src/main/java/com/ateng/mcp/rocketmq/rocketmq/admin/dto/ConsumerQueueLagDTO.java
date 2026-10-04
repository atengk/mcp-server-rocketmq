package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

/**
 * 分片队列消费位点与积压明细数据传输对象。
 * 承载单个分片队列上的最大位点、消费位点、积压差值与最后消费更新时间戳。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class ConsumerQueueLagDTO {

    /**
     * 目标主题名称。
     */
    private String topic;

    /**
     * 队列所在的 Broker 名称。
     */
    private String brokerName;

    /**
     * 队列编号。
     */
    private int queueId;

    /**
     * Broker 当前最大消息位点。
     */
    private long brokerOffset;

    /**
     * 消费组当前提交的消费位点。
     */
    private long consumerOffset;

    /**
     * 未消费消息积压差值（brokerOffset - consumerOffset）。
     */
    private long lag;

    /**
     * 最后一次位点更新时间戳。
     */
    private long lastTimestamp;

    public ConsumerQueueLagDTO() {
    }

    public ConsumerQueueLagDTO(String topic, String brokerName, int queueId, long brokerOffset, long consumerOffset, long lastTimestamp) {
        this.topic = topic;
        this.brokerName = brokerName;
        this.queueId = queueId;
        this.brokerOffset = brokerOffset;
        this.consumerOffset = consumerOffset;
        this.lag = Math.max(0, brokerOffset - consumerOffset);
        this.lastTimestamp = lastTimestamp;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getBrokerOffset() {
        return brokerOffset;
    }

    public void setBrokerOffset(long brokerOffset) {
        this.brokerOffset = brokerOffset;
    }

    public long getConsumerOffset() {
        return consumerOffset;
    }

    public void setConsumerOffset(long consumerOffset) {
        this.consumerOffset = consumerOffset;
    }

    public long getLag() {
        return lag;
    }

    public void setLag(long lag) {
        this.lag = lag;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
}
