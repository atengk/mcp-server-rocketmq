package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 消费组连接与订阅状态全景数据传输对象。
 * 承载消费组的在线实例分布、消费类型、广播/集群模式及全量订阅表达式。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class ConsumerConnectionDTO {

    /**
     * 消费组名称。
     */
    private String consumerGroup;

    /**
     * 消费类型（如 CONSUME_PASSIVELY 即 PUSH 模式，CONSUME_ACTIVELY 即 PULL 模式）。
     */
    private String consumeType;

    /**
     * 消息投递模式（CLUSTERING 集群消费，BROADCASTING 广播消费）。
     */
    private String messageModel;

    /**
     * 消费起始位点策略（如 CONSUME_FROM_LAST_OFFSET 等）。
     */
    private String consumeFromWhere;

    /**
     * 在线客户端实例列表。
     */
    private List<ConsumerClientDTO> clients = new ArrayList<>();

    /**
     * 消费组当前所有订阅关系列表。
     */
    private List<SubscriptionDTO> subscriptions = new ArrayList<>();

    /**
     * 在线客户端总数。
     */
    private int totalClients = 0;

    /**
     * 消费组是否在线（即是否有活跃客户端）。
     */
    private boolean online = false;

    public ConsumerConnectionDTO() {
    }

    public ConsumerConnectionDTO(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getConsumeType() {
        return consumeType;
    }

    public void setConsumeType(String consumeType) {
        this.consumeType = consumeType;
    }

    public String getMessageModel() {
        return messageModel;
    }

    public void setMessageModel(String messageModel) {
        this.messageModel = messageModel;
    }

    public String getConsumeFromWhere() {
        return consumeFromWhere;
    }

    public void setConsumeFromWhere(String consumeFromWhere) {
        this.consumeFromWhere = consumeFromWhere;
    }

    public List<ConsumerClientDTO> getClients() {
        return clients != null ? clients : Collections.emptyList();
    }

    public void setClients(List<ConsumerClientDTO> clients) {
        this.clients = clients != null ? new ArrayList<>(clients) : new ArrayList<>();
        this.totalClients = this.clients.size();
        this.online = !this.clients.isEmpty();
    }

    public List<SubscriptionDTO> getSubscriptions() {
        return subscriptions != null ? subscriptions : Collections.emptyList();
    }

    public void setSubscriptions(List<SubscriptionDTO> subscriptions) {
        this.subscriptions = subscriptions != null ? new ArrayList<>(subscriptions) : new ArrayList<>();
    }

    public int getTotalClients() {
        return totalClients;
    }

    public void setTotalClients(int totalClients) {
        this.totalClients = totalClients;
        this.online = totalClients > 0;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
