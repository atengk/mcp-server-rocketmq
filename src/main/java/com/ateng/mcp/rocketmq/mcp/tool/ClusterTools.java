package com.ateng.mcp.rocketmq.mcp.tool;

import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 集群拓扑感知与节点探针 MCP 工具集。
 * 提供对 NameServer 及 Broker 集群节点拓扑、主从角色与通信地址的标准化查询能力。
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
}
