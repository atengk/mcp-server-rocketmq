package com.ateng.mcp.rocketmq.mcp.resource;

import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * 集群物理拓扑只读资源暴露组件。
 * 提供 rocketmq://cluster/topology 资源，供 AI 宿主无需工具调用直接挂载物理集群主从拓扑快照。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class ClusterResources {

    private static final Logger log = LoggerFactory.getLogger(ClusterResources.class);

    private final AdminClientService adminClientService;
    private final JsonMapper jsonMapper;

    public ClusterResources(AdminClientService adminClientService) {
        this.adminClientService = adminClientService;
        this.jsonMapper = JsonMapper.builder().build();
    }

    /**
     * 获取当前 RocketMQ 物理集群拓扑与 Broker 节点在线状态快照。
     *
     * @return 格式化后的 JSON 字符串
     */
    @McpResource(
            uri = "rocketmq://cluster/topology",
            description = "RocketMQ 物理集群拓扑快照，包含物理集群划分、Broker 节点分布、Master/Slave 主从角色与网络地址。"
    )
    public String getClusterTopology() {
        log.info("Reading MCP Resource: rocketmq://cluster/topology");
        try {
            ClusterInfoDTO clusterInfo = adminClientService.getClusterInfo();
            return jsonMapper.writeValueAsString(clusterInfo);
        } catch (Exception e) {
            log.error("Failed to read cluster topology resource: {}", e.getMessage());
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            try {
                return jsonMapper.writeValueAsString(Map.of("error", errorMsg));
            } catch (Exception ex) {
                return "{\"error\":\"" + errorMsg.replace("\"", "'") + "\"}";
            }
        }
    }
}
