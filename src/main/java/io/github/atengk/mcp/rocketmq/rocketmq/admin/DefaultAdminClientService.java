package io.github.atengk.mcp.rocketmq.rocketmq.admin;

import io.github.atengk.mcp.rocketmq.config.RocketmqProperties;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.BrokerStatsDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.BrokerSummaryDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerClientDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerConnectionDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerGroupListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerLagDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerLagSummaryDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerQueueLagDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.DlqMessageListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.MessageDetailDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.MessageListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.MessageTraceDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.MessageTraceNodeDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.QueueDataDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ResendDlqResultDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ResetOffsetResultDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.SubscriptionDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopConsumerLagDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicOperationResultDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicOverviewDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicQueueOffsetDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicRouteDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicStatusDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.messaging.MessagingClientService;
import io.github.atengk.mcp.rocketmq.rocketmq.messaging.dto.SendMessageResultDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.util.ConsumerGroupFilterUtils;
import io.github.atengk.mcp.rocketmq.rocketmq.util.MessageBodyGuard;
import io.github.atengk.mcp.rocketmq.rocketmq.util.TopicFilterUtils;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.trace.TraceView;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper;
import org.apache.rocketmq.remoting.protocol.admin.TopicOffset;
import org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.body.Connection;
import org.apache.rocketmq.remoting.protocol.body.ConsumerConnection;
import org.apache.rocketmq.remoting.protocol.body.KVTable;
import org.apache.rocketmq.remoting.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.remoting.protocol.body.TopicList;
import org.apache.rocketmq.remoting.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.apache.rocketmq.remoting.protocol.route.QueueData;
import org.apache.rocketmq.remoting.protocol.route.TopicRouteData;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RocketMQ 运维管理客户端服务默认实现。
 * 负责管理 DefaultMQAdminExt 单例生命周期、自愈重连并提供对集群底层元数据、指标与主题感知的安全转换访问。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Service
