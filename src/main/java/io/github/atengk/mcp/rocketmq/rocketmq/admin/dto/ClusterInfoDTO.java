package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 集群整体物理拓扑与节点分布数据传输对象。
 * 汇总集群下各 Broker 节点分布、主从拓扑及健康统计数据。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class ClusterInfoDTO {

    /**
     * 集群与所属 Broker 节点名称映射字典。
     */
    private Map<String, List<String>> clusterTable = new HashMap<>();

    /**
     * 扁平化的 Broker 节点明细列表。
     */
    private List<BrokerSummaryDTO> brokers = new ArrayList<>();

    /**
     * 集群内注册的 Broker 节点总数。
     */
    private int totalBrokers = 0;

    /**
     * 集群内 Master 主节点总数。
     */
    private int totalMasters = 0;

    /**
     * 集群内 Slave 从节点总数。
     */
    private int totalSlaves = 0;

    public ClusterInfoDTO() {
    }

    public Map<String, List<String>> getClusterTable() {
        return clusterTable != null ? clusterTable : Collections.emptyMap();
    }

    public void setClusterTable(Map<String, List<String>> clusterTable) {
        this.clusterTable = clusterTable != null ? clusterTable : new HashMap<>();
    }

    public List<BrokerSummaryDTO> getBrokers() {
        return brokers != null ? brokers : Collections.emptyList();
    }

    public void setBrokers(List<BrokerSummaryDTO> brokers) {
        this.brokers = brokers != null ? brokers : new ArrayList<>();
    }

    public int getTotalBrokers() {
        return totalBrokers;
    }

    public void setTotalBrokers(int totalBrokers) {
        this.totalBrokers = totalBrokers;
    }

    public int getTotalMasters() {
        return totalMasters;
    }

    public void setTotalMasters(int totalMasters) {
        this.totalMasters = totalMasters;
    }

    public int getTotalSlaves() {
        return totalSlaves;
    }

    public void setTotalSlaves(int totalSlaves) {
        this.totalSlaves = totalSlaves;
    }
}
