package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 主题路由全景数据传输对象。
 * 承载指定主题的读写队列分布与 Broker 路由节点列表。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class TopicRouteDTO {

    /**
     * 主题名称。
     */
    private String topic;

    /**
     * 各 Broker 节点读写队列配置列表。
     */
    private List<QueueDataDTO> queueDatas = new ArrayList<>();

    /**
     * 主题所路由到的 Broker 节点列表。
     */
    private List<BrokerSummaryDTO> brokerDatas = new ArrayList<>();

    public TopicRouteDTO() {
    }

    public TopicRouteDTO(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public List<QueueDataDTO> getQueueDatas() {
        return queueDatas != null ? queueDatas : Collections.emptyList();
    }

    public void setQueueDatas(List<QueueDataDTO> queueDatas) {
        this.queueDatas = queueDatas != null ? queueDatas : new ArrayList<>();
    }

    public List<BrokerSummaryDTO> getBrokerDatas() {
        return brokerDatas != null ? brokerDatas : Collections.emptyList();
    }

    public void setBrokerDatas(List<BrokerSummaryDTO> brokerDatas) {
        this.brokerDatas = brokerDatas != null ? brokerDatas : new ArrayList<>();
    }
}