public class DefaultAdminClientService implements AdminClientService, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminClientService.class);

    private static final long MASTER_BROKER_ID = 0L;
    private static final int DEFAULT_QUEUE_NUMS = 8;
    private static final int DEFAULT_TOPIC_PERM = 6;
    private static final long RECONNECT_COOLDOWN_MS = 5000L;

    private final RocketmqProperties properties;
    private final MessagingClientService messagingClientService;
    private DefaultMQAdminExt mqAdminExt;
    private volatile boolean started = false;
    private volatile long lastReconnectTimestamp = 0L;
    public DefaultAdminClientService(RocketmqProperties properties) {
        this(properties, null);
    }

    @Autowired
    public DefaultAdminClientService(RocketmqProperties properties, @Lazy MessagingClientService messagingClientService) {
        this.properties = properties;
        this.messagingClientService = messagingClientService;
    }

    @Override
    public void afterPropertiesSet() {
        initAdminClient();
    }

    private synchronized void initAdminClient() {
        if (started) {
            return;
        }
        try {
            this.mqAdminExt = createMQAdminExt();
            this.mqAdminExt.setNamesrvAddr(properties.getNamesrvAddr());
            this.mqAdminExt.setInstanceName("mcp-admin-" + System.currentTimeMillis());
            this.mqAdminExt.start();
            this.started = true;
            log.info("DefaultMQAdminExt successfully initialized with NameServer: {}", properties.getNamesrvAddr());
        } catch (Exception e) {
            log.warn("Failed to start DefaultMQAdminExt eagerly. Will retry on first operational call. Reason: {}", e.getMessage());
        }
    }

    /**
     * 工厂方法创建 DefaultMQAdminExt 实例，便于单元测试子类插桩。
     *
     * @return 新建的 DefaultMQAdminExt 客户端
     */
    protected DefaultMQAdminExt createMQAdminExt() {
        if (properties != null && properties.getAccessKey() != null && properties.getSecretKey() != null
                && !properties.getAccessKey().isBlank() && !properties.getSecretKey().isBlank()) {
            log.info("Configuring DefaultMQAdminExt with ACL RPCHook using accessKey: {}", properties.getAccessKey().trim());
            return new DefaultMQAdminExt(new AclClientRPCHook(
                    new SessionCredentials(properties.getAccessKey().trim(), properties.getSecretKey().trim())));
        }
        return new DefaultMQAdminExt();
    }

    public synchronized void reconnect() {
        long now = System.currentTimeMillis();
        if (now - lastReconnectTimestamp < RECONNECT_COOLDOWN_MS) {
            log.info("Skipping frequent reconnect request due to {}ms cooldown window.", RECONNECT_COOLDOWN_MS);
            return;
        }
        lastReconnectTimestamp = now;
        if (mqAdminExt != null) {
            try {
                mqAdminExt.shutdown();
            } catch (Exception e) {
                log.debug("Swallowed exception during client shutdown before reconnect: {}", e.getMessage());
            }
            mqAdminExt = null;
        }
        started = false;
        initAdminClient();
    }

    protected DefaultMQAdminExt ensureStarted() throws Exception {
        if (!started || mqAdminExt == null) {
            synchronized (this) {
                if (!started || mqAdminExt == null) {
                    initAdminClient();
                }
            }
        }
        if (!started || mqAdminExt == null) {
            throw new IllegalStateException("DefaultMQAdminExt is not available. Check NameServer connectivity: " + properties.getNamesrvAddr());
        }
        return mqAdminExt;
    }

    @Override
    public ClusterInfo examineBrokerClusterInfo() throws Exception {
        try {
            DefaultMQAdminExt client = ensureStarted();
            return client.examineBrokerClusterInfo();
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
            log.warn("Connection lost when examining cluster info. Triggering self-healing reconnect: {}", e.getMessage());
            reconnect();
            DefaultMQAdminExt client = ensureStarted();
            return client.examineBrokerClusterInfo();
        }
    }

    @Override
    public ClusterInfoDTO getClusterInfo() throws Exception {
        ClusterInfo clusterInfo = examineBrokerClusterInfo();
        ClusterInfoDTO dto = new ClusterInfoDTO();
        if (clusterInfo == null) {
            return dto;
        }

        // 1. 映射 Cluster 与 BrokerNames 列表
        Map<String, Set<String>> clusterAddrTable = clusterInfo.getClusterAddrTable();
        Map<String, List<String>> clusterTable = new HashMap<>();
        if (clusterAddrTable != null) {
            for (Map.Entry<String, Set<String>> entry : clusterAddrTable.entrySet()) {
                clusterTable.put(entry.getKey(), new ArrayList<>(entry.getValue() != null ? entry.getValue() : Collections.emptySet()));
            }
        }
        dto.setClusterTable(clusterTable);

        // 2. 映射 Broker 节点详情
        Map<String, BrokerData> brokerAddrTable = clusterInfo.getBrokerAddrTable();
        List<BrokerSummaryDTO> brokerList = new ArrayList<>();
        int masterCount = 0;
        int slaveCount = 0;

        if (brokerAddrTable != null) {
            for (BrokerData brokerData : brokerAddrTable.values()) {
                if (brokerData == null || brokerData.getBrokerAddrs() == null) {
                    continue;
                }
                for (Map.Entry<Long, String> addrEntry : brokerData.getBrokerAddrs().entrySet()) {
                    Long brokerId = addrEntry.getKey();
                    String addr = addrEntry.getValue();
                    String role = (brokerId != null && brokerId == 0L) ? "MASTER" : "SLAVE";
                    if ("MASTER".equals(role)) {
                        masterCount++;
                    } else {
                        slaveCount++;
                    }
                    brokerList.add(new BrokerSummaryDTO(
                            brokerData.getBrokerName(),
                            brokerData.getCluster(),
                            brokerId,
                            role,
                            addr,
                            null
                    ));
                }
            }
        }

        dto.setBrokers(brokerList);
        dto.setTotalBrokers(brokerList.size());
        dto.setTotalMasters(masterCount);
        dto.setTotalSlaves(slaveCount);

        return dto;
    }

    @Override
    public BrokerStatsDTO getBrokerStats(String brokerAddr) throws Exception {
        if (brokerAddr == null || brokerAddr.isBlank()) {
            throw new IllegalArgumentException("brokerAddr 不能为空");
        }

        String targetAddr = resolveBrokerAddress(brokerAddr.trim());
        KVTable kvTable;
        try {
            DefaultMQAdminExt client = ensureStarted();
            kvTable = client.fetchBrokerRuntimeStats(targetAddr);
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
            log.warn("Connection lost when fetching broker stats from {}. Triggering self-healing reconnect: {}", targetAddr, e.getMessage());
            reconnect();
            DefaultMQAdminExt client = ensureStarted();
            kvTable = client.fetchBrokerRuntimeStats(targetAddr);
        }

        String brokerName = targetAddr.equals(brokerAddr.trim()) ? null : brokerAddr.trim();
        BrokerStatsDTO dto = new BrokerStatsDTO(targetAddr, brokerName);
        if (kvTable != null && kvTable.getTable() != null) {
            Map<String, String> table = kvTable.getTable();
            dto.setTable(new HashMap<>(table));
            dto.setPutTps(table.getOrDefault("putTps", "0.0"));
            dto.setGetTransferredTps(table.getOrDefault("getTransferredTps", "0.0"));
            dto.setInTotalTps(table.getOrDefault("inTotalTps", "0.0"));
            dto.setOutTotalTps(table.getOrDefault("outTotalTps", "0.0"));
            dto.setCommitLogDiskRatio(table.getOrDefault("commitLogDiskRatio", "0.0"));
            dto.setRuntime(table.getOrDefault("runtime", "unknown"));

            String bootTimeStr = table.get("bootTimestamp");
            if (bootTimeStr != null && !bootTimeStr.isBlank()) {
                try {
                    dto.setBootTimestamp(Long.parseLong(bootTimeStr));
                } catch (NumberFormatException e) {
                    log.debug("Failed to parse bootTimestamp: {}", bootTimeStr);
                }
            }
        }
        return dto;
    }

    @Override
    public TopicListDTO listTopics(boolean includeSystem) throws Exception {
        TopicList topicList;
        try {
            DefaultMQAdminExt client = ensureStarted();
            topicList = client.fetchAllTopicList();
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
            log.warn("Connection lost when fetching topic list. Triggering self-healing reconnect: {}", e.getMessage());
            reconnect();
            DefaultMQAdminExt client = ensureStarted();
            topicList = client.fetchAllTopicList();
        }

        if (topicList == null || topicList.getTopicList() == null || topicList.getTopicList().isEmpty()) {
            return new TopicListDTO(Collections.emptyList());
        }

        Set<String> rawTopics = topicList.getTopicList();
        List<String> resultList;
        if (includeSystem) {
            resultList = rawTopics.stream()
                    .filter(t -> t != null && !t.isBlank())
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            resultList = TopicFilterUtils.filterBusinessTopics(rawTopics);
        }

        return new TopicListDTO(resultList);
    }

    @Override
    public TopicRouteDTO getTopicRoute(String topic) throws Exception {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic 不能为空");
        }

        TopicRouteData routeData;
        try {
            DefaultMQAdminExt client = ensureStarted();
            routeData = client.examineTopicRouteInfo(topic.trim());
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
            log.warn("Connection lost when examining topic route for {}. Triggering self-healing reconnect: {}", topic, e.getMessage());
            reconnect();
            DefaultMQAdminExt client = ensureStarted();
            routeData = client.examineTopicRouteInfo(topic.trim());
        }

        TopicRouteDTO dto = new TopicRouteDTO(topic.trim());
        if (routeData == null) {
            return dto;
        }

        // 1. 映射 QueueData
        if (routeData.getQueueDatas() != null) {
            List<QueueDataDTO> queueList = new ArrayList<>();
            for (QueueData qd : routeData.getQueueDatas()) {
                if (qd != null) {
                    queueList.add(new QueueDataDTO(
                            qd.getBrokerName(),
                            qd.getReadQueueNums(),
                            qd.getWriteQueueNums(),
                            qd.getPerm()
                    ));
                }
            }
            dto.setQueueDatas(queueList);
        }

        // 2. 映射 BrokerData
        if (routeData.getBrokerDatas() != null) {
            List<BrokerSummaryDTO> brokerList = new ArrayList<>();
            for (BrokerData bd : routeData.getBrokerDatas()) {
                if (bd != null && bd.getBrokerAddrs() != null) {
                    for (Map.Entry<Long, String> entry : bd.getBrokerAddrs().entrySet()) {
                        Long brokerId = entry.getKey();
                        String role = (brokerId != null && brokerId == 0L) ? "MASTER" : "SLAVE";
                        brokerList.add(new BrokerSummaryDTO(
                                bd.getBrokerName(),
                                bd.getCluster(),
                                brokerId,
                                role,
                                entry.getValue(),
                                null
                        ));
                    }
                }
            }
            dto.setBrokerDatas(brokerList);
        }

        return dto;
    }

    @Override
    public TopicStatusDTO getTopicStatus(String topic) throws Exception {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic 不能为空");
        }

        TopicStatsTable statsTable;
        try {
            DefaultMQAdminExt client = ensureStarted();
            statsTable = client.examineTopicStats(topic.trim());
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
            log.warn("Connection lost when examining topic stats for {}. Triggering self-healing reconnect: {}", topic, e.getMessage());
            reconnect();
            DefaultMQAdminExt client = ensureStarted();
            statsTable = client.examineTopicStats(topic.trim());
        }

        TopicStatusDTO dto = new TopicStatusDTO(topic.trim());
        if (statsTable == null || statsTable.getOffsetTable() == null || statsTable.getOffsetTable().isEmpty()) {
            return dto;
        }

        List<TopicQueueOffsetDTO> queueOffsets = new ArrayList<>();
        long totalMessages = 0;
        long globalMin = Long.MAX_VALUE;
        long globalMax = 0;

        for (Map.Entry<MessageQueue, TopicOffset> entry : statsTable.getOffsetTable().entrySet()) {
            MessageQueue mq = entry.getKey();
            TopicOffset offset = entry.getValue();
            if (mq == null || offset == null) {
                continue;
            }

            long minOffset = offset.getMinOffset();
            long maxOffset = offset.getMaxOffset();
            long count = Math.max(0, maxOffset - minOffset);
            totalMessages += count;

            if (minOffset < globalMin) {
                globalMin = minOffset;
            }
            if (maxOffset > globalMax) {
                globalMax = maxOffset;
            }

            queueOffsets.add(new TopicQueueOffsetDTO(
                    mq.getBrokerName(),
                    mq.getQueueId(),
                    minOffset,
                    maxOffset,
                    offset.getLastUpdateTimestamp()
            ));
        }

        dto.setQueues(queueOffsets);
        dto.setTotalMessages(totalMessages);
        dto.setMinOffset(globalMin == Long.MAX_VALUE ? 0 : globalMin);
        dto.setMaxOffset(globalMax);

        return dto;
    }

    @Override
    public TopicOverviewDTO getTopicsOverview() throws Exception {
        TopicListDTO topicListDTO = listTopics(false);
        List<TopicOverviewDTO.TopicSummaryDTO> summaries = new ArrayList<>();
        for (String topic : topicListDTO.getTopics()) {
            if (topic == null || topic.isBlank()) {
                continue;
            }
            try {
                TopicRouteDTO route = getTopicRoute(topic);
                int totalRead = 0;
                int totalWrite = 0;
                Set<String> brokerSet = new LinkedHashSet<>();
                if (route != null && route.getQueueDatas() != null) {
                    for (QueueDataDTO qd : route.getQueueDatas()) {
                        if (qd != null) {
                            totalRead += qd.getReadQueueNums();
                            totalWrite += qd.getWriteQueueNums();
                            if (qd.getBrokerName() != null && !qd.getBrokerName().isBlank()) {
                                brokerSet.add(qd.getBrokerName());
                            }
                        }
                    }
                }
                summaries.add(new TopicOverviewDTO.TopicSummaryDTO(topic, totalRead, totalWrite, new ArrayList<>(brokerSet)));
            } catch (Exception e) {
                log.warn("Failed to fetch route for topic {} when building overview: {}", topic, e.getMessage());
                summaries.add(new TopicOverviewDTO.TopicSummaryDTO(topic, 0, 0, Collections.emptyList()));
            }
        }
        return new TopicOverviewDTO(summaries);
    }

    @Override
    public ConsumerGroupListDTO listConsumerGroups(boolean includeSystem) throws Exception {
        ClusterInfo clusterInfo;
        try {
            clusterInfo = examineBrokerClusterInfo();
        } catch (Exception e) {
            log.warn("Failed to examine cluster info when listing consumer groups: {}", e.getMessage());
            clusterInfo = null;
        }

        Set<String> allGroups = new HashSet<>();
        if (clusterInfo != null && clusterInfo.getBrokerAddrTable() != null) {
            for (BrokerData bd : clusterInfo.getBrokerAddrTable().values()) {
                if (bd != null && bd.getBrokerAddrs() != null) {
                    String masterAddr = bd.getBrokerAddrs().get(0L);
                    if (masterAddr != null && !masterAddr.isBlank()) {
                        try {
                            DefaultMQAdminExt client = ensureStarted();
                            SubscriptionGroupWrapper wrapper = client.getAllSubscriptionGroup(masterAddr, 3000L);
                            if (wrapper != null && wrapper.getSubscriptionGroupTable() != null) {
                                allGroups.addAll(wrapper.getSubscriptionGroupTable().keySet());
                            }
                        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
                            log.warn("Connection lost when fetching subscription groups from {}. Triggering reconnect: {}", masterAddr, e.getMessage());
                            reconnect();
                            try {
                                DefaultMQAdminExt client = ensureStarted();
                                SubscriptionGroupWrapper wrapper = client.getAllSubscriptionGroup(masterAddr, 3000L);
                                if (wrapper != null && wrapper.getSubscriptionGroupTable() != null) {
                                    allGroups.addAll(wrapper.getSubscriptionGroupTable().keySet());
                                }
                            } catch (Exception ex) {
                                log.warn("Retry failed to fetch subscription groups from {}: {}", masterAddr, ex.getMessage());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to fetch subscription groups from {}: {}", masterAddr, e.getMessage());
                        }
                    }
                }
            }
        }

        List<String> resultGroups;
        if (includeSystem) {
            resultGroups = allGroups.stream()
                    .filter(g -> g != null && !g.isBlank())
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            resultGroups = ConsumerGroupFilterUtils.filterBusinessConsumerGroups(allGroups);
        }

        return new ConsumerGroupListDTO(resultGroups);
    }

    @Override
    public ConsumerConnectionDTO getConsumerStatus(String consumerGroup) throws Exception {
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("consumerGroup 不能为空");
        }

        String targetGroup = consumerGroup.trim();
        ConsumerConnection connection = null;
        try {
            DefaultMQAdminExt client = ensureStarted();
            connection = client.examineConsumerConnectionInfo(targetGroup);
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
            log.warn("Connection lost when examining consumer connection for {}. Triggering reconnect: {}", targetGroup, e.getMessage());
            reconnect();
            DefaultMQAdminExt client = ensureStarted();
            connection = client.examineConsumerConnectionInfo(targetGroup);
        } catch (Exception e) {
            log.warn("Failed to examine consumer connection info for {}: {}", targetGroup, e.getMessage());
        }

        ConsumerConnectionDTO dto = new ConsumerConnectionDTO(targetGroup);
        if (connection == null) {
            return dto;
        }

        if (connection.getConsumeType() != null) {
            dto.setConsumeType(connection.getConsumeType().name());
        }
        if (connection.getMessageModel() != null) {
            dto.setMessageModel(connection.getMessageModel().name());
        }
        if (connection.getConsumeFromWhere() != null) {
            dto.setConsumeFromWhere(connection.getConsumeFromWhere().name());
        }

        if (connection.getConnectionSet() != null) {
            List<ConsumerClientDTO> clients = new ArrayList<>();
            for (Connection conn : connection.getConnectionSet()) {
                if (conn != null) {
                    clients.add(new ConsumerClientDTO(
                            conn.getClientId(),
                            conn.getClientAddr(),
                            conn.getLanguage() != null ? conn.getLanguage().name() : null,
                            conn.getVersion()
                    ));
                }
            }
            dto.setClients(clients);
        }

        if (connection.getSubscriptionTable() != null) {
            List<SubscriptionDTO> subscriptions = new ArrayList<>();
            for (SubscriptionData subData : connection.getSubscriptionTable().values()) {
                if (subData != null) {
                    subscriptions.add(new SubscriptionDTO(
                            subData.getTopic(),
                            subData.getSubString(),
                            subData.getTagsSet()
                    ));
                }
            }
            dto.setSubscriptions(subscriptions);
        }

        return dto;
    }

    @Override
    public ConsumerLagDTO getConsumerLag(String consumerGroup, String topic) throws Exception {
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("consumerGroup 不能为空");
        }

        String targetGroup = consumerGroup.trim();
        String targetTopic = (topic != null && !topic.isBlank()) ? topic.trim() : null;

        ConsumeStats stats = null;
        try {
            DefaultMQAdminExt client = ensureStarted();
            if (targetTopic != null) {
                stats = client.examineConsumeStats(targetGroup, targetTopic);
            } else {
                stats = client.examineConsumeStats(targetGroup);
            }
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
            log.warn("Connection lost when examining consume stats for {}. Triggering reconnect: {}", targetGroup, e.getMessage());
            reconnect();
            DefaultMQAdminExt client = ensureStarted();
            if (targetTopic != null) {
                stats = client.examineConsumeStats(targetGroup, targetTopic);
            } else {
                stats = client.examineConsumeStats(targetGroup);
            }
        } catch (Exception e) {
            log.warn("Failed to examine consume stats for {} / topic {}: {}", targetGroup, targetTopic, e.getMessage());
        }

        ConsumerLagDTO dto = new ConsumerLagDTO(targetGroup);
        dto.setTopic(targetTopic);

        if (stats == null || stats.getOffsetTable() == null || stats.getOffsetTable().isEmpty()) {
            return dto;
        }

        dto.setConsumeTps(stats.getConsumeTps());

        List<ConsumerQueueLagDTO> queueLags = new ArrayList<>();
        long totalLag = 0;
        for (Map.Entry<MessageQueue, OffsetWrapper> entry : stats.getOffsetTable().entrySet()) {
            MessageQueue mq = entry.getKey();
            OffsetWrapper ow = entry.getValue();
            if (mq == null || ow == null) {
                continue;
            }

            long bOffset = ow.getBrokerOffset();
            long cOffset = ow.getConsumerOffset();
            long lag = Math.max(0, bOffset - cOffset);
            totalLag += lag;

            queueLags.add(new ConsumerQueueLagDTO(
                    mq.getTopic(),
                    mq.getBrokerName(),
                    mq.getQueueId(),
                    bOffset,
                    cOffset,
                    ow.getLastTimestamp()
            ));
        }

        dto.setTotalLag(totalLag);
        dto.setQueues(queueLags);
        return dto;
    }

    @Override
    public TopConsumerLagDTO getTopConsumerLag(int topN) throws Exception {
        int limit = topN <= 0 ? 10 : topN;
        ConsumerGroupListDTO groupListDTO = listConsumerGroups(false);
        List<String> groups = groupListDTO.getGroups();

        List<ConsumerLagSummaryDTO> summaries = new ArrayList<>();
        for (String group : groups) {
            if (group == null || group.isBlank()) {
                continue;
            }
            try {
                DefaultMQAdminExt client = ensureStarted();
                ConsumeStats stats = client.examineConsumeStats(group.trim());
                long totalDiff = stats != null ? stats.computeTotalDiff() : 0L;
                double tps = stats != null ? stats.getConsumeTps() : 0.0;
                summaries.add(new ConsumerLagSummaryDTO(group.trim(), totalDiff, tps));
            } catch (Exception e) {
                // 当消费组离线或无对应 Topic 统计时，计为 0 积压并防御日志风暴
                log.debug("Consumer group {} has no active consume stats: {}", group, e.getMessage());
                summaries.add(new ConsumerLagSummaryDTO(group.trim(), 0L, 0.0));
            }
        }

        summaries.sort((a, b) -> Long.compare(b.getTotalLag(), a.getTotalLag()));
        List<ConsumerLagSummaryDTO> topResults = summaries.stream().limit(limit).collect(Collectors.toList());

        return new TopConsumerLagDTO(groupListDTO.getTotalCount(), topResults);
    }

    private MessageDetailDTO convertMessageExtToDTO(MessageExt msg) {
        if (msg == null) {
            return null;
        }
        String uniqKey = msg.getProperty(org.apache.rocketmq.common.message.MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
        String clientMsgId = (uniqKey != null && !uniqKey.isBlank()) ? uniqKey.trim() : msg.getMsgId();
        MessageDetailDTO dto = new MessageDetailDTO(clientMsgId, msg.getTopic());
        dto.setOffsetMsgId(msg.getMsgId());
        dto.setTags(msg.getTags());
        dto.setKeys(msg.getKeys());
        dto.setQueueId(msg.getQueueId());
        dto.setQueueOffset(msg.getQueueOffset());
        dto.setBornTimestamp(msg.getBornTimestamp());
        dto.setStoreTimestamp(msg.getStoreTimestamp());
        dto.setReconsumeTimes(msg.getReconsumeTimes());
        if (msg.getBornHost() != null) {
            dto.setBornHost(formatSocketAddress(msg.getBornHost()));
        }
        if (msg.getStoreHost() != null) {
            dto.setStoreHost(formatSocketAddress(msg.getStoreHost()));
        }
        if (msg.getProperties() != null) {
            dto.setProperties(new HashMap<>(msg.getProperties()));
        }
        byte[] body = msg.getBody();
        dto.setBodySize(body != null ? body.length : 0);
        dto.setBody(MessageBodyGuard.protect(body));
        return dto;
    }

    private String formatSocketAddress(java.net.SocketAddress socketAddress) {
        if (socketAddress == null) {
            return null;
        }
        String str = socketAddress.toString();
        return str.startsWith("/") ? str.substring(1) : str;
    }

    @Override
    public MessageDetailDTO queryMessageById(String msgId, String topic) throws Exception {
        if (msgId == null || msgId.isBlank()) {
            throw new IllegalArgumentException("msgId 不能为空");
        }
        String targetId = msgId.trim();
        String targetTopic = (topic != null && !topic.isBlank()) ? topic.trim() : null;

        MessageExt msg = null;
        try {
            msg = doQueryMessageById(targetId, targetTopic);
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
            log.warn("Connection lost when querying message by id {}. Triggering reconnect: {}", targetId, e.getMessage());
            reconnect();
            msg = doQueryMessageById(targetId, targetTopic);
        }

        if (msg == null) {
            throw new IllegalArgumentException("未找到 ID 为 " + targetId + " 的消息");
        }
        return convertMessageExtToDTO(msg);
    }

    private MessageExt doQueryMessageById(String targetId, String targetTopic) throws Exception {
        DefaultMQAdminExt client = ensureStarted();
        if (targetTopic != null) {
            try {
                return client.viewMessage(targetTopic, targetId);
            } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
                throw e; // 网络通信故障向上透传触发重试/自愈
            } catch (Exception e) {
                // 消息未找到等业务异常记录 DEBUG 并返回 null，杜绝误触发重连
                log.debug("Message {} not found in topic {}: {}", targetId, targetTopic, e.getMessage());
                return null;
            }
        }

        TopicListDTO topicListDTO = listTopics(false);
        if (topicListDTO != null && topicListDTO.getTopics() != null) {
            for (String currentTopic : topicListDTO.getTopics()) {
                try {
                    MessageExt candidate = client.viewMessage(currentTopic, targetId);
                    if (candidate != null) {
                        return candidate;
                    }
                } catch (Exception ignored) {
                    // 业务主题未匹配当前消息，继续遍历下一个主题
                }
            }
        }
        return null;
    }

    @Override
    public MessageListDTO queryMessageByKey(String topic, String key, Long beginTimestamp, Long endTimestamp, Integer maxNum) throws Exception {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic 不能为空");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key 不能为空");
        }
        if (beginTimestamp != null && endTimestamp != null && beginTimestamp > endTimestamp) {
            throw new IllegalArgumentException("beginTimestamp (" + beginTimestamp + ") 不能大于 endTimestamp (" + endTimestamp + ")");
        }
        if (maxNum != null && (maxNum < 1 || maxNum > 64)) {
            throw new IllegalArgumentException("maxNum 必须在 1 到 64 之间，当前为: " + maxNum);
        }

        String targetTopic = topic.trim();
        String targetKey = key.trim();
        long end = (endTimestamp != null && endTimestamp > 0) ? endTimestamp : System.currentTimeMillis();
        long begin = (beginTimestamp != null && beginTimestamp > 0) ? beginTimestamp : (end - 24L * 3600 * 1000);
        int max = (maxNum != null && maxNum > 0) ? maxNum : 32;

        QueryResult qr = null;
        try {
            DefaultMQAdminExt client = ensureStarted();
            qr = client.queryMessage(targetTopic, targetKey, max, begin, end);
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
            log.warn("Connection lost when querying message by key {} on topic {}. Triggering reconnect: {}", targetKey, targetTopic, e.getMessage());
            reconnect();
            DefaultMQAdminExt client = ensureStarted();
            qr = client.queryMessage(targetTopic, targetKey, max, begin, end);
        } catch (Exception e) {
            log.warn("No message found or query failed for key {} on topic {}: {}", targetKey, targetTopic, e.getMessage());
        }

        if (qr == null || qr.getMessageList() == null || qr.getMessageList().isEmpty()) {
            return new MessageListDTO(Collections.emptyList());
        }

        List<MessageDetailDTO> list = qr.getMessageList().stream()
                .map(this::convertMessageExtToDTO)
                .collect(Collectors.toList());
        return new MessageListDTO(list);
    }

    @Override
    public DlqMessageListDTO queryDlqMessages(String consumerGroup, Long beginTimestamp, Long endTimestamp, Integer maxNum) throws Exception {
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("consumerGroup 不能为空");
        }

        String group = consumerGroup.trim();
        String dlqTopic = "%DLQ%" + group;
        long end = (endTimestamp != null && endTimestamp > 0) ? endTimestamp : System.currentTimeMillis();
        long begin = (beginTimestamp != null && beginTimestamp > 0) ? beginTimestamp : (end - 24L * 3600 * 1000);
        int max = (maxNum != null && maxNum > 0) ? maxNum : 32;

        QueryResult qr = null;
        try {
            DefaultMQAdminExt client = ensureStarted();
            qr = client.queryMessage(dlqTopic, "*", max, begin, end);
        } catch (RemotingConnectException | RemotingTimeoutException | RemotingSendRequestException e) {
            log.warn("Connection lost when querying dlq messages for {}. Triggering reconnect: {}", group, e.getMessage());
            reconnect();
            DefaultMQAdminExt client = ensureStarted();
            qr = client.queryMessage(dlqTopic, "*", max, begin, end);
        } catch (Exception e) {
            log.info("No DLQ messages found or DLQ topic does not exist for consumer group {}: {}", group, e.getMessage());
        }

        List<MessageDetailDTO> list = (qr != null && qr.getMessageList() != null)
                ? qr.getMessageList().stream().map(this::convertMessageExtToDTO).collect(Collectors.toList())
                : Collections.emptyList();

        return new DlqMessageListDTO(group, dlqTopic, list);
    }

    @Override
    public MessageTraceDTO queryMessageTrace(String msgId, String topic) throws Exception {
        if (msgId == null || msgId.isBlank()) {
            throw new IllegalArgumentException("msgId 不能为空");
        }

        String targetId = msgId.trim();
        String targetTopic = (topic != null && !topic.isBlank()) ? topic.trim() : null;

        MessageTraceDTO dto = new MessageTraceDTO(targetId, targetTopic);
        List<MessageTraceNodeDTO> nodes = new ArrayList<>();

        try {
            DefaultMQAdminExt client = ensureStarted();
            long end = System.currentTimeMillis();
            long begin = end - 24L * 3600 * 1000;
            QueryResult qr = client.queryMessage("RMQ_SYS_TRACE_TOPIC", targetId, 32, begin, end);
            if (qr != null && qr.getMessageList() != null) {
                for (MessageExt traceMsg : qr.getMessageList()) {
                    List<TraceView> traceViews = TraceView.decodeFromTraceTransData(targetId, traceMsg);
                    if (traceViews != null) {
                        for (TraceView tv : traceViews) {
                            nodes.add(new MessageTraceNodeDTO(
                                    tv.getMsgType(),
                                    tv.getClientHost() != null ? tv.getClientHost() : tv.getStoreHost(),
                                    tv.getCostTime(),
                                    tv.getTimeStamp(),
                                    tv.getStatus(),
                                    tv.getGroupName()
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("No trace records found in RMQ_SYS_TRACE_TOPIC for {}: {}", targetId, e.getMessage());
        }

        if (nodes.isEmpty() && targetTopic != null) {
            try {
                MessageDetailDTO msg = queryMessageById(targetId, targetTopic);
                if (msg != null) {
                    if (msg.getBornTimestamp() > 0) {
                        nodes.add(new MessageTraceNodeDTO("Pub", msg.getBornHost(), 0, msg.getBornTimestamp(), "SUCCESS", null));
                    }
                    if (msg.getStoreTimestamp() > 0) {
                        int storeCost = (int) Math.max(0, msg.getStoreTimestamp() - msg.getBornTimestamp());
                        nodes.add(new MessageTraceNodeDTO("Broker", msg.getStoreHost(), storeCost, msg.getStoreTimestamp(), "SUCCESS", null));
                    }
                }
            } catch (Exception e) {
                log.debug("Fallback message lookup failed for trace {}: {}", targetId, e.getMessage());
            }
        }

        nodes.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        dto.setNodes(nodes);
        return dto;
    }

    /**
     * 将 Broker 标识解析为实际的网络通信地址。若入参仅为 Broker 名称则从集群路由表检索 Master 物理地址。
     *
     * @param brokerAddrOrName Broker 网络地址（IP:PORT）或实例名称
     * @return 实际通信的目标物理网络地址
     */
    public String resolveBrokerAddress(String brokerAddrOrName) {
        if (brokerAddrOrName.contains(":")) {
            return brokerAddrOrName;
        }
        try {
            ClusterInfo clusterInfo = examineBrokerClusterInfo();
            if (clusterInfo != null && clusterInfo.getBrokerAddrTable() != null) {
                BrokerData brokerData = clusterInfo.getBrokerAddrTable().get(brokerAddrOrName);
                if (brokerData != null && brokerData.getBrokerAddrs() != null) {
                    String masterAddr = brokerData.getBrokerAddrs().get(0L);
                    if (masterAddr != null && !masterAddr.isBlank()) {
                        return masterAddr;
                    }
                    if (!brokerData.getBrokerAddrs().isEmpty()) {
                        return brokerData.getBrokerAddrs().values().iterator().next();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve broker address by name {}: {}", brokerAddrOrName, e.getMessage());
        }
        return brokerAddrOrName;
    }

    @Override
    public DefaultMQAdminExt getMQAdminExt() {
        return mqAdminExt;
    }

    @Override
    public TopicOperationResultDTO createTopic(String topic, Integer readQueueNums, Integer writeQueueNums, Integer perm) throws Exception {
        // 1. 前置卫语句判空与参数清洗
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Parameter 'topic' must not be blank");
        }
        int readQueues = (readQueueNums != null && readQueueNums > 0) ? readQueueNums : DEFAULT_QUEUE_NUMS;
        int writeQueues = (writeQueueNums != null && writeQueueNums > 0) ? writeQueueNums : DEFAULT_QUEUE_NUMS;
        int permission = (perm != null && perm >= 0) ? perm : DEFAULT_TOPIC_PERM;

        // 2. 装配 Topic 配置并同步下发至各 Master Broker
        DefaultMQAdminExt client = ensureStarted();
        ClusterInfo clusterInfo = examineBrokerClusterInfo();
        TopicConfig topicConfig = new TopicConfig(topic.trim());
        topicConfig.setReadQueueNums(readQueues);
        topicConfig.setWriteQueueNums(writeQueues);
        topicConfig.setPerm(permission);

        Set<String> masterBrokers = getMasterBrokerAddresses(clusterInfo);
        if (masterBrokers.isEmpty()) {
            throw new IllegalStateException("集群中未发现活跃的 Master Broker 节点，无法创建主题: " + topic.trim());
        }
        int updatedBrokers = 0;
        for (String masterAddr : masterBrokers) {
            client.createAndUpdateTopicConfig(masterAddr, topicConfig);
            updatedBrokers++;
        }
        log.info("Successfully created or updated topic '{}' (readQueues={}, writeQueues={}, perm={}) across {} broker(s)",
                topic.trim(), readQueues, writeQueues, permission, updatedBrokers);
        return new TopicOperationResultDTO("CREATE_TOPIC", topic.trim(), "SUCCESS",
                "Successfully created or updated topic on " + updatedBrokers + " broker(s)");
    }

    @Override
    public TopicOperationResultDTO deleteTopic(String topic) throws Exception {
        // 1. 前置卫语句防御与系统主题拦截
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Parameter 'topic' must not be blank");
        }
        if (TopicFilterUtils.isSystemTopic(topic.trim())) {
            throw new IllegalArgumentException("Cannot delete internal system topic: " + topic);
        }

        // 2. 依次向 Broker 与 NameServer 双端发起主题元数据物理清理
        DefaultMQAdminExt client = ensureStarted();
        ClusterInfo clusterInfo = examineBrokerClusterInfo();
        Set<String> masterBrokers = getMasterBrokerAddresses(clusterInfo);
        if (!masterBrokers.isEmpty()) {
            client.deleteTopicInBroker(masterBrokers, topic.trim());
        }

        Set<String> namesrvSet = Collections.emptySet();
        if (properties.getNamesrvAddr() != null && !properties.getNamesrvAddr().isBlank()) {
            namesrvSet = Arrays.stream(properties.getNamesrvAddr().split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        if (!namesrvSet.isEmpty()) {
            client.deleteTopicInNameServer(namesrvSet, topic.trim());
        }

        log.info("Successfully deleted topic '{}' from {} brokers and {} nameservers",
                topic.trim(), masterBrokers.size(), namesrvSet.size());
        return new TopicOperationResultDTO("DELETE_TOPIC", topic.trim(), "SUCCESS",
                "Successfully deleted topic from brokers and nameservers");
    }

    @Override
    public ResetOffsetResultDTO resetOffset(String consumerGroup, String topic, Long timestamp, Boolean resetToMax) throws Exception {
        // 1. 前置卫语句判空防御与时间模式决策
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("Parameter 'consumerGroup' must not be blank");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Parameter 'topic' must not be blank");
        }

        boolean toMax = Boolean.TRUE.equals(resetToMax);
        long targetTime;
        String mode;
        if (toMax) {
            mode = "MAX_OFFSET";
            targetTime = System.currentTimeMillis();
        } else {
            mode = "TIMESTAMP";
            if (timestamp == null || timestamp <= 0) {
                throw new IllegalArgumentException("Parameter 'timestamp' must be greater than 0 when resetToMax is false");
            }
            targetTime = timestamp;
        }

        // 2. 调度 AdminRemoting 客户端执行消费位点回拨
        DefaultMQAdminExt client = ensureStarted();
        client.resetOffsetByTimestamp(topic.trim(), consumerGroup.trim(), targetTime, true);
        log.info("Successfully reset offset for group '{}' on topic '{}' to mode '{}' (targetTime={})",
                consumerGroup.trim(), topic.trim(), mode, targetTime);
        return new ResetOffsetResultDTO(consumerGroup.trim(), topic.trim(), mode, targetTime, "SUCCESS",
                "Consumer offset reset successfully");
    }

    @Override
    public ResendDlqResultDTO resendDlqMessage(String consumerGroup, String msgId, String targetTopic) throws Exception {
        // 1. 前置卫语句防御与服务可用性校验
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("Parameter 'consumerGroup' must not be blank");
        }
        if (msgId == null || msgId.isBlank()) {
            throw new IllegalArgumentException("Parameter 'msgId' must not be blank");
        }
        if (messagingClientService == null) {
            throw new IllegalStateException("MessagingClientService is unavailable for resending DLQ message");
        }

        // 2. 检索死信队列报文并解析目标投递主题
        DefaultMQAdminExt client = ensureStarted();
        String dlqTopic = "%DLQ%" + consumerGroup.trim();
        MessageExt msg = client.viewMessage(dlqTopic, msgId.trim());
        if (msg == null) {
            throw new IllegalArgumentException("Message not found with id: " + msgId);
        }

        String resolvedTopic = targetTopic;
        if (resolvedTopic == null || resolvedTopic.isBlank()) {
            resolvedTopic = msg.getProperty(MessageConst.PROPERTY_REAL_TOPIC);
        }
        if (resolvedTopic == null || resolvedTopic.isBlank()) {
            throw new IllegalArgumentException("Cannot determine target topic for message " + msgId + ", please specify 'targetTopic' explicitly");
        }

        // 3. 执行消息重投投递并装配结果
        String body = (msg.getBody() != null) ? new String(msg.getBody(), StandardCharsets.UTF_8) : "";
        String tag = msg.getTags();
        String keys = msg.getKeys();

        SendMessageResultDTO sendResult = messagingClientService.sendMessage(resolvedTopic.trim(), body, tag, keys, null, null);
        String newMsgId = (sendResult != null) ? sendResult.getMessageId() : null;
        log.info("Successfully resent DLQ message '{}' (group: {}) to topic '{}', new messageId: {}",
                msgId.trim(), consumerGroup.trim(), resolvedTopic.trim(), newMsgId);
        return new ResendDlqResultDTO(consumerGroup.trim(), msgId.trim(), resolvedTopic.trim(), newMsgId, "SUCCESS",
                "Dead letter message successfully resent to topic " + resolvedTopic.trim());
    }

    /**
     * 从集群拓扑中提取所有 Master Broker 的通信地址集合。
     *
     * @param clusterInfo 集群元数据信息
     * @return Master Broker 地址集合，不为 null
     */
    private Set<String> getMasterBrokerAddresses(ClusterInfo clusterInfo) {
        if (clusterInfo == null || clusterInfo.getBrokerAddrTable() == null) {
            return Collections.emptySet();
        }
        Set<String> masterBrokers = new HashSet<>();
        for (BrokerData brokerData : clusterInfo.getBrokerAddrTable().values()) {
            if (brokerData != null && brokerData.getBrokerAddrs() != null) {
                String masterAddr = brokerData.getBrokerAddrs().get(MASTER_BROKER_ID);
                if (masterAddr != null && !masterAddr.isBlank()) {
                    masterBrokers.add(masterAddr);
                }
            }
        }
        return masterBrokers;
    }

    @Override
    public synchronized void destroy() {
        if (started && mqAdminExt != null) {
            try {
                mqAdminExt.shutdown();
                log.info("DefaultMQAdminExt successfully shut down.");
            } catch (Exception e) {
                log.warn("Error while shutting down DefaultMQAdminExt: {}", e.getMessage());
            } finally {
                started = false;
            }
        }
    }
}
