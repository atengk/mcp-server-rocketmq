package com.ateng.mcp.rocketmq.rocketmq.admin;

import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerStatsDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;

/**
 * RocketMQ 运维管理驱动服务接口。
 * 基于 Remoting 协议封装集群拓扑感知、Topic 与消费组管控等底层运维操作。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public interface AdminClientService {

    /**
     * 调阅底层原生 RocketMQ 集群拓扑原始报文。
     *
     * @return RocketMQ 原生 ClusterInfo 实例
     * @throws Exception 当底层通信或查询异常时抛出
     */
    ClusterInfo examineBrokerClusterInfo() throws Exception;

    /**
     * 获取高阶转换后的集群拓扑与 Broker 节点摘要数据。
     *
     * @return 格式化后的 ClusterInfoDTO 实例
     * @throws Exception 当底层通讯或数据解析失败时抛出
     */
    ClusterInfoDTO getClusterInfo() throws Exception;

    /**
     * 获取指定 Broker 节点的核心运行时指标与物理磁盘水位。
     *
     * @param brokerAddr Broker 网络通信地址（IP:PORT）或 Broker 名称
     * @return 格式化后的 BrokerStatsDTO 实例
     * @throws Exception 当底层通信或指标提取异常时抛出
     */
    BrokerStatsDTO getBrokerStats(String brokerAddr) throws Exception;

    /**
     * 获取受管的 DefaultMQAdminExt 单例实例。
     *
     * @return DefaultMQAdminExt 底层管理客户端
     */
    DefaultMQAdminExt getMQAdminExt();
}
