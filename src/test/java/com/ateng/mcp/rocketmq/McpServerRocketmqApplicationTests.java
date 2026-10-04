package com.ateng.mcp.rocketmq;

import com.ateng.mcp.rocketmq.mcp.resource.ServerResources;
import com.ateng.mcp.rocketmq.mcp.tool.ClusterTools;
import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerSummaryDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Spring Boot 上下文集成与高阶测试接缝验证。
 * 验证 MCP 服务端上下文加载、Tool 回调与 Resource 获取端到端切片。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("McpServerRocketmqApplication 集成测试")
class McpServerRocketmqApplicationTests {

    @MockitoBean
    private AdminClientService adminClientService;

    @Autowired
    private ClusterTools clusterTools;

    @Autowired
    private ServerResources serverResources;

    @Autowired
    private McpSyncServer mcpSyncServer;

    @Test
    @DisplayName("验证 Spring 上下文成功装配 ClusterTools 并可通过主接缝执行工具回调")
    void shouldExecuteClusterToolsCallback() throws Exception {
        ClusterInfoDTO mockDto = new ClusterInfoDTO();
        mockDto.setClusterTable(Map.of("RmqCluster", List.of("broker-0")));
        mockDto.setBrokers(List.of(
                new BrokerSummaryDTO("broker-0", "RmqCluster", 0L, "MASTER", "127.0.0.1:10911", null)
        ));
        mockDto.setTotalBrokers(1);
        mockDto.setTotalMasters(1);
        mockDto.setTotalSlaves(0);

        when(adminClientService.getClusterInfo()).thenReturn(mockDto);

        ClusterInfoDTO result = clusterTools.getClusterInfo();
        assertThat(result).isNotNull();
        assertThat(result.getTotalBrokers()).isEqualTo(1);
        assertThat(result.getTotalMasters()).isEqualTo(1);
        assertThat(result.getClusterTable()).containsKey("RmqCluster");
    }

    @Test
    @DisplayName("验证 ServerResources 可在集成上下文中输出格式化 JSON 状态")
    void shouldReadServerStatusResource() {
        String statusJson = serverResources.getServerStatus();
        assertThat(statusJson).isNotBlank();
        assertThat(statusJson).contains("\"status\":\"UP\"");
        assertThat(statusJson).contains("\"serverName\":\"mcp-server-rocketmq\"");
    }

    @Test
    @DisplayName("验证 Spring AI MCP 框架自动扫描并注册工具与资源规格至 MCP 分发层")
    void shouldRegisterMcpToolsAndResourcesWithSpringAi() {
        assertThat(mcpSyncServer).isNotNull();

        List<McpSchema.Tool> tools = mcpSyncServer.listTools();
        assertThat(tools).isNotNull().isNotEmpty();
        assertThat(tools).anyMatch(tool -> "rocketmq_cluster_info".equals(tool.name()));

        List<McpSchema.Resource> resources = mcpSyncServer.listResources();
        assertThat(resources).isNotNull().isNotEmpty();
        assertThat(resources).anyMatch(resource -> "rocketmq://server/status".equals(resource.uri()));
    }
}
