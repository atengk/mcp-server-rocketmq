package io.github.atengk.mcp.rocketmq.mcp.tool;

import io.github.atengk.mcp.rocketmq.rocketmq.admin.AdminClientService;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.BrokerStatsDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 集群拓扑感知与节点探针 MCP 工具集。
 * 提供对 NameServer 及 Broker 集群节点拓扑、主从角色、网络通信地址与运行时指标的标准化查询能力。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class ClusterTools {

    private static final Logger log = LoggerFactory.getLogger(ClusterTools.class);

    private final AdminClientService adminClientService;

    public ClusterTools(AdminClientService adminClientService) {
        this.adminClientService = adminClientService;
    }

    /**
     * 查询 RocketMQ 集群的物理节点拓扑、Broker 分布与主从状态。
     *
     * @return 格式化后的集群拓扑与 Broker 节点摘要数据
     * @throws Exception 当底层通信或查询异常时抛出
     */
    @McpTool(
            name = "rocketmq_cluster_info",
            description = "查询 RocketMQ 集群的基础节点拓扑分布、Broker 主从角色与网络地址信息。"
    )
    public ClusterInfoDTO getClusterInfo() throws Exception {
        log.info("Executing MCP Tool: rocketmq_cluster_info");
        return adminClientService.getClusterInfo();
    }

    /**
     * 查询指定 RocketMQ Broker 节点的实时运行时指标，包括写入 TPS、吞吐速率及磁盘物理水位。
     *
     * @param brokerAddr Broker 网络通信地址（IP:PORT）或实例标识名称（如 broker-a）
     * @return 格式化后的 BrokerStatsDTO 运行时指标对象
     * @throws Exception 当底层通信或指标提取异常时抛出
     */
    @McpTool(
            name = "rocketmq_broker_stats",
            description = "查询指定 RocketMQ Broker 节点的实时运行时指标，包括消息写入 TPS、出入吞吐量、磁盘物理水位及运行存活状态。"
    )
    public BrokerStatsDTO getBrokerStats(
            @McpToolParam(description = "Broker 网络通信地址（IP:PORT）或实例标识名称（如 broker-a）", required = true)
            String brokerAddr) throws Exception {
        log.info("Executing MCP Tool: rocketmq_broker_stats with broker: {}", brokerAddr);
        return adminClientService.getBrokerStats(brokerAddr);
    }
}
