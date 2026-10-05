package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 消费组死信队列消息列表数据传输对象。
 * 承载死信主题名称、目标消费组及死信消息详情列表。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class DlqMessageListDTO {

    /**
     * 关联的消费组名称。
     */
    private String consumerGroup;

    /**
     * 死信队列对应的主题名称（%DLQ%consumerGroup）。
     */
    private String dlqTopic;

    /**
     * 死信消息集合。
     */
    private List<MessageDetailDTO> messages = new ArrayList<>();

    /**
     * 死信消息总数。
     */
    private int totalCount = 0;

    public DlqMessageListDTO() {
    }

    public DlqMessageListDTO(String consumerGroup, String dlqTopic, List<MessageDetailDTO> messages) {
        this.consumerGroup = consumerGroup;
        this.dlqTopic = dlqTopic;
        setMessages(messages);
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getDlqTopic() {
        return dlqTopic;
    }

    public void setDlqTopic(String dlqTopic) {
        this.dlqTopic = dlqTopic;
    }

    public List<MessageDetailDTO> getMessages() {
        return messages != null ? messages : Collections.emptyList();
    }

    public void setMessages(List<MessageDetailDTO> messages) {
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
        this.totalCount = this.messages.size();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
