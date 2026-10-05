package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

/**
 * 消费组积压排行摘要数据传输对象。
 * 用于 TopN 积压排行榜展示单个消费组的堆积与速率指标。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class ConsumerLagSummaryDTO {

    /**
     * 消费组名称。
     */
    private String consumerGroup;

    /**
     * 未消费消息总积压量。
     */
    private long totalLag = 0;

    /**
     * 实时消费速率 TPS。
     */
    private double consumeTps = 0.0;

    /**
     * 是否存在待消费积压消息。
     */
    private boolean hasPendingMessages = false;

    public ConsumerLagSummaryDTO() {
    }

    public ConsumerLagSummaryDTO(String consumerGroup, long totalLag, double consumeTps) {
        this.consumerGroup = consumerGroup;
        this.totalLag = totalLag;
        this.consumeTps = consumeTps;
        this.hasPendingMessages = totalLag > 0;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public long getTotalLag() {
        return totalLag;
    }

    public void setTotalLag(long totalLag) {
        this.totalLag = totalLag;
        this.hasPendingMessages = totalLag > 0;
    }

    public double getConsumeTps() {
        return consumeTps;
    }

    public void setConsumeTps(double consumeTps) {
        this.consumeTps = consumeTps;
    }

    public boolean hasPendingMessages() {
        return hasPendingMessages;
    }

    public void setHasPendingMessages(boolean hasPendingMessages) {
        this.hasPendingMessages = hasPendingMessages;
    }
}
