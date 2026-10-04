package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 消息检索列表数据传输对象。
 * 承载按 Key 或条件检索到的消息详情集合与总数。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class MessageListDTO {

    /**
     * 匹配到的消息明细列表。
     */
    private List<MessageDetailDTO> messages = new ArrayList<>();

    /**
     * 命中结果总数量。
     */
    private int totalCount = 0;

    public MessageListDTO() {
    }

    public MessageListDTO(List<MessageDetailDTO> messages) {
        setMessages(messages);
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
