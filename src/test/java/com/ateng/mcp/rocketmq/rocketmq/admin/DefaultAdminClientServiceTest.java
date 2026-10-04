package com.ateng.mcp.rocketmq.rocketmq.admin;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerStatsDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.body.KVTable;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RocketMQ 运维管理服务单元测试。
 * 验证底层 ClusterInfo 报文解析、真实指标提取、名称地址映射与网络断连自愈逻辑。
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
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

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

        when(mockClient.examineBrokerClusterInfo()).thenReturn(clusterInfo);

        ClusterInfoDTO dto = service.getClusterInfo();

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalBrokers()).isEqualTo(2);
        assertThat(dto.getTotalMasters()).isEqualTo(1);
        assertThat(dto.getTotalSlaves()).isEqualTo(1);
        assertThat(dto.getClusterTable()).containsKey("DefaultCluster");
        assertThat(dto.getBrokers()).hasSize(2);
    }

    @Test
    @DisplayName("验证生产环境 getBrokerStats 真实指标提取解析逻辑")
    void shouldExtractBrokerStatsThroughProductionLogic() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        KVTable kvTable = new KVTable();
        HashMap<String, String> table = new HashMap<>();
        table.put("putTps", "125.8");
        table.put("getTransferredTps", "340.2");
        table.put("inTotalTps", "125.8");
        table.put("outTotalTps", "340.2");
        table.put("commitLogDiskRatio", "0.42");
        table.put("bootTimestamp", "1728000000000");
        table.put("runtime", "5d 12h");
        kvTable.setTable(table);

        when(mockClient.fetchBrokerRuntimeStats("192.168.1.10:10911")).thenReturn(kvTable);

        // 调用生产的 getBrokerStats 方法（不再由测试桩重写覆盖）
        BrokerStatsDTO dto = service.getBrokerStats("192.168.1.10:10911");

        assertThat(dto).isNotNull();
        assertThat(dto.getBrokerAddr()).isEqualTo("192.168.1.10:10911");
        assertThat(dto.getPutTps()).isEqualTo("125.8");
        assertThat(dto.getGetTransferredTps()).isEqualTo("340.2");
        assertThat(dto.getInTotalTps()).isEqualTo("125.8");
        assertThat(dto.getOutTotalTps()).isEqualTo("340.2");
        assertThat(dto.getCommitLogDiskRatio()).isEqualTo("0.42");
        assertThat(dto.getBootTimestamp()).isEqualTo(1728000000000L);
        assertThat(dto.getRuntime()).isEqualTo("5d 12h");
        assertThat(dto.getTable()).containsEntry("putTps", "125.8");
    }

    @Test
    @DisplayName("验证网络连接异常触发自愈重连机制并成功重试")
    void shouldTriggerSelfHealingReconnectOnNetworkFailure() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        KVTable kvTable = new KVTable();
        HashMap<String, String> table = new HashMap<>();
        table.put("putTps", "100.0");
        kvTable.setTable(table);

        // 第一次调用抛出 RemotingConnectException，第二次重试成功返回数据
        when(mockClient.fetchBrokerRuntimeStats("192.168.1.10:10911"))
                .thenThrow(new RemotingConnectException("Connection dropped"))
                .thenReturn(kvTable);

        BrokerStatsDTO dto = service.getBrokerStats("192.168.1.10:10911");

        assertThat(dto).isNotNull();
        assertThat(dto.getPutTps()).isEqualTo("100.0");
        verify(mockClient, times(2)).fetchBrokerRuntimeStats("192.168.1.10:10911");
    }

    @Test
    @DisplayName("验证传入 Broker 名称时自动解析为 Master 物理地址")
    void shouldResolveBrokerNameToMasterAddress() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        ClusterInfo clusterInfo = new ClusterInfo();
        HashMap<String, BrokerData> brokerAddrTable = new HashMap<>();
        BrokerData brokerData = new BrokerData();
        brokerData.setBrokerName("broker-a");
        HashMap<Long, String> addrs = new HashMap<>();
        addrs.put(0L, "192.168.1.10:10911");
        addrs.put(1L, "192.168.1.11:10911");
        brokerData.setBrokerAddrs(addrs);
        brokerAddrTable.put("broker-a", brokerData);
        clusterInfo.setBrokerAddrTable(brokerAddrTable);

        when(mockClient.examineBrokerClusterInfo()).thenReturn(clusterInfo);

        String resolved = service.resolveBrokerAddress("broker-a");
        assertThat(resolved).isEqualTo("192.168.1.10:10911");
    }

    @Test
    @DisplayName("验证非法空参数时抛出合法校验异常")
    void shouldThrowExceptionWhenBrokerAddrIsBlank() {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        assertThatThrownBy(() -> service.getBrokerStats(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static class TestableAdminClientService extends DefaultAdminClientService {
        private final DefaultMQAdminExt mockClient;

        public TestableAdminClientService(RocketmqProperties properties, DefaultMQAdminExt mockClient) {
            super(properties);
            this.mockClient = mockClient;
        }

        @Override
        protected DefaultMQAdminExt createMQAdminExt() {
            return mockClient;
        }

        @Override
        protected DefaultMQAdminExt ensureStarted() {
            return mockClient;
        }
    }
}
