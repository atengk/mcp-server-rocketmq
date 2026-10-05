package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 主题状态与位点容量全景数据传输对象。
 * 承载指定主题所有分片队列的位点统计及全量消息存留估算。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class TopicStatusDTO {

    /**
     * 主题名称。
     */
    private String topic;

    /**
     * 主题在所有队列上的消息留存总量。
     */
    private long totalMessages = 0;

    /**
     * 全局最小点位。
     */
    private long minOffset = 0;

    /**
     * 全局最大点位。
     */
    private long maxOffset = 0;

    /**
     * 各分片队列位点详情列表。
     */
    private List<TopicQueueOffsetDTO> queues = new ArrayList<>();

    public TopicStatusDTO() {
    }

    public TopicStatusDTO(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(long totalMessages) {
        this.totalMessages = totalMessages;
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

    public List<TopicQueueOffsetDTO> getQueues() {
        return queues != null ? queues : Collections.emptyList();
    }

    public void setQueues(List<TopicQueueOffsetDTO> queues) {
        this.queues = queues != null ? queues : new ArrayList<>();
    }
}
