package com.ateng.mcp.rocketmq.mcp.resource;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 服务端自身状态与运行配置只读资源暴露。
 * 提供 rocketmq://server/status 资源，供 AI 宿主直接读取服务端安全配置与运行指标。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class ServerResources {

    private static final Logger log = LoggerFactory.getLogger(ServerResources.class);

    private final RocketmqProperties properties;
    private final Environment environment;
    private final JsonMapper jsonMapper;

    public ServerResources(RocketmqProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
        this.jsonMapper = JsonMapper.builder().build();
    }

    /**
     * 获取 RocketMQ MCP Server 当前运行状态及四维防护策略快照。
     *
     * @return 格式化后的 JSON 字符串
     */
    @McpResource(
            uri = "rocketmq://server/status",
            description = "RocketMQ MCP Server 自身运行状态、通信传输协议与四维安全防护策略配置。"
    )
    public String getServerStatus() {
        log.info("Reading MCP Resource: rocketmq://server/status");
        String transport = environment.getProperty("mcp.transport");
        if (transport == null || transport.isBlank()) {
            transport = environment.getProperty("MCP_TRANSPORT", "sse");
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("serverName", "mcp-server-rocketmq");
        status.put("version", "1.0.0");
        status.put("transport", transport);
        status.put("namesrvAddr", properties.getNamesrvAddr());
        status.put("endpoints", properties.getEndpoints());
        status.put("readOnly", properties.isReadOnly());
        status.put("enableDestructiveTools", properties.isEnableDestructiveTools());
        status.put("javaVersion", System.getProperty("java.version"));

        try {
            return jsonMapper.writeValueAsString(status);
        } catch (Exception e) {
            log.error("Failed to serialize server status resource: {}", e.getMessage());
            return "{\"status\":\"DOWN\"}";
        }
    }
}
