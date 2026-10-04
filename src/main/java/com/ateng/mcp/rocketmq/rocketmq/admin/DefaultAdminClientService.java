package com.ateng.mcp.rocketmq.rocketmq.admin;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerStatsDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerSummaryDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.body.KVTable;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
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

/**
 * RocketMQ 运维管理客户端服务默认实现。
 * 负责管理 DefaultMQAdminExt 单例生命周期、自愈重连并提供对集群底层元数据与指标的安全转换访问。
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
