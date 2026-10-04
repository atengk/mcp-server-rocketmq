package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 消费组积压全景数据传输对象。
 * 承载消费组整体未消费堆积总量、消费 TPS 及各分片队列的点位积压明细。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class ConsumerLagDTO {

    /**
     * 消费组名称。
     */
    private String consumerGroup;

    /**
     * 指定查询的主题名称（若为空表示该组所有主题）。
     */
    private String topic;

    /**
     * 消费组当前总积压消息数（所有队列 Lag 累加）。
     */
    private long totalLag = 0;

    /**
     * 消费组当前实时消费速率 TPS。
     */
    private double consumeTps = 0.0;

    /**
     * 各分片队列点位积压明细列表。
     */
    private List<ConsumerQueueLagDTO> queues = new ArrayList<>();

    public ConsumerLagDTO() {
    }

    public ConsumerLagDTO(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getTotalLag() {
        return totalLag;
    }

    public void setTotalLag(long totalLag) {
        this.totalLag = totalLag;
    }

    public double getConsumeTps() {
        return consumeTps;
    }

    public void setConsumeTps(double consumeTps) {
        this.consumeTps = consumeTps;
    }

    public List<ConsumerQueueLagDTO> getQueues() {
        return queues != null ? queues : Collections.emptyList();
    }

    public void setQueues(List<ConsumerQueueLagDTO> queues) {
        this.queues = queues != null ? new ArrayList<>(queues) : new ArrayList<>();
    }
}
