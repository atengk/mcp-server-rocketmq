package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 消费组列表数据传输对象。
 * 承载集群内检索到的消费组名称集合及总数。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class ConsumerGroupListDTO {

    /**
     * 消费组名称列表。
     */
    private List<String> groups = new ArrayList<>();

    /**
     * 消费组总数。
     */
    private int totalCount = 0;

    public ConsumerGroupListDTO() {
    }

    public ConsumerGroupListDTO(List<String> groups) {
        setGroups(groups);
    }

    public List<String> getGroups() {
        return groups != null ? groups : Collections.emptyList();
    }

    public void setGroups(List<String> groups) {
        this.groups = groups != null ? new ArrayList<>(groups) : new ArrayList<>();
        this.totalCount = this.groups.size();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
