package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

/**
 * Broker 节点摘要数据传输对象。
 * 承载单个 Broker 节点的物理地址、主从角色及所属集群元数据。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class BrokerSummaryDTO {

    /**
     * Broker 实例标识名称。
     */
    private String brokerName;

    /**
     * 所属物理集群名称。
     */
    private String cluster;

    /**
     * Broker 节点编号，0 表示 Master，大于 0 表示 Slave。
     */
    private Long brokerId;

    /**
     * 节点角色：MASTER 或 SLAVE。
     */
    private String role;

    /**
     * 节点网络通信地址（IP:PORT）。
     */
    private String address;

    /**
     * 节点服务版本描述。
     */
    private String version;

    public BrokerSummaryDTO() {
    }

    public BrokerSummaryDTO(String brokerName, String cluster, Long brokerId, String role, String address, String version) {
        this.brokerName = brokerName;
        this.cluster = cluster;
        this.brokerId = brokerId;
        this.role = role;
        this.address = address;
        this.version = version;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public Long getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(Long brokerId) {
        this.brokerId = brokerId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
