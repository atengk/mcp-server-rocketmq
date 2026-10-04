package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 全集群消费组积压排行 TopN 数据传输对象。
 * 承载被评估的消费组总数以及按堆积量降序排列的 TopN 消费组列表。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class TopConsumerLagDTO {

    /**
     * 集群内参与评估的业务消费组总数。
     */
    private int totalEvaluatedGroups = 0;

    /**
     * 截取的 TopN 消费组积压明细列表（按总积压量降序排列）。
     */
    private List<ConsumerLagSummaryDTO> topLags = new ArrayList<>();

    public TopConsumerLagDTO() {
    }

    public TopConsumerLagDTO(int totalEvaluatedGroups, List<ConsumerLagSummaryDTO> topLags) {
        this.totalEvaluatedGroups = totalEvaluatedGroups;
        setTopLags(topLags);
    }

    public int getTotalEvaluatedGroups() {
        return totalEvaluatedGroups;
    }

    public void setTotalEvaluatedGroups(int totalEvaluatedGroups) {
        this.totalEvaluatedGroups = totalEvaluatedGroups;
    }

    public List<ConsumerLagSummaryDTO> getTopLags() {
        return topLags != null ? topLags : Collections.emptyList();
    }

    public void setTopLags(List<ConsumerLagSummaryDTO> topLags) {
        this.topLags = topLags != null ? new ArrayList<>(topLags) : new ArrayList<>();
    }
}
