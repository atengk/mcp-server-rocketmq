package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

/**
 * 分区队列位点与消息容量数据传输对象。
 * 承载单个分片队列上的最小位点、最大位点及实时消息数量。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class TopicQueueOffsetDTO {

    /**
     * Broker 实例标识名称。
     */
    private String brokerName;

    /**
     * 队列编号。
     */
    private int queueId;

    /**
     * 当前队列最小消费点位。
     */
    private long minOffset;

    /**
     * 当前队列最大消息点位。
     */
    private long maxOffset;

    /**
     * 当前队列消息总留存量（maxOffset - minOffset）。
     */
    private long messageCount;

    /**
     * 最后更新时间戳。
     */
    private long lastUpdateTimestamp;

    public TopicQueueOffsetDTO() {
    }

    public TopicQueueOffsetDTO(String brokerName, int queueId, long minOffset, long maxOffset, long lastUpdateTimestamp) {
        this.brokerName = brokerName;
        this.queueId = queueId;
        this.minOffset = minOffset;
        this.maxOffset = maxOffset;
        this.messageCount = Math.max(0, maxOffset - minOffset);
        this.lastUpdateTimestamp = lastUpdateTimestamp;
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

    public long getMinOffset() {
        return minOffset;
    }

    public void setMinOffset(long minOffset) {
        this.minOffset = minOffset;
    }

    public long getMaxOffset() {
        return maxOffset;
    }

    public void setMaxOffset(long maxOffset) {
        this.maxOffset = maxOffset;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }
}
