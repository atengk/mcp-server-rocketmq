package com.ateng.mcp.rocketmq.rocketmq.admin;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RocketMQ 运维管理服务单元测试。
 * 验证底层 ClusterInfo 报文解析与 DTO 拓扑映射逻辑。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@DisplayName("DefaultAdminClientService 单元测试")
class DefaultAdminClientServiceTest {

    @Test
    @DisplayName("验证将原生 ClusterInfo 正确映射为 ClusterInfoDTO")
    void shouldMapClusterInfoToDtoCorrectly() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        TestableAdminClientService service = new TestableAdminClientService(properties);

        ClusterInfo clusterInfo = new ClusterInfo();
        HashMap<String, Set<String>> clusterTable = new HashMap<>();
        clusterTable.put("DefaultCluster", Set.of("broker-a"));
        clusterInfo.setClusterAddrTable(clusterTable);

        HashMap<String, BrokerData> brokerAddrTable = new HashMap<>();
        BrokerData brokerData = new BrokerData();
        brokerData.setBrokerName("broker-a");
        brokerData.setCluster("DefaultCluster");
        HashMap<Long, String> addrs = new HashMap<>();
        addrs.put(0L, "192.168.1.10:10911");
        addrs.put(1L, "192.168.1.11:10911");
        brokerData.setBrokerAddrs(addrs);
        brokerAddrTable.put("broker-a", brokerData);
        clusterInfo.setBrokerAddrTable(brokerAddrTable);

        service.setMockClusterInfo(clusterInfo);

        ClusterInfoDTO dto = service.getClusterInfo();

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalBrokers()).isEqualTo(2);
        assertThat(dto.getTotalMasters()).isEqualTo(1);
        assertThat(dto.getTotalSlaves()).isEqualTo(1);
        assertThat(dto.getClusterTable()).containsKey("DefaultCluster");
        assertThat(dto.getBrokers()).hasSize(2);
    }

    @Test
    @DisplayName("当底层 ClusterInfo 为空时应保底返回空 DTO 对象")
    void shouldHandleNullOrEmptyClusterInfoGracefully() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        TestableAdminClientService service = new TestableAdminClientService(properties);
        service.setMockClusterInfo(null);

        ClusterInfoDTO dto = service.getClusterInfo();

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalBrokers()).isEqualTo(0);
        assertThat(dto.getBrokers()).isEmpty();
        assertThat(dto.getClusterTable()).isEmpty();
    }

    private static class TestableAdminClientService extends DefaultAdminClientService {
        private ClusterInfo mockClusterInfo;

        public TestableAdminClientService(RocketmqProperties properties) {
            super(properties);
        }

        public void setMockClusterInfo(ClusterInfo mockClusterInfo) {
            this.mockClusterInfo = mockClusterInfo;
        }

        @Override
        public ClusterInfo examineBrokerClusterInfo() {
            return mockClusterInfo;
        }
    }
}
