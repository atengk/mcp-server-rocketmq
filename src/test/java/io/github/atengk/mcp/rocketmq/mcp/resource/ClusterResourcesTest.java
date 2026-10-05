package io.github.atengk.mcp.rocketmq.mcp.resource;

import io.github.atengk.mcp.rocketmq.rocketmq.admin.AdminClientService;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.BrokerSummaryDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 集群物理拓扑 MCP 资源单元测试。
 * 验证 rocketmq://cluster/topology 资源序列化与节点信息输出。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterResources MCP 资源单元测试")
class ClusterResourcesTest {

    @Mock
    private AdminClientService adminClientService;

    @InjectMocks
    private ClusterResources clusterResources;

    @Test
    @DisplayName("验证 rocketmq://cluster/topology 返回正确的拓扑快照 JSON")
    void shouldReturnClusterTopologyResourceJson() throws Exception {
        ClusterInfoDTO mockDto = new ClusterInfoDTO();
        mockDto.setClusterTable(Map.of("BenchmarkCluster", List.of("broker-0")));
        mockDto.setBrokers(List.of(
                new BrokerSummaryDTO("broker-0", "BenchmarkCluster", 0L, "MASTER", "10.0.0.1:10911", null)
        ));
        mockDto.setTotalBrokers(1);
        mockDto.setTotalMasters(1);
        mockDto.setTotalSlaves(0);

        when(adminClientService.getClusterInfo()).thenReturn(mockDto);

        String json = clusterResources.getClusterTopology();

        assertThat(json).isNotBlank();
        assertThat(json).contains("\"BenchmarkCluster\"");
        assertThat(json).contains("\"broker-0\"");
        assertThat(json).contains("\"MASTER\"");
        assertThat(json).contains("\"10.0.0.1:10911\"");
        assertThat(json).contains("\"totalBrokers\":1");
    }

    @Test
    @DisplayName("当底层查询异常时应返回受控的 JSON 错误提示")
    void shouldHandleExceptionGracefully() throws Exception {
        when(adminClientService.getClusterInfo()).thenThrow(new RuntimeException("Connection timeout"));

        String json = clusterResources.getClusterTopology();

        assertThat(json).isNotBlank();
        assertThat(json).contains("\"error\":\"Connection timeout\"");
    }
}
