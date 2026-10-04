package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 消息完整投递轨迹时间线数据传输对象。
 * 承载指定消息全生命周期跨端时间线节点拓扑。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class MessageTraceDTO {

    /**
     * 目标消息唯一标识 ID。
     */
    private String msgId;

    /**
     * 主题名称。
     */
    private String topic;

    /**
     * 业务索引 Key。
     */
    private String keys;

    /**
     * 按时序排列的轨迹生命周期节点列表。
     */
    private List<MessageTraceNodeDTO> nodes = new ArrayList<>();

    public MessageTraceDTO() {
    }

    public MessageTraceDTO(String msgId, String topic) {
        this.msgId = msgId;
        this.topic = topic;
    }

    public MessageTraceDTO(String msgId, String topic, List<MessageTraceNodeDTO> nodes) {
        this.msgId = msgId;
        this.topic = topic;
        this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    public List<MessageTraceNodeDTO> getNodes() {
        return nodes != null ? nodes : Collections.emptyList();
    }

    public List<MessageTraceNodeDTO> getTraceNodes() {
        return getNodes();
    }

    public void setNodes(List<MessageTraceNodeDTO> nodes) {
        this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
    }
}
