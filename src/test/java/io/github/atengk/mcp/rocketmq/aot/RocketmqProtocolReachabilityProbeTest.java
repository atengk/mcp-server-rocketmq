package io.github.atengk.mcp.rocketmq.aot;

import com.alibaba.fastjson.parser.ParserConfig;
import io.github.atengk.mcp.rocketmq.config.aot.RocketmqAotEnvironmentPostProcessor;
import io.github.atengk.mcp.rocketmq.config.aot.RocketmqRuntimeHintsRegistrar;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper;
import org.apache.rocketmq.remoting.protocol.admin.TopicOffset;
import org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.body.Connection;
import org.apache.rocketmq.remoting.protocol.body.ConsumerConnection;
import org.apache.rocketmq.remoting.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.remoting.protocol.body.TopicList;
import org.apache.rocketmq.remoting.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetConsumerListByGroupRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.namesrv.GetRouteInfoRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.QueryConsumerOffsetRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ResetOffsetRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ViewMessageRequestHeader;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.apache.rocketmq.remoting.protocol.route.QueueData;
import org.apache.rocketmq.remoting.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.protocol.subscription.SubscriptionGroupConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RocketMQ 协议 AOT 运行时元数据可达性与全协议序列化探针测试（接缝 1）。
 * 脱离网络 Mock，在内存中直接验证 Fastjson ASM 禁用状态与 Remoting 协议报文编解码完整性。
 *
 * @author Ateng
 * @since 2026-10-05
 */
@DisplayName("RocketMQ 协议 AOT 运行时探针测试 (接缝 1)")
class RocketmqProtocolReachabilityProbeTest {

    @BeforeAll
    static void initEnvironment() {
        // 触发 AOT 启动环境处理器
        RocketmqAotEnvironmentPostProcessor processor = new RocketmqAotEnvironmentPostProcessor();
        processor.postProcessEnvironment(new MockEnvironment(), new SpringApplication());
    }

    @Test
    @DisplayName("验证 Fastjson 1.x ASM 动态字节码生成已被全局禁用")
    void shouldDisableFastjsonAsmGlobally() {
        assertThat(ParserConfig.getGlobalInstance().isAsmEnable())
                .as("Fastjson ASM 动态生成必须全局关闭，防止 GraalVM Native 抛出 Unsafe.defineClass 异常")
                .isFalse();
    }

    @Test
    @DisplayName("验证 ClusterInfo 报文脱离 ASM 在内存中成功序列化与反序列化")
    void shouldSerializeAndDeserializeClusterInfo() {
        ClusterInfo clusterInfo = new ClusterInfo();
        HashMap<String, Set<String>> clusterTable = new HashMap<>();
        clusterTable.put("DefaultCluster", Set.of("broker-a"));
        clusterInfo.setClusterAddrTable(clusterTable);

        HashMap<String, BrokerData> brokerAddrTable = new HashMap<>();
        BrokerData brokerData = new BrokerData();
        brokerData.setCluster("DefaultCluster");
        brokerData.setBrokerName("broker-a");
        HashMap<Long, String> addrs = new HashMap<>();
        addrs.put(0L, "127.0.0.1:10911");
        brokerData.setBrokerAddrs(addrs);
        brokerAddrTable.put("broker-a", brokerData);
        clusterInfo.setBrokerAddrTable(brokerAddrTable);

        byte[] jsonBytes = RemotingSerializable.encode(clusterInfo);
        assertThat(jsonBytes).isNotEmpty();

        ClusterInfo decoded = RemotingSerializable.decode(jsonBytes, ClusterInfo.class);
        assertThat(decoded).isNotNull();
        assertThat(decoded.getClusterAddrTable()).containsKey("DefaultCluster");
        assertThat(decoded.getBrokerAddrTable()).containsKey("broker-a");
    }

