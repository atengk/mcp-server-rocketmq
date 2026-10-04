package com.ateng.mcp.rocketmq.mcp.resource;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP 状态资源单元测试。
 * 验证 rocketmq://server/status 资源暴露内容与安全策略状态。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@DisplayName("ServerResources MCP 资源单元测试")
class ServerResourcesTest {

    @Test
    @DisplayName("验证 rocketmq://server/status 返回符合预期的系统状态与安全开关 JSON")
    void shouldReturnServerStatusResource() {
        RocketmqProperties properties = new RocketmqProperties();
        properties.setNamesrvAddr("192.168.1.100:9876");
        properties.setEndpoints("192.168.1.100:8081");
        properties.setReadOnly(true);
        properties.setEnableDestructiveTools(false);

        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("mcp.transport", "stdio");

        ServerResources serverResources = new ServerResources(properties, environment);
        String statusJson = serverResources.getServerStatus();

        assertThat(statusJson).isNotBlank();
        assertThat(statusJson).contains("\"status\":\"UP\"");
        assertThat(statusJson).contains("\"transport\":\"stdio\"");
        assertThat(statusJson).contains("\"namesrvAddr\":\"192.168.1.100:9876\"");
        assertThat(statusJson).contains("\"endpoints\":\"192.168.1.100:8081\"");
        assertThat(statusJson).contains("\"readOnly\":true");
        assertThat(statusJson).contains("\"enableDestructiveTools\":false");
    }
}
