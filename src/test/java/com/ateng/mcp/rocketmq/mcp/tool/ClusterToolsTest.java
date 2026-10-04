package com.ateng.mcp.rocketmq.mcp.tool;

import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerSummaryDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
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
 * 集群拓扑 MCP 工具单元测试。
 * 验证 rocketmq_cluster_info 工具调用与数据封装逻辑。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterTools MCP 工具单元测试")
class ClusterToolsTest {

    @Mock
    private AdminClientService adminClientService;

    @InjectMocks
    private ClusterTools clusterTools;

    @Test
    @DisplayName("验证 rocketmq_cluster_info 工具调用成功返回集群节点拓扑")
    void shouldReturnClusterInfoSuccessfully() throws Exception {
        ClusterInfoDTO mockDto = new ClusterInfoDTO();
        mockDto.setClusterTable(Map.of("DefaultCluster", List.of("broker-a")));
        mockDto.setBrokers(List.of(
                new BrokerSummaryDTO("broker-a", "DefaultCluster", 0L, "MASTER", "192.168.1.10:10911", null),
                new BrokerSummaryDTO("broker-a", "DefaultCluster", 1L, "SLAVE", "192.168.1.11:10911", null)
        ));
        mockDto.setTotalBrokers(2);
        mockDto.setTotalMasters(1);
        mockDto.setTotalSlaves(1);

        when(adminClientService.getClusterInfo()).thenReturn(mockDto);

        ClusterInfoDTO result = clusterTools.getClusterInfo();

        assertThat(result).isNotNull();
        assertThat(result.getTotalBrokers()).isEqualTo(2);
        assertThat(result.getTotalMasters()).isEqualTo(1);
        assertThat(result.getTotalSlaves()).isEqualTo(1);
        assertThat(result.getClusterTable()).containsKey("DefaultCluster");
        assertThat(result.getBrokers()).hasSize(2);
        assertThat(result.getBrokers().getFirst().getRole()).isEqualTo("MASTER");
    }
}
