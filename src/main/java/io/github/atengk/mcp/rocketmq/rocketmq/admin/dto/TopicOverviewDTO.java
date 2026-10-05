package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 业务主题与队列拓扑全景概览数据传输对象。
 * 承载集群内所有业务主题名称、读写队列规模与分布节点概览。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class TopicOverviewDTO {

    /**
     * 业务主题总数。
     */
    private int totalCount = 0;

    /**
     * 业务主题概要列表。
     */
    private List<TopicSummaryDTO> topics = new ArrayList<>();

    public TopicOverviewDTO() {
    }

    public TopicOverviewDTO(List<TopicSummaryDTO> topics) {
        setTopics(topics);
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<TopicSummaryDTO> getTopics() {
        return topics != null ? topics : Collections.emptyList();
    }

    public void setTopics(List<TopicSummaryDTO> topics) {
        this.topics = topics != null ? new ArrayList<>(topics) : new ArrayList<>();
        this.totalCount = this.topics.size();
    }

    /**
     * 单个主题的队列规模与分布节点摘要。
     *
     * @author Ateng
     * @since 2026-10-04
     */
    public static class TopicSummaryDTO {

        /**
         * 主题名称。
         */
        private String topic;

        /**
         * 累计读队列数量。
         */
        private int totalReadQueues = 0;

        /**
         * 累计写队列数量。
         */
        private int totalWriteQueues = 0;

        /**
         * 分布的 Broker 实例列表。
         */
        private List<String> brokers = new ArrayList<>();

        public TopicSummaryDTO() {
        }

        public TopicSummaryDTO(String topic, int totalReadQueues, int totalWriteQueues, List<String> brokers) {
            this.topic = topic;
            this.totalReadQueues = totalReadQueues;
            this.totalWriteQueues = totalWriteQueues;
            this.brokers = brokers != null ? new ArrayList<>(brokers) : new ArrayList<>();
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public int getTotalReadQueues() {
            return totalReadQueues;
        }

        public void setTotalReadQueues(int totalReadQueues) {
            this.totalReadQueues = totalReadQueues;
        }

        public int getTotalWriteQueues() {
            return totalWriteQueues;
        }

        public void setTotalWriteQueues(int totalWriteQueues) {
            this.totalWriteQueues = totalWriteQueues;
        }

        public List<String> getBrokers() {
            return brokers != null ? brokers : Collections.emptyList();
        }

        public void setBrokers(List<String> brokers) {
            this.brokers = brokers != null ? new ArrayList<>(brokers) : new ArrayList<>();
        }
    }
}
