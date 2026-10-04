package com.ateng.mcp.rocketmq.rocketmq.admin;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerStatsDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerSummaryDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.QueueDataDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicOverviewDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicQueueOffsetDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicRouteDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicStatusDTO;
import com.ateng.mcp.rocketmq.rocketmq.util.TopicFilterUtils;
import java.util.LinkedHashSet;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.admin.TopicOffset;
import org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.body.KVTable;
import org.apache.rocketmq.remoting.protocol.body.TopicList;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.apache.rocketmq.remoting.protocol.route.QueueData;
import org.apache.rocketmq.remoting.protocol.route.TopicRouteData;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    private final RocketmqProperties properties;
    private DefaultMQAdminExt mqAdminExt;
    private volatile boolean started = false;

    public DefaultAdminClientService(RocketmqProperties properties) {
        this.properties = properties;
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
        return new DefaultMQAdminExt();
    }

    public synchronized void reconnect() {
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

    protected synchronized DefaultMQAdminExt ensureStarted() throws Exception {
        if (!started || mqAdminExt == null) {
            initAdminClient();
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
    public void destroy() {
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
