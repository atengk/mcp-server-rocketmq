package com.ateng.mcp.rocketmq;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Apache RocketMQ Model Context Protocol (MCP) 服务端主启动类。
 * 基于 Spring AI 2 与 Spring Boot 4 构建，支持 stdio 与 SSE 双模传输自适应。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@SpringBootApplication
@EnableConfigurationProperties(RocketmqProperties.class)
public class McpServerRocketmqApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerRocketmqApplication.class, args);
    }
}
