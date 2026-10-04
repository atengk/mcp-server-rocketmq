package com.ateng.mcp.rocketmq.mcp.resource;

import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicOverviewDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * 集群业务主题与队列概览只读资源暴露组件。
 * 提供 rocketmq://topics 资源，供 AI 宿主无需工具调用直接挂载业务主题列表与队列拓扑概览快照。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class TopicResources {

    private static final Logger log = LoggerFactory.getLogger(TopicResources.class);

    private final AdminClientService adminClientService;
    private final JsonMapper jsonMapper;

    public TopicResources(AdminClientService adminClientService) {
        this.adminClientService = adminClientService;
        this.jsonMapper = JsonMapper.builder().build();
    }

    /**
     * 获取当前 RocketMQ 集群的业务主题列表与队列概览快照（默认自动过滤内置系统主题）。
     *
     * @return 格式化后的 JSON 字符串
     */
    @McpResource(
            uri = "rocketmq://topics",
            description = "RocketMQ 业务主题概览快照，包含集群内所有业务主题名称列表、读写队列规模与分布节点概览（自动过滤内置系统主题）。"
    )
    public String getTopicsOverview() {
        log.info("Reading MCP Resource: rocketmq://topics");
        try {
            TopicOverviewDTO overview = adminClientService.getTopicsOverview();
            return jsonMapper.writeValueAsString(overview);
        } catch (Exception e) {
            log.error("Failed to read topics overview resource: {}", e.getMessage());
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            try {
                return jsonMapper.writeValueAsString(Map.of("error", errorMsg));
            } catch (Exception ex) {
                return "{\"error\":\"" + errorMsg.replace("\"", "'") + "\"}";
            }
        }
    }
}