    @Test
    @DisplayName("验证 TopicRouteData 报文无损序列化与反序列化")
    void shouldSerializeAndDeserializeTopicRouteData() {
        TopicRouteData routeData = new TopicRouteData();
        QueueData queueData = new QueueData();
        queueData.setBrokerName("broker-a");
        queueData.setReadQueueNums(8);
        queueData.setWriteQueueNums(8);
        queueData.setPerm(6);
        routeData.setQueueDatas(List.of(queueData));

        BrokerData brokerData = new BrokerData();
        brokerData.setCluster("DefaultCluster");
        brokerData.setBrokerName("broker-a");
        HashMap<Long, String> addrs = new HashMap<>();
        addrs.put(0L, "127.0.0.1:10911");
        brokerData.setBrokerAddrs(addrs);
        routeData.setBrokerDatas(List.of(brokerData));

        byte[] jsonBytes = RemotingSerializable.encode(routeData);
        TopicRouteData decoded = RemotingSerializable.decode(jsonBytes, TopicRouteData.class);

        assertThat(decoded).isNotNull();
        assertThat(decoded.getQueueDatas()).hasSize(1);
        assertThat(decoded.getBrokerDatas()).hasSize(1);
        assertThat(decoded.getBrokerDatas().get(0).getBrokerName()).isEqualTo("broker-a");
    }

    @Test
    @DisplayName("验证 ConsumeStats 与 TopicStatsTable 报文序列化反序列化")
    void shouldSerializeAndDeserializeConsumeStats() {
        ConsumeStats consumeStats = new ConsumeStats();
        HashMap<MessageQueue, OffsetWrapper> offsetTable = new HashMap<>();
        MessageQueue mq = new MessageQueue("test-topic", "broker-a", 0);
        OffsetWrapper wrapper = new OffsetWrapper();
        wrapper.setBrokerOffset(1000L);
        wrapper.setConsumerOffset(950L);
        wrapper.setLastTimestamp(System.currentTimeMillis());
        offsetTable.put(mq, wrapper);
        consumeStats.setOffsetTable(offsetTable);

        byte[] jsonBytes = RemotingSerializable.encode(consumeStats);
        ConsumeStats decoded = RemotingSerializable.decode(jsonBytes, ConsumeStats.class);

        assertThat(decoded).isNotNull();
        assertThat(decoded.getOffsetTable()).isNotEmpty();

        TopicStatsTable statsTable = new TopicStatsTable();
        HashMap<MessageQueue, TopicOffset> topicOffsetTable = new HashMap<>();
        TopicOffset topicOffset = new TopicOffset();
        topicOffset.setMinOffset(0L);
        topicOffset.setMaxOffset(1000L);
        topicOffset.setLastUpdateTimestamp(System.currentTimeMillis());
        topicOffsetTable.put(mq, topicOffset);
        statsTable.setOffsetTable(topicOffsetTable);

        byte[] tableBytes = RemotingSerializable.encode(statsTable);
        TopicStatsTable decodedTable = RemotingSerializable.decode(tableBytes, TopicStatsTable.class);
        assertThat(decodedTable).isNotNull();
        assertThat(decodedTable.getOffsetTable()).isNotEmpty();
    }

    @Test
    @DisplayName("验证 ConsumerConnection 与 SubscriptionGroupWrapper 报文序列化反序列化")
    void shouldSerializeAndDeserializeConsumerConnection() {
        ConsumerConnection connection = new ConsumerConnection();
        HashSet<Connection> connectionSet = new HashSet<>();
        Connection conn = new Connection();
        conn.setClientId("127.0.0.1@12345");
        conn.setClientAddr("127.0.0.1:50000");
        conn.setLanguage(org.apache.rocketmq.remoting.protocol.LanguageCode.JAVA);
        conn.setVersion(400);
        connectionSet.add(conn);
        connection.setConnectionSet(connectionSet);

        byte[] connBytes = RemotingSerializable.encode(connection);
        ConsumerConnection decodedConn = RemotingSerializable.decode(connBytes, ConsumerConnection.class);
        assertThat(decodedConn).isNotNull();
        assertThat(decodedConn.getConnectionSet()).hasSize(1);

        SubscriptionGroupWrapper groupWrapper = new SubscriptionGroupWrapper();
        SubscriptionGroupConfig config = new SubscriptionGroupConfig();
        config.setGroupName("test-group");
        config.setConsumeEnable(true);
        groupWrapper.getSubscriptionGroupTable().put("test-group", config);

        byte[] groupBytes = RemotingSerializable.encode(groupWrapper);
        SubscriptionGroupWrapper decodedGroup = RemotingSerializable.decode(groupBytes, SubscriptionGroupWrapper.class);
        assertThat(decodedGroup).isNotNull();
        assertThat(decodedGroup.getSubscriptionGroupTable()).containsKey("test-group");
    }

