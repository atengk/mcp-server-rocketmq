package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Broker 运行时核心统计指标数据传输对象。
 * 承载指定 Broker 节点的写入 TPS、出入吞吐量、磁盘水位及运行存活状态。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class BrokerStatsDTO {

    /**
     * Broker 节点物理通信地址（IP:PORT）。
     */
    private String brokerAddr;

    /**
     * Broker 实例标识名称。
     */
    private String brokerName;

    /**
     * 写入 TPS（Put TPS）。
     */
    private String putTps;

    /**
     * 读取/传输 TPS（Get Transferred TPS）。
     */
    private String getTransferredTps;

    /**
     * 写入总数据流量 TPS。
     */
    private String inTotalTps;

    /**
     * 读取/拉取总数据流量 TPS。
     */
    private String outTotalTps;

    /**
     * CommitLog 物理磁盘使用水位比例（例如 0.45 即 45%）。
     */
    private String commitLogDiskRatio;

    /**
     * Broker 节点启动时间戳。
     */
    private Long bootTimestamp;

    /**
     * 节点运行时长描述。
     */
    private String runtime;

    /**
     * 底层返回的完整指标原始键值对映射。
     */
    private Map<String, String> table = new HashMap<>();

    public BrokerStatsDTO() {
    }

    public BrokerStatsDTO(String brokerAddr, String brokerName) {
        this.brokerAddr = brokerAddr;
        this.brokerName = brokerName;
    }

    public String getBrokerAddr() {
        return brokerAddr;
    }

    public void setBrokerAddr(String brokerAddr) {
        this.brokerAddr = brokerAddr;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getPutTps() {
        return putTps;
    }

    public void setPutTps(String putTps) {
        this.putTps = putTps;
    }

    public String getGetTransferredTps() {
        return getTransferredTps;
    }

    public void setGetTransferredTps(String getTransferredTps) {
        this.getTransferredTps = getTransferredTps;
    }

    public String getInTotalTps() {
        return inTotalTps;
    }

    public void setInTotalTps(String inTotalTps) {
        this.inTotalTps = inTotalTps;
    }

    public String getOutTotalTps() {
        return outTotalTps;
    }

    public void setOutTotalTps(String outTotalTps) {
        this.outTotalTps = outTotalTps;
    }

    public String getCommitLogDiskRatio() {
        return commitLogDiskRatio;
    }

    public void setCommitLogDiskRatio(String commitLogDiskRatio) {
        this.commitLogDiskRatio = commitLogDiskRatio;
    }

    public Long getBootTimestamp() {
        return bootTimestamp;
    }

    public void setBootTimestamp(Long bootTimestamp) {
        this.bootTimestamp = bootTimestamp;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public Map<String, String> getTable() {
        return table != null ? table : Collections.emptyMap();
    }

    public void setTable(Map<String, String> table) {
        this.table = table != null ? table : new HashMap<>();
    }
}
