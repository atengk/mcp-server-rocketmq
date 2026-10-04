package com.ateng.mcp.rocketmq.mcp.tool;

import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicRouteDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 主题元数据、队列路由与位点状态 MCP 工具集。
 * 提供主题发现、读写队列分布拓扑、以及各队列最小/最大位点与容量感知的标准化查询能力。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class TopicTools {

    private static final Logger log = LoggerFactory.getLogger(TopicTools.class);

    private final AdminClientService adminClientService;

    public TopicTools(AdminClientService adminClientService) {
        this.adminClientService = adminClientService;
    }

    /**
     * 查询 RocketMQ 集群中的所有主题列表，默认自动过滤系统内部主题。
     *
     * @param includeSystemTopics 是否包含系统内置主题（如 %SYS%、TBW102 等），默认为 false
     * @return 格式化后的主题列表与统计对象
     * @throws Exception 当底层通信或查询异常时抛出
     */
    @McpTool(
            name = "rocketmq_list_topics",
            description = "查询 RocketMQ 集群中的主题列表，默认自动过滤系统内置主题（如 %SYS%、TBW102 等）。"
    )
    public TopicListDTO listTopics(
            @McpToolParam(description = "是否包含系统内部保留主题，默认为 false", required = false)
            Boolean includeSystemTopics) throws Exception {
        boolean includeSystem = Boolean.TRUE.equals(includeSystemTopics);
        log.info("Executing MCP Tool: rocketmq_list_topics with includeSystem: {}", includeSystem);
        return adminClientService.listTopics(includeSystem);
    }

    /**
     * 查询指定主题的读写队列分布与 Broker 路由详情。
     *
     * @param topic 目标主题名称
     * @return 格式化后的 TopicRouteDTO 路由详情对象
     * @throws Exception 当底层通信或路由查询异常时抛出
     */
    @McpTool(
            name = "rocketmq_topic_route",
            description = "查询指定 RocketMQ 主题的读写队列分布、Broker 物理路由拓扑与权限配置。"
    )
    public TopicRouteDTO getTopicRoute(
            @McpToolParam(description = "目标主题名称", required = true)
            String topic) throws Exception {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic name must not be blank");
        }
        log.info("Executing MCP Tool: rocketmq_topic_route with topic: {}", topic);
        return adminClientService.getTopicRoute(topic.trim());
    }

    /**
     * 查询指定主题各分片队列的位点统计与消息留存容量。
     *
     * @param topic 目标主题名称
     * @return 格式化后的 TopicStatusDTO 位点状态与容量统计对象
     * @throws Exception 当底层通信或位点计算异常时抛出
     */
    @McpTool(
            name = "rocketmq_topic_status",
            description = "查询指定 RocketMQ 主题各分片队列的最小/最大位点与消息留存总量统计。"
    )
    public TopicStatusDTO getTopicStatus(
            @McpToolParam(description = "目标主题名称", required = true)
            String topic) throws Exception {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic name must not be blank");
        }
        log.info("Executing MCP Tool: rocketmq_topic_status with topic: {}", topic);
        return adminClientService.getTopicStatus(topic.trim());
    }
}
