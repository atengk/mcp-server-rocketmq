package com.ateng.mcp.rocketmq;

import com.ateng.mcp.rocketmq.mcp.resource.ClusterResources;
import com.ateng.mcp.rocketmq.mcp.resource.ServerResources;
import com.ateng.mcp.rocketmq.mcp.resource.TopicResources;
import com.ateng.mcp.rocketmq.mcp.tool.ClusterTools;
import com.ateng.mcp.rocketmq.mcp.tool.ConsumerTools;
import com.ateng.mcp.rocketmq.mcp.tool.TopicTools;
import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerStatsDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerSummaryDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ConsumerConnectionDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ConsumerGroupListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ConsumerLagDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ConsumerLagSummaryDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopConsumerLagDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicOverviewDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicRouteDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicStatusDTO;
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
    private TopicTools topicTools;

    @Autowired
    private ConsumerTools consumerTools;

    @Autowired
    private ServerResources serverResources;

    @Autowired
    private ClusterResources clusterResources;

    @Autowired
    private TopicResources topicResources;

    @Autowired
    private McpSyncServer mcpSyncServer;

    @Test
    @DisplayName("验证 Spring 上下文成功装配 ClusterTools 并可通过主接缝执行集群拓扑与 Broker 指标回调")
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

        BrokerStatsDTO mockStats = new BrokerStatsDTO("127.0.0.1:10911", "broker-0");
        mockStats.setPutTps("88.8");
        mockStats.setCommitLogDiskRatio("0.35");
        when(adminClientService.getBrokerStats("broker-0")).thenReturn(mockStats);

        BrokerStatsDTO statsResult = clusterTools.getBrokerStats("broker-0");
        assertThat(statsResult).isNotNull();
        assertThat(statsResult.getPutTps()).isEqualTo("88.8");
        assertThat(statsResult.getCommitLogDiskRatio()).isEqualTo("0.35");
    }

    @Test
    @DisplayName("验证 Spring 上下文成功装配 TopicTools 与 TopicResources 并执行端到端回调")
    void shouldExecuteTopicToolsAndResourcesCallback() throws Exception {
        TopicListDTO mockTopicList = new TopicListDTO(List.of("CartTopic"));
        when(adminClientService.listTopics(false)).thenReturn(mockTopicList);

        TopicListDTO listResult = topicTools.listTopics(false);
        assertThat(listResult).isNotNull();
        assertThat(listResult.getTopics()).containsExactly("CartTopic");

        TopicRouteDTO mockRoute = new TopicRouteDTO("CartTopic");
        when(adminClientService.getTopicRoute("CartTopic")).thenReturn(mockRoute);

        TopicRouteDTO routeResult = topicTools.getTopicRoute("CartTopic");
        assertThat(routeResult).isNotNull();
        assertThat(routeResult.getTopic()).isEqualTo("CartTopic");

        TopicStatusDTO mockStatus = new TopicStatusDTO("CartTopic");
        mockStatus.setTotalMessages(999L);
        when(adminClientService.getTopicStatus("CartTopic")).thenReturn(mockStatus);

        TopicStatusDTO statusResult = topicTools.getTopicStatus("CartTopic");
        assertThat(statusResult).isNotNull();
        assertThat(statusResult.getTotalMessages()).isEqualTo(999L);

        TopicOverviewDTO.TopicSummaryDTO summary = new TopicOverviewDTO.TopicSummaryDTO("CartTopic", 8, 8, List.of("broker-0"));
        when(adminClientService.getTopicsOverview()).thenReturn(new TopicOverviewDTO(List.of(summary)));

        String overviewJson = topicResources.getTopicsOverview();
        assertThat(overviewJson).isNotBlank();
        assertThat(overviewJson).contains("\"CartTopic\"");
        assertThat(overviewJson).contains("\"totalReadQueues\":8");
    }

    @Test
    @DisplayName("验证 Spring 上下文成功装配 ConsumerTools 并执行端到端消费组工具回调")
    void shouldExecuteConsumerToolsCallback() throws Exception {
        ConsumerGroupListDTO mockGroups = new ConsumerGroupListDTO(List.of("demo_consumer_group"));
        when(adminClientService.listConsumerGroups(false)).thenReturn(mockGroups);

        ConsumerGroupListDTO listResult = consumerTools.listConsumerGroups(false);
        assertThat(listResult).isNotNull();
        assertThat(listResult.getGroups()).containsExactly("demo_consumer_group");

        ConsumerConnectionDTO mockConn = new ConsumerConnectionDTO("demo_consumer_group");
        mockConn.setTotalClients(1);
        when(adminClientService.getConsumerStatus("demo_consumer_group")).thenReturn(mockConn);

        ConsumerConnectionDTO statusResult = consumerTools.getConsumerStatus("demo_consumer_group");
        assertThat(statusResult).isNotNull();
        assertThat(statusResult.getTotalClients()).isEqualTo(1);

        ConsumerLagDTO mockLag = new ConsumerLagDTO("demo_consumer_group");
        mockLag.setTotalLag(1234L);
        when(adminClientService.getConsumerLag("demo_consumer_group", null)).thenReturn(mockLag);

        ConsumerLagDTO lagResult = consumerTools.getConsumerLag("demo_consumer_group", null);
        assertThat(lagResult).isNotNull();
        assertThat(lagResult.getTotalLag()).isEqualTo(1234L);

        ConsumerLagSummaryDTO summary = new ConsumerLagSummaryDTO("demo_consumer_group", 1234L, 20.0);
        TopConsumerLagDTO mockTop = new TopConsumerLagDTO(1, List.of(summary));
        when(adminClientService.getTopConsumerLag(10)).thenReturn(mockTop);

        TopConsumerLagDTO topResult = consumerTools.getTopConsumerLag(10);
        assertThat(topResult).isNotNull();
        assertThat(topResult.getTopLags()).hasSize(1);
        assertThat(topResult.getTopLags().getFirst().getConsumerGroup()).isEqualTo("demo_consumer_group");
    }

    @Test
    @DisplayName("验证 ServerResources 与 ClusterResources 可在集成上下文中输出格式化 JSON 状态与拓扑")
    void shouldReadResourcesSuccessfully() throws Exception {
        String statusJson = serverResources.getServerStatus();
        assertThat(statusJson).isNotBlank();
        assertThat(statusJson).contains("\"status\":\"UP\"");
        assertThat(statusJson).contains("\"serverName\":\"mcp-server-rocketmq\"");

        ClusterInfoDTO mockDto = new ClusterInfoDTO();
        mockDto.setTotalBrokers(3);
        when(adminClientService.getClusterInfo()).thenReturn(mockDto);

        String topologyJson = clusterResources.getClusterTopology();
        assertThat(topologyJson).isNotBlank();
        assertThat(topologyJson).contains("\"totalBrokers\":3");
    }

    @Test
    @DisplayName("验证 Spring AI MCP 框架自动扫描并注册 9 项 Tools 与 3 项 Resources 至分发层")
    void shouldRegisterMcpToolsAndResourcesWithSpringAi() {
        assertThat(mcpSyncServer).isNotNull();

        List<McpSchema.Tool> tools = mcpSyncServer.listTools();
        assertThat(tools).isNotNull().hasSizeGreaterThanOrEqualTo(9);
        assertThat(tools).anyMatch(tool -> "rocketmq_cluster_info".equals(tool.name()));
        assertThat(tools).anyMatch(tool -> "rocketmq_broker_stats".equals(tool.name()));
        assertThat(tools).anyMatch(tool -> "rocketmq_list_topics".equals(tool.name()));
        assertThat(tools).anyMatch(tool -> "rocketmq_topic_route".equals(tool.name()));
        assertThat(tools).anyMatch(tool -> "rocketmq_topic_status".equals(tool.name()));
        assertThat(tools).anyMatch(tool -> "rocketmq_list_consumer_groups".equals(tool.name()));
        assertThat(tools).anyMatch(tool -> "rocketmq_consumer_status".equals(tool.name()));
        assertThat(tools).anyMatch(tool -> "rocketmq_consumer_lag".equals(tool.name()));
        assertThat(tools).anyMatch(tool -> "rocketmq_top_consumer_lag".equals(tool.name()));

        List<McpSchema.Resource> resources = mcpSyncServer.listResources();
        assertThat(resources).isNotNull().hasSizeGreaterThanOrEqualTo(3);
        assertThat(resources).anyMatch(resource -> "rocketmq://server/status".equals(resource.uri()));
        assertThat(resources).anyMatch(resource -> "rocketmq://cluster/topology".equals(resource.uri()));
        assertThat(resources).anyMatch(resource -> "rocketmq://topics".equals(resource.uri()));
    }
}