    @Test
    @DisplayName("验证核心 CommandCustomHeader 反射编解码无异常")
    void shouldEncodeAndDecodeCommandCustomHeaders() throws Exception {
        CreateTopicRequestHeader createTopic = new CreateTopicRequestHeader();
        createTopic.setTopic("test-topic");
        createTopic.setTopicFilterType("SINGLE_TAG");
        createTopic.setReadQueueNums(8);
        createTopic.setWriteQueueNums(8);
        assertHeaderReflectiveSafety(createTopic, CreateTopicRequestHeader.class);

        DeleteTopicRequestHeader deleteTopic = new DeleteTopicRequestHeader();
        deleteTopic.setTopic("test-topic");
        assertHeaderReflectiveSafety(deleteTopic, DeleteTopicRequestHeader.class);

        GetRouteInfoRequestHeader routeInfo = new GetRouteInfoRequestHeader();
        routeInfo.setTopic("test-topic");
        assertHeaderReflectiveSafety(routeInfo, GetRouteInfoRequestHeader.class);

        ResetOffsetRequestHeader resetOffset = new ResetOffsetRequestHeader();
        resetOffset.setTopic("test-topic");
        resetOffset.setGroup("test-group");
        resetOffset.setTimestamp(1000L);
        assertHeaderReflectiveSafety(resetOffset, ResetOffsetRequestHeader.class);

        QueryConsumerOffsetRequestHeader queryOffset = new QueryConsumerOffsetRequestHeader();
        queryOffset.setTopic("test-topic");
        queryOffset.setConsumerGroup("test-group");
        queryOffset.setQueueId(0);
        assertHeaderReflectiveSafety(queryOffset, QueryConsumerOffsetRequestHeader.class);

        GetConsumerListByGroupRequestHeader getConsumerList = new GetConsumerListByGroupRequestHeader();
        getConsumerList.setConsumerGroup("test-group");
        assertHeaderReflectiveSafety(getConsumerList, GetConsumerListByGroupRequestHeader.class);

        ViewMessageRequestHeader viewMessage = new ViewMessageRequestHeader();
        viewMessage.setOffset(100L);
        assertHeaderReflectiveSafety(viewMessage, ViewMessageRequestHeader.class);
    }

    private <T extends CommandCustomHeader> void assertHeaderReflectiveSafety(T header, Class<T> clazz) throws Exception {
        header.checkFields();
        RemotingCommand command = RemotingCommand.createRequestCommand(10, header);
        command.makeCustomHeaderToNet();
        assertThat(command.getExtFields()).isNotNull();

        T decoded = command.decodeCommandCustomHeader(clazz);
        assertThat(decoded).isNotNull();
    }

    @Test
    @DisplayName("验证 RocketmqRuntimeHintsRegistrar 成功注册核心协议反射白名单")
    void shouldRegisterRuntimeHintsSuccessfully() {
        RuntimeHints hints = new RuntimeHints();
        RocketmqRuntimeHintsRegistrar registrar = new RocketmqRuntimeHintsRegistrar();
        registrar.registerHints(hints, getClass().getClassLoader());

        // 校验关键类是否已纳入反射注册
        assertThat(hints.reflection().typeHints()).anyMatch(hint ->
                hint.getType().getName().equals(ClusterInfo.class.getName())
        );
        assertThat(hints.reflection().typeHints()).anyMatch(hint ->
                hint.getType().getName().equals(TopicRouteData.class.getName())
        );
        assertThat(hints.reflection().typeHints()).anyMatch(hint ->
                hint.getType().getName().equals(ConsumeStats.class.getName())
        );
        assertThat(hints.reflection().typeHints()).anyMatch(hint ->
                hint.getType().getName().equals(CreateTopicRequestHeader.class.getName())
        );
        assertThat(hints.reflection().typeHints()).anyMatch(hint ->
                hint.getType().getName().equals(MessageExt.class.getName())
        );
    }
}
