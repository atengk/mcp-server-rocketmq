package com.ateng.mcp.rocketmq.rocketmq.admin;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerSummaryDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
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
 * 负责管理 DefaultMQAdminExt 单例生命周期并提供对集群底层元数据的安全转换访问。
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
            this.mqAdminExt = new DefaultMQAdminExt();
            this.mqAdminExt.setNamesrvAddr(properties.getNamesrvAddr());
            this.mqAdminExt.setInstanceName("mcp-admin-" + System.currentTimeMillis());
            this.mqAdminExt.start();
            this.started = true;
            log.info("DefaultMQAdminExt successfully initialized with NameServer: {}", properties.getNamesrvAddr());
        } catch (Exception e) {
            log.warn("Failed to start DefaultMQAdminExt eagerly. Will retry on first operational call. Reason: {}", e.getMessage());
        }
    }

    private synchronized DefaultMQAdminExt ensureStarted() throws Exception {
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
        DefaultMQAdminExt client = ensureStarted();
        return client.examineBrokerClusterInfo();
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
