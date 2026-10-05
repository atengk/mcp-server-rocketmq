package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

/**
 * 消息投递轨迹阶段节点数据传输对象。
 * 承载消息在发送端、Broker 存储端及消费端各生命周期环节的耗时与状态。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class MessageTraceNodeDTO {

    /**
     * 轨迹阶段名称（如 Pub 发送、Broker 存储、Sub 消费、Sub_Return 消费应答）。
     */
    private String stage;

    /**
     * 关联客户端或 Broker 节点网络通信地址。
     */
    private String clientHost;

    /**
     * 该环节执行耗时（毫秒）。
     */
    private int costTime = 0;

    /**
     * 事件发生时间戳。
     */
    private long timestamp = 0L;

    /**
     * 执行结果状态（如 SUCCESS、FAILED 等）。
     */
    private String status;

    /**
     * 生产或消费组名称。
     */
    private String groupName;

    public MessageTraceNodeDTO() {
    }

    public MessageTraceNodeDTO(String stage, String clientHost, int costTime, long timestamp, String status, String groupName) {
        this.stage = stage;
        this.clientHost = clientHost;
        this.costTime = costTime;
        this.timestamp = timestamp;
        this.status = status;
        this.groupName = groupName;
    }

    public String getStage() {
        return stage;
    }

    public String getNodeType() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public void setNodeType(String nodeType) {
        this.stage = nodeType;
    }

    public String getClientHost() {
        return clientHost;
    }

    public void setClientHost(String clientHost) {
        this.clientHost = clientHost;
    }

    public int getCostTime() {
        return costTime;
    }

    public void setCostTime(int costTime) {
        this.costTime = costTime;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
