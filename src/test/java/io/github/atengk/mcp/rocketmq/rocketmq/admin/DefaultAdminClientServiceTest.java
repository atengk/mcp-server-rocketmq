package io.github.atengk.mcp.rocketmq.rocketmq.admin;

import io.github.atengk.mcp.rocketmq.config.RocketmqProperties;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.BrokerStatsDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerConnectionDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerGroupListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerLagDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.DlqMessageListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.MessageDetailDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.MessageListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.MessageTraceDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ResendDlqResultDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ResetOffsetResultDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopConsumerLagDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicOperationResultDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicOverviewDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicRouteDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicStatusDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.messaging.MessagingClientService;
import io.github.atengk.mcp.rocketmq.rocketmq.messaging.dto.SendMessageResultDTO;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper;
import org.apache.rocketmq.remoting.protocol.admin.TopicOffset;
import org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.body.Connection;
import org.apache.rocketmq.remoting.protocol.body.ConsumerConnection;
import org.apache.rocketmq.remoting.protocol.body.KVTable;
import org.apache.rocketmq.remoting.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.remoting.protocol.body.TopicList;
import org.apache.rocketmq.remoting.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.apache.rocketmq.remoting.protocol.route.QueueData;
import org.apache.rocketmq.remoting.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.protocol.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    @DisplayName("验证 listTopics 方法正确根据参数过滤内置系统主题")
    void shouldListTopicsFilteringSystemTopics() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        TopicList topicList = new TopicList();
        Set<String> topics = new HashSet<>();
        topics.add("UserTopic");
        topics.add("OrderTopic");
        topics.add("TBW102");
        topics.add("%SYS%Benchmark");
        topicList.setTopicList(topics);

        when(mockClient.fetchAllTopicList()).thenReturn(topicList);

        // 默认过滤系统主题
        TopicListDTO businessDto = service.listTopics(false);
        assertThat(businessDto).isNotNull();
        assertThat(businessDto.getTotalCount()).isEqualTo(2);
        assertThat(businessDto.getTopics()).containsExactly("OrderTopic", "UserTopic");

        // 显式包含系统主题
        TopicListDTO allDto = service.listTopics(true);
        assertThat(allDto).isNotNull();
        assertThat(allDto.getTotalCount()).isEqualTo(4);
        assertThat(allDto.getTopics()).contains("TBW102", "%SYS%Benchmark", "OrderTopic", "UserTopic");
    }

    @Test
    @DisplayName("验证 getTopicRoute 解析 TopicRouteData 的队列分布与 Broker 地址映射")
    void shouldGetTopicRouteDataCorrectly() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        TopicRouteData routeData = new TopicRouteData();
        List<QueueData> queueDatas = new ArrayList<>();
        QueueData qd = new QueueData();
        qd.setBrokerName("broker-a");
        qd.setReadQueueNums(8);
        qd.setWriteQueueNums(8);
        qd.setPerm(6);
        queueDatas.add(qd);
        routeData.setQueueDatas(queueDatas);

        List<BrokerData> brokerDatas = new ArrayList<>();
        BrokerData bd = new BrokerData();
        bd.setBrokerName("broker-a");
        bd.setCluster("DefaultCluster");
        HashMap<Long, String> addrs = new HashMap<>();
        addrs.put(0L, "192.168.1.10:10911");
        bd.setBrokerAddrs(addrs);
        brokerDatas.add(bd);
        routeData.setBrokerDatas(brokerDatas);

        when(mockClient.examineTopicRouteInfo("TestTopic")).thenReturn(routeData);

        TopicRouteDTO dto = service.getTopicRoute("TestTopic");

        assertThat(dto).isNotNull();
        assertThat(dto.getTopic()).isEqualTo("TestTopic");
        assertThat(dto.getQueueDatas()).hasSize(1);
        assertThat(dto.getQueueDatas().getFirst().getBrokerName()).isEqualTo("broker-a");
        assertThat(dto.getQueueDatas().getFirst().getReadQueueNums()).isEqualTo(8);
        assertThat(dto.getBrokerDatas()).hasSize(1);
        assertThat(dto.getBrokerDatas().getFirst().getRole()).isEqualTo("MASTER");
    }

    @Test
    @DisplayName("验证 getTopicStatus 解析 TopicStatsTable 计算位点与累计消息数量")
    void shouldGetTopicStatsCorrectly() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        TopicStatsTable statsTable = new TopicStatsTable();
        HashMap<MessageQueue, TopicOffset> offsetTable = new HashMap<>();

        MessageQueue mq0 = new MessageQueue("TestTopic", "broker-a", 0);
        TopicOffset offset0 = new TopicOffset();
        offset0.setMinOffset(100L);
        offset0.setMaxOffset(600L);
        offset0.setLastUpdateTimestamp(1728000000000L);
        offsetTable.put(mq0, offset0);

        MessageQueue mq1 = new MessageQueue("TestTopic", "broker-a", 1);
        TopicOffset offset1 = new TopicOffset();
        offset1.setMinOffset(50L);
        offset1.setMaxOffset(350L);
        offset1.setLastUpdateTimestamp(1728000001000L);
        offsetTable.put(mq1, offset1);

        statsTable.setOffsetTable(offsetTable);

        when(mockClient.examineTopicStats("TestTopic")).thenReturn(statsTable);

        TopicStatusDTO dto = service.getTopicStatus("TestTopic");

        assertThat(dto).isNotNull();
        assertThat(dto.getTopic()).isEqualTo("TestTopic");
        assertThat(dto.getTotalMessages()).isEqualTo(800L); // (600-100) + (350-50) = 500 + 300 = 800
        assertThat(dto.getMinOffset()).isEqualTo(50L);
        assertThat(dto.getMaxOffset()).isEqualTo(600L);
        assertThat(dto.getQueues()).hasSize(2);
    }

    @Test
    @DisplayName("验证 getTopicRoute 与 getTopicStatus 传入非法空白主题时防御性拦截")
    void shouldThrowExceptionWhenTopicParamsAreBlank() {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        assertThatThrownBy(() -> service.getTopicRoute(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getTopicStatus(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("验证 getTopicsOverview 正确聚合业务主题列表与各主题读写队列规模")
    void shouldGetTopicsOverviewAggregatingRoutes() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        TopicList topicList = new TopicList();
        topicList.setTopicList(Set.of("PayTopic", "TBW102"));
        when(mockClient.fetchAllTopicList()).thenReturn(topicList);

        TopicRouteData routeData = new TopicRouteData();
        List<QueueData> queueDatas = new ArrayList<>();
        QueueData qd = new QueueData();
        qd.setBrokerName("broker-a");
        qd.setReadQueueNums(4);
        qd.setWriteQueueNums(4);
        queueDatas.add(qd);
        routeData.setQueueDatas(queueDatas);

        when(mockClient.examineTopicRouteInfo("PayTopic")).thenReturn(routeData);

        TopicOverviewDTO overview = service.getTopicsOverview();

        assertThat(overview).isNotNull();
        assertThat(overview.getTotalCount()).isEqualTo(1);
        assertThat(overview.getTopics()).hasSize(1);
        TopicOverviewDTO.TopicSummaryDTO summary = overview.getTopics().getFirst();
        assertThat(summary.getTopic()).isEqualTo("PayTopic");
        assertThat(summary.getTotalReadQueues()).isEqualTo(4);
        assertThat(summary.getTotalWriteQueues()).isEqualTo(4);
        assertThat(summary.getBrokers()).containsExactly("broker-a");
    }

    @Test
    @DisplayName("验证 listConsumerGroups 正确从 Broker 提取消费组并根据参数过滤系统保留组")
    void shouldListConsumerGroupsFilteringSystemGroups() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        ClusterInfo clusterInfo = new ClusterInfo();
        HashMap<String, BrokerData> brokerAddrTable = new HashMap<>();
        BrokerData bd = new BrokerData();
        bd.setBrokerName("broker-a");
        HashMap<Long, String> addrs = new HashMap<>();
        addrs.put(0L, "192.168.1.10:10911");
        bd.setBrokerAddrs(addrs);
        brokerAddrTable.put("broker-a", bd);
        clusterInfo.setBrokerAddrTable(brokerAddrTable);

        when(mockClient.examineBrokerClusterInfo()).thenReturn(clusterInfo);

        SubscriptionGroupWrapper wrapper = new SubscriptionGroupWrapper();
        ConcurrentHashMap<String, SubscriptionGroupConfig> subTable = new ConcurrentHashMap<>();
        subTable.put("order_group", new SubscriptionGroupConfig());
        subTable.put("pay_group", new SubscriptionGroupConfig());
        subTable.put("TOOLS_CONSUMER", new SubscriptionGroupConfig());
        wrapper.setSubscriptionGroupTable(subTable);

        when(mockClient.getAllSubscriptionGroup("192.168.1.10:10911", 3000L)).thenReturn(wrapper);

        // 默认过滤系统消费组
        ConsumerGroupListDTO businessGroups = service.listConsumerGroups(false);
        assertThat(businessGroups).isNotNull();
        assertThat(businessGroups.getTotalCount()).isEqualTo(2);
        assertThat(businessGroups.getGroups()).containsExactly("order_group", "pay_group");

        // 包含系统消费组
        ConsumerGroupListDTO allGroups = service.listConsumerGroups(true);
        assertThat(allGroups).isNotNull();
        assertThat(allGroups.getTotalCount()).isEqualTo(3);
        assertThat(allGroups.getGroups()).contains("TOOLS_CONSUMER", "order_group", "pay_group");
    }

    @Test
    @DisplayName("验证 getConsumerStatus 解析 ConsumerConnection 的客户端列表与订阅详情")
    void shouldGetConsumerStatusCorrectly() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        ConsumerConnection conn = new ConsumerConnection();
        conn.setConsumeType(ConsumeType.CONSUME_PASSIVELY);
        conn.setMessageModel(MessageModel.CLUSTERING);
        conn.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        HashSet<Connection> connSet = new HashSet<>();
        Connection c = new Connection();
        c.setClientId("192.168.1.100@12345");
        c.setClientAddr("192.168.1.100:54321");
        c.setVersion(440);
        connSet.add(c);
        conn.setConnectionSet(connSet);

        ConcurrentHashMap<String, SubscriptionData> subTable = new ConcurrentHashMap<>();
        SubscriptionData sub = new SubscriptionData();
        sub.setTopic("OrderTopic");
        sub.setSubString("TagA || TagB");
        sub.setTagsSet(Set.of("TagA", "TagB"));
        subTable.put("OrderTopic", sub);
        conn.setSubscriptionTable(subTable);

        when(mockClient.examineConsumerConnectionInfo("order_group")).thenReturn(conn);

        ConsumerConnectionDTO dto = service.getConsumerStatus("order_group");

        assertThat(dto).isNotNull();
        assertThat(dto.getConsumerGroup()).isEqualTo("order_group");
        assertThat(dto.isOnline()).isTrue();
        assertThat(dto.getConsumeType()).isEqualTo("CONSUME_PASSIVELY");
        assertThat(dto.getMessageModel()).isEqualTo("CLUSTERING");
        assertThat(dto.getClients()).hasSize(1);
        assertThat(dto.getClients().getFirst().getClientId()).isEqualTo("192.168.1.100@12345");
        assertThat(dto.getSubscriptions()).hasSize(1);
        assertThat(dto.getSubscriptions().getFirst().getTopic()).isEqualTo("OrderTopic");
        assertThat(dto.getSubscriptions().getFirst().getTagsSet()).contains("TagA", "TagB");
    }

    @Test
    @DisplayName("验证 getConsumerLag 精确计算各分片队列 Lag 与消费组总堆积量")
    void shouldGetConsumerLagCorrectly() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        ConsumeStats stats = new ConsumeStats();
        stats.setConsumeTps(55.5);
        HashMap<MessageQueue, OffsetWrapper> offsetTable = new HashMap<>();

        MessageQueue mq0 = new MessageQueue("OrderTopic", "broker-a", 0);
        OffsetWrapper ow0 = new OffsetWrapper();
        ow0.setBrokerOffset(1000L);
        ow0.setConsumerOffset(800L);
        ow0.setLastTimestamp(1728000000000L);
        offsetTable.put(mq0, ow0);

        MessageQueue mq1 = new MessageQueue("OrderTopic", "broker-a", 1);
        OffsetWrapper ow1 = new OffsetWrapper();
        ow1.setBrokerOffset(2000L);
        ow1.setConsumerOffset(1500L);
        ow1.setLastTimestamp(1728000001000L);
        offsetTable.put(mq1, ow1);

        stats.setOffsetTable(offsetTable);

        when(mockClient.examineConsumeStats("order_group", "OrderTopic")).thenReturn(stats);

        ConsumerLagDTO dto = service.getConsumerLag("order_group", "OrderTopic");

        assertThat(dto).isNotNull();
        assertThat(dto.getConsumerGroup()).isEqualTo("order_group");
        assertThat(dto.getTopic()).isEqualTo("OrderTopic");
        assertThat(dto.getConsumeTps()).isEqualTo(55.5);
        assertThat(dto.getTotalLag()).isEqualTo(700L); // (1000-800) + (2000-1500) = 200 + 500 = 700
        assertThat(dto.getQueues()).hasSize(2);
    }

    @Test
    @DisplayName("验证 getTopConsumerLag 聚合全集群消费组堆积量并按降序截取 TopN")
    void shouldGetTopConsumerLagCorrectly() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        ClusterInfo clusterInfo = new ClusterInfo();
        HashMap<String, BrokerData> brokerAddrTable = new HashMap<>();
        BrokerData bd = new BrokerData();
        bd.setBrokerName("broker-a");
        HashMap<Long, String> addrs = new HashMap<>();
        addrs.put(0L, "192.168.1.10:10911");
        bd.setBrokerAddrs(addrs);
        brokerAddrTable.put("broker-a", bd);
        clusterInfo.setBrokerAddrTable(brokerAddrTable);
        when(mockClient.examineBrokerClusterInfo()).thenReturn(clusterInfo);

        SubscriptionGroupWrapper wrapper = new SubscriptionGroupWrapper();
        ConcurrentHashMap<String, SubscriptionGroupConfig> subTable = new ConcurrentHashMap<>();
        subTable.put("group_low", new SubscriptionGroupConfig());
        subTable.put("group_high", new SubscriptionGroupConfig());
        wrapper.setSubscriptionGroupTable(subTable);
        when(mockClient.getAllSubscriptionGroup("192.168.1.10:10911", 3000L)).thenReturn(wrapper);

        ConsumeStats statsLow = new ConsumeStats();
        HashMap<MessageQueue, OffsetWrapper> offsetTableLow = new HashMap<>();
        OffsetWrapper owLow = new OffsetWrapper();
        owLow.setBrokerOffset(100L);
        owLow.setConsumerOffset(90L); // diff = 10
        offsetTableLow.put(new MessageQueue("TopicA", "broker-a", 0), owLow);
        statsLow.setOffsetTable(offsetTableLow);

        ConsumeStats statsHigh = new ConsumeStats();
        HashMap<MessageQueue, OffsetWrapper> offsetTableHigh = new HashMap<>();
        OffsetWrapper owHigh = new OffsetWrapper();
        owHigh.setBrokerOffset(10000L);
        owHigh.setConsumerOffset(1000L); // diff = 9000
        offsetTableHigh.put(new MessageQueue("TopicB", "broker-a", 0), owHigh);
        statsHigh.setOffsetTable(offsetTableHigh);

        when(mockClient.examineConsumeStats("group_low")).thenReturn(statsLow);
        when(mockClient.examineConsumeStats("group_high")).thenReturn(statsHigh);

        TopConsumerLagDTO topDto = service.getTopConsumerLag(5);

        assertThat(topDto).isNotNull();
        assertThat(topDto.getTotalEvaluatedGroups()).isEqualTo(2);
        assertThat(topDto.getTopLags()).hasSize(2);
        assertThat(topDto.getTopLags().get(0).getConsumerGroup()).isEqualTo("group_high");
        assertThat(topDto.getTopLags().get(0).getTotalLag()).isEqualTo(9000L);
        assertThat(topDto.getTopLags().get(1).getConsumerGroup()).isEqualTo("group_low");
        assertThat(topDto.getTopLags().get(1).getTotalLag()).isEqualTo(10L);
    }

    @Test
    @DisplayName("验证传入空白消费组参数时防御性抛出非法参数异常")
    void shouldThrowExceptionWhenConsumerParamsAreBlank() {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        assertThatThrownBy(() -> service.getConsumerStatus("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getConsumerLag(null, "TestTopic"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("验证根据主题与消息 ID 成功检索消息明细")
    void shouldQueryMessageByIdWithTopic() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        MessageExt msg = new MessageExt();
        msg.setTopic("OrderTopic");
        msg.setMsgId("0A00000100002A9F0000000000000001");
        msg.setTags("TagA");
        msg.setKeys("ORDER_1001");
        msg.setQueueId(1);
        msg.setQueueOffset(100L);
        msg.setBornHost(new InetSocketAddress("127.0.0.1", 10911));
        msg.setStoreHost(new InetSocketAddress("127.0.0.1", 10911));
        msg.setBornTimestamp(1700000000000L);
        msg.setStoreTimestamp(1700000001000L);
        msg.setReconsumeTimes(0);
        msg.setBody("hello rocketmq".getBytes(StandardCharsets.UTF_8));

        when(mockClient.viewMessage("OrderTopic", "0A00000100002A9F0000000000000001")).thenReturn(msg);

        MessageDetailDTO dto = service.queryMessageById("0A00000100002A9F0000000000000001", "OrderTopic");

        assertThat(dto).isNotNull();
        assertThat(dto.getMsgId()).isEqualTo("0A00000100002A9F0000000000000001");
        assertThat(dto.getTopic()).isEqualTo("OrderTopic");
        assertThat(dto.getTags()).isEqualTo("TagA");
        assertThat(dto.getKeys()).isEqualTo("ORDER_1001");
        assertThat(dto.getQueueId()).isEqualTo(1);
        assertThat(dto.getBody()).isEqualTo("hello rocketmq");
        assertThat(dto.isTruncated()).isFalse();
    }

    @Test
    @DisplayName("验证仅根据消息 ID (不带 Topic) 检索消息明细，自动扫描业务主题")
    void shouldQueryMessageByIdWithoutTopic() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        MessageExt msg = new MessageExt();
        msg.setTopic("DefaultTopic");
        msg.setMsgId("0A00000100002A9F0000000000000002");
        msg.setQueueId(0);
        msg.setQueueOffset(50L);
        msg.setBornTimestamp(1700000000000L);
        msg.setStoreTimestamp(1700000001000L);
        msg.setBody("test payload".getBytes(StandardCharsets.UTF_8));

        org.apache.rocketmq.remoting.protocol.body.TopicList topicList = new org.apache.rocketmq.remoting.protocol.body.TopicList();
        topicList.setTopicList(new java.util.HashSet<>(List.of("DefaultTopic")));
        when(mockClient.fetchAllTopicList()).thenReturn(topicList);
        when(mockClient.viewMessage("DefaultTopic", "0A00000100002A9F0000000000000002")).thenReturn(msg);

        MessageDetailDTO dto = service.queryMessageById("0A00000100002A9F0000000000000002", null);

        assertThat(dto).isNotNull();
        assertThat(dto.getMsgId()).isEqualTo("0A00000100002A9F0000000000000002");
        assertThat(dto.getTopic()).isEqualTo("DefaultTopic");
        assertThat(dto.getBody()).isEqualTo("test payload");
    }

    @Test
    @DisplayName("验证根据 Key 检索消息列表成功返回")
    void shouldQueryMessageByKeySuccessfully() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        MessageExt msg1 = new MessageExt();
        msg1.setTopic("OrderTopic");
        msg1.setMsgId("MSG_K_1");
        msg1.setKeys("ORDER_K");
        msg1.setQueueId(0);
        msg1.setQueueOffset(10L);
        msg1.setBornTimestamp(1700000000000L);
        msg1.setStoreTimestamp(1700000001000L);
        msg1.setBody("order-1".getBytes(StandardCharsets.UTF_8));

        QueryResult queryResult = new QueryResult(1700000000000L, List.of(msg1));
        when(mockClient.queryMessage(anyString(), anyString(), anyInt(), anyLong(), anyLong()))
                .thenReturn(queryResult);

        MessageListDTO listDto = service.queryMessageByKey("OrderTopic", "ORDER_K", 1000L, 2000L, 10);

        assertThat(listDto).isNotNull();
        assertThat(listDto.getTotalCount()).isEqualTo(1);
        assertThat(listDto.getMessages().get(0).getKeys()).isEqualTo("ORDER_K");
    }

    @Test
    @DisplayName("验证查询死信队列消息列表成功返回")
    void shouldQueryDlqMessagesSuccessfully() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        MessageExt dlqMsg = new MessageExt();
        dlqMsg.setTopic("%DLQ%TestGroup");
        dlqMsg.setMsgId("DLQ_001");
        dlqMsg.setKeys("FAIL_KEY");
        dlqMsg.setQueueId(0);
        dlqMsg.setQueueOffset(5L);
        dlqMsg.setBornTimestamp(1700000000000L);
        dlqMsg.setStoreTimestamp(1700000001000L);
        dlqMsg.setReconsumeTimes(16);
        dlqMsg.setBody("dead message payload".getBytes(StandardCharsets.UTF_8));

        QueryResult queryResult = new QueryResult(1700000000000L, List.of(dlqMsg));
        when(mockClient.queryMessage(anyString(), anyString(), anyInt(), anyLong(), anyLong()))
                .thenReturn(queryResult);

        DlqMessageListDTO dlqDto = service.queryDlqMessages("TestGroup", null, null, null);

        assertThat(dlqDto).isNotNull();
        assertThat(dlqDto.getConsumerGroup()).isEqualTo("TestGroup");
        assertThat(dlqDto.getDlqTopic()).isEqualTo("%DLQ%TestGroup");
        assertThat(dlqDto.getTotalCount()).isEqualTo(1);
        assertThat(dlqDto.getMessages().get(0).getReconsumeTimes()).isEqualTo(16);
    }

    @Test
    @DisplayName("验证查询消息生命周期轨迹，在无轨迹主题时降级返回发送与存储节点")
    void shouldQueryMessageTraceWithFallback() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        MessageExt msg = new MessageExt();
        msg.setTopic("OrderTopic");
        msg.setMsgId("MSG_TRACE_001");
        msg.setKeys("ORDER_T");
        msg.setBornHost(new InetSocketAddress("192.168.1.100", 50000));
        msg.setStoreHost(new InetSocketAddress("192.168.1.10", 10911));
        msg.setBornTimestamp(1700000000000L);
        msg.setStoreTimestamp(1700000001000L);
        msg.setBody("trace payload".getBytes(StandardCharsets.UTF_8));

        when(mockClient.viewMessage("OrderTopic", "MSG_TRACE_001")).thenReturn(msg);
        // 系统轨迹主题抛出异常或查询无结果，模拟无 RMQ_SYS_TRACE_TOPIC
        when(mockClient.queryMessage(org.mockito.ArgumentMatchers.eq("RMQ_SYS_TRACE_TOPIC"), anyString(), anyInt(), anyLong(), anyLong()))
                .thenThrow(new RuntimeException("Trace topic not found"));

        MessageTraceDTO traceDto = service.queryMessageTrace("MSG_TRACE_001", "OrderTopic");

        assertThat(traceDto).isNotNull();
        assertThat(traceDto.getMsgId()).isEqualTo("MSG_TRACE_001");
        assertThat(traceDto.getTopic()).isEqualTo("OrderTopic");
        // 应该降级包含 Pub 与 Broker 两个节点
        assertThat(traceDto.getTraceNodes()).hasSize(2);
        assertThat(traceDto.getTraceNodes().get(0).getNodeType()).isEqualTo("Pub");
        assertThat(traceDto.getTraceNodes().get(1).getNodeType()).isEqualTo("Broker");
    }

    @Test
    @DisplayName("验证消息查询入参空白时抛出非法参数异常")
    void shouldThrowExceptionWhenMessageParamsAreBlank() {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        assertThatThrownBy(() -> service.queryMessageById("  ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.queryMessageByKey(" ", "key", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.queryMessageByKey("Topic", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.queryDlqMessages("  ", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.queryMessageTrace(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("验证声明式创建主题向 Master Broker 发送 TopicConfig 并返回结果")
    void shouldCreateTopicSuccessfully() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        ClusterInfo clusterInfo = new ClusterInfo();
        HashMap<String, BrokerData> brokerAddrTable = new HashMap<>();
        BrokerData brokerData = new BrokerData();
        brokerData.setBrokerName("broker-a");
        brokerData.setBrokerAddrs(new HashMap<>(Map.of(0L, "192.168.1.10:10911")));
        brokerAddrTable.put("broker-a", brokerData);
        clusterInfo.setBrokerAddrTable(brokerAddrTable);

        when(mockClient.examineBrokerClusterInfo()).thenReturn(clusterInfo);

        TopicOperationResultDTO result = service.createTopic("NewOrderTopic", 16, 16, 6);

        assertThat(result).isNotNull();
        assertThat(result.getOperation()).isEqualTo("CREATE_TOPIC");
        assertThat(result.getTopic()).isEqualTo("NewOrderTopic");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(mockClient).createAndUpdateTopicConfig(anyString(), any(TopicConfig.class));
    }

    @Test
    @DisplayName("验证删除主题时向 Broker 与 NameServer 发出指令并返回结果")
    void shouldDeleteTopicSuccessfully() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        properties.setNamesrvAddr("127.0.0.1:9876;127.0.0.2:9876");
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        ClusterInfo clusterInfo = new ClusterInfo();
        HashMap<String, BrokerData> brokerAddrTable = new HashMap<>();
        BrokerData brokerData = new BrokerData();
        brokerData.setBrokerName("broker-a");
        brokerData.setBrokerAddrs(new HashMap<>(Map.of(0L, "192.168.1.10:10911")));
        brokerAddrTable.put("broker-a", brokerData);
        clusterInfo.setBrokerAddrTable(brokerAddrTable);

        when(mockClient.examineBrokerClusterInfo()).thenReturn(clusterInfo);

        TopicOperationResultDTO result = service.deleteTopic("OldTopic");

        assertThat(result).isNotNull();
        assertThat(result.getOperation()).isEqualTo("DELETE_TOPIC");
        assertThat(result.getTopic()).isEqualTo("OldTopic");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(mockClient).deleteTopicInBroker(any(), anyString());
        verify(mockClient).deleteTopicInNameServer(any(), anyString());
    }

    @Test
    @DisplayName("验证按时间戳重置位点与跳过积压至最大位点模式成功调用底层 API")
    void shouldResetOffsetSuccessfully() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        // 1. 时间戳模式
        ResetOffsetResultDTO tsResult = service.resetOffset("order_group", "OrderTopic", 1700000000000L, false);
        assertThat(tsResult).isNotNull();
        assertThat(tsResult.getResetMode()).isEqualTo("TIMESTAMP");
        assertThat(tsResult.getTargetTimestamp()).isEqualTo(1700000000000L);
        verify(mockClient).resetOffsetByTimestamp("OrderTopic", "order_group", 1700000000000L, true);

        // 2. 最大位点跳过积压模式
        ResetOffsetResultDTO maxResult = service.resetOffset("order_group", "OrderTopic", null, true);
        assertThat(maxResult).isNotNull();
        assertThat(maxResult.getResetMode()).isEqualTo("MAX_OFFSET");
        assertThat(maxResult.getTargetTimestamp()).isNotNull();
        verify(mockClient, times(2)).resetOffsetByTimestamp(anyString(), anyString(), anyLong(), any(Boolean.class));
    }

    @Test
    @DisplayName("验证重新投递死信队列消息成功解析并调用 MessagingClientService 发送回原主题")
    void shouldResendDlqMessageSuccessfully() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        MessagingClientService mockMessaging = mock(MessagingClientService.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient, mockMessaging);

        MessageExt dlqMsg = new MessageExt();
        dlqMsg.setMsgId("DLQ_MSG_01");
        dlqMsg.setBody("retry payload".getBytes(StandardCharsets.UTF_8));
        dlqMsg.setTags("TagA");
        dlqMsg.setKeys("KEY123");
        MessageAccessor.putProperty(dlqMsg, MessageConst.PROPERTY_REAL_TOPIC, "OriginalOrderTopic");

        when(mockClient.viewMessage("%DLQ%order_group", "DLQ_MSG_01")).thenReturn(dlqMsg);
        SendMessageResultDTO sendResult = new SendMessageResultDTO("NEW_MSG_02", "OriginalOrderTopic", "SUCCESS");
        when(mockMessaging.sendMessage("OriginalOrderTopic", "retry payload", "TagA", "KEY123", null, null))
                .thenReturn(sendResult);

        ResendDlqResultDTO result = service.resendDlqMessage("order_group", "DLQ_MSG_01", null);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getConsumerGroup()).isEqualTo("order_group");
        assertThat(result.getMsgId()).isEqualTo("DLQ_MSG_01");
        assertThat(result.getTargetTopic()).isEqualTo("OriginalOrderTopic");
        assertThat(result.getResendMessageId()).isEqualTo("NEW_MSG_02");
    }

    @Test
    @DisplayName("验证控制面破坏性参数为空或试图删除系统主题时抛出 IllegalArgumentException")
    void shouldThrowExceptionWhenDestructiveAdminParamsAreInvalid() {
        RocketmqProperties properties = new RocketmqProperties();
        DefaultMQAdminExt mockClient = mock(DefaultMQAdminExt.class);
        TestableAdminClientService service = new TestableAdminClientService(properties, mockClient);

        // 创建主题参数为空
        assertThatThrownBy(() -> service.createTopic("   ", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        // 删除主题参数为空或删除系统主题
        assertThatThrownBy(() -> service.deleteTopic(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deleteTopic("TBW102"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete internal system topic");
        assertThatThrownBy(() -> service.deleteTopic("%SYS%BENCHMARK"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete internal system topic");

        // 重置位点参数防御
        assertThatThrownBy(() -> service.resetOffset("  ", "Topic", 1000L, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.resetOffset("group", "  ", 1000L, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.resetOffset("group", "Topic", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'timestamp' must be greater than 0");

        // 死信重投参数防御
        assertThatThrownBy(() -> service.resendDlqMessage(" ", "MSG_01", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.resendDlqMessage("group", " ", null))
                .isInstanceOf(IllegalArgumentException.class);

        // 死信重投组件缺失防御
        assertThatThrownBy(() -> service.resendDlqMessage("group", "MSG_01", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MessagingClientService is unavailable");
    }

    private static class TestableAdminClientService extends DefaultAdminClientService {
        private final DefaultMQAdminExt mockClient;

        public TestableAdminClientService(RocketmqProperties properties, DefaultMQAdminExt mockClient) {
            this(properties, mockClient, null);
        }

        public TestableAdminClientService(RocketmqProperties properties, DefaultMQAdminExt mockClient, MessagingClientService messagingClientService) {
            super(properties, messagingClientService);
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
