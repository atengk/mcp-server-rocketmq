package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 主题列表数据传输对象。
 * 承载集群内检索到的主题名称集合及总数。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class TopicListDTO {

    /**
     * 主题名称列表。
     */
    private List<String> topics = new ArrayList<>();

    /**
     * 主题总数。
     */
    private int totalCount = 0;

    public TopicListDTO() {
    }

    public TopicListDTO(List<String> topics) {
        setTopics(topics);
    }

    public List<String> getTopics() {
        return topics != null ? topics : Collections.emptyList();
    }

    public void setTopics(List<String> topics) {
        this.topics = topics != null ? new ArrayList<>(topics) : new ArrayList<>();
        this.totalCount = this.topics.size();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
