package com.ateng.mcp.rocketmq.rocketmq.admin;

import com.ateng.mcp.rocketmq.rocketmq.admin.dto.BrokerStatsDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ClusterInfoDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ConsumerConnectionDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ConsumerGroupListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ConsumerLagDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.DlqMessageListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.MessageDetailDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.MessageListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.MessageTraceDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ResendDlqResultDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ResetOffsetResultDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopConsumerLagDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicOperationResultDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicOverviewDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicRouteDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicStatusDTO;
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
     * 查询集群主题列表。
     *
     * @param includeSystem 是否包含系统内部保留主题（默认应为 false）
     * @return 格式化后的 TopicListDTO 实例
     * @throws Exception 当底层通信或查询异常时抛出
     */
    TopicListDTO listTopics(boolean includeSystem) throws Exception;

    /**
     * 查询指定主题的读写队列分布与 Broker 路由详情。
     *
     * @param topic 主题名称
     * @return 格式化后的 TopicRouteDTO 实例
     * @throws Exception 当底层通信或路由查询异常时抛出
     */
    TopicRouteDTO getTopicRoute(String topic) throws Exception;

    /**
     * 查询指定主题各分片队列的位点统计与消息留存容量。
     *
     * @param topic 主题名称
     * @return 格式化后的 TopicStatusDTO 实例
     * @throws Exception 当底层通信或位点计算异常时抛出
     */
    TopicStatusDTO getTopicStatus(String topic) throws Exception;

    /**
     * 获取业务主题全景概览，包含业务主题列表、读写队列规模与分布节点。
     *
     * @return 格式化后的 TopicOverviewDTO 实例
     * @throws Exception 当底层通信或查询异常时抛出
     */
    TopicOverviewDTO getTopicsOverview() throws Exception;

    /**
     * 查询集群消费组列表。
     *
     * @param includeSystem 是否包含系统内置保留消费组（默认为 false）
     * @return 格式化后的 ConsumerGroupListDTO 实例
     * @throws Exception 当底层通信或查询异常时抛出
     */
    ConsumerGroupListDTO listConsumerGroups(boolean includeSystem) throws Exception;

    /**
     * 查询指定消费组的在线客户端连接与订阅健康度状态。
     *
     * @param consumerGroup 消费组名称
     * @return 格式化后的 ConsumerConnectionDTO 实例
     * @throws Exception 当底层通信或查询异常时抛出
     */
    ConsumerConnectionDTO getConsumerStatus(String consumerGroup) throws Exception;

    /**
     * 查询指定消费组各分片队列的消费点位、最大位点与实时积压量 (Lag)。
     *
     * @param consumerGroup 消费组名称
     * @param topic 指定过滤的主题名称（可选，若为空则包含所有订阅主题）
     * @return 格式化后的 ConsumerLagDTO 实例
     * @throws Exception 当底层通信或计算异常时抛出
     */
    ConsumerLagDTO getConsumerLag(String consumerGroup, String topic) throws Exception;

    /**
     * 查询全集群业务消费组总未消费积压量降序排列的 TopN 排行榜。
     *
     * @param topN 截取数量，小于等于 0 时默认为 10
     * @return 格式化后的 TopConsumerLagDTO 实例
     * @throws Exception 当底层通信或计算异常时抛出
     */
    TopConsumerLagDTO getTopConsumerLag(int topN) throws Exception;

    /**
     * 根据消息 ID 精确查询消息全景属性与消息体内容（受防爆截断保护）。
     *
     * @param msgId 消息全局唯一标识 ID（必填）
     * @param topic 主题名称（建议提供以提升检索效率，可选）
     * @return 格式化后的 MessageDetailDTO 实例
     * @throws Exception 当底层通信或查询异常时抛出
     */
    MessageDetailDTO queryMessageById(String msgId, String topic) throws Exception;

    /**
     * 根据业务索引 Key 在指定时间窗口内扫描并检索消息列表。
     *
     * @param topic 目标主题名称（必填）
     * @param key 业务索引 Key（必填）
     * @param beginTimestamp 开始时间戳（毫秒，可选）
     * @param endTimestamp 结束时间戳（毫秒，可选）
     * @param maxNum 最大返回条数（可选，默认为 32）
     * @return 格式化后的 MessageListDTO 实例
     * @throws Exception 当底层通信或查询异常时抛出
     */
    MessageListDTO queryMessageByKey(String topic, String key, Long beginTimestamp, Long endTimestamp, Integer maxNum) throws Exception;

    /**
     * 查询指定消费组死信队列（%DLQ%consumerGroup）中的堆积消息。
     *
     * @param consumerGroup 消费组名称（必填）
     * @param beginTimestamp 开始时间戳（毫秒，可选）
     * @param endTimestamp 结束时间戳（毫秒，可选）
     * @param maxNum 最大返回条数（可选，默认为 32）
     * @return 格式化后的 DlqMessageListDTO 实例
     * @throws Exception 当底层通信或查询异常时抛出
     */
    DlqMessageListDTO queryDlqMessages(String consumerGroup, Long beginTimestamp, Long endTimestamp, Integer maxNum) throws Exception;

    /**
     * 调阅指定消息在全生命周期中的投递轨迹时间线与耗时。
     *
     * @param msgId 消息唯一标识 ID（必填）
     * @param topic 主题名称（可选）
     * @return 格式化后的 MessageTraceDTO 实例
     * @throws Exception 当底层通信或查询异常时抛出
     */
    MessageTraceDTO queryMessageTrace(String msgId, String topic) throws Exception;

    /**
     * 获取受管的 DefaultMQAdminExt 单例实例。
     *
     * @return DefaultMQAdminExt 底层管理客户端
     */
    DefaultMQAdminExt getMQAdminExt();

    /**
     * 声明式创建或更新业务主题配置（包含读写队列数与权限模式）。
     *
     * @param topic 主题名称（必填）
     * @param readQueueNums 读队列数量（可选，默认 8）
     * @param writeQueueNums 写队列数量（可选，默认 8）
     * @param perm 权限模式（可选，默认 6 即读写）
     * @return 格式化后的 TopicOperationResultDTO 实例
     * @throws Exception 当底层创建失败时抛出
     */
    TopicOperationResultDTO createTopic(String topic, Integer readQueueNums, Integer writeQueueNums, Integer perm) throws Exception;

    /**
     * 删除指定的业务主题（禁止删除系统保留主题）。
     *
     * @param topic 待删除的主题名称（必填）
     * @return 格式化后的 TopicOperationResultDTO 实例
     * @throws Exception 当底层删除失败时抛出
     */
    TopicOperationResultDTO deleteTopic(String topic) throws Exception;

    /**
     * 重置指定消费组在目标主题上的消费位点（支持按时间戳或跳过积压至最大位点）。
     *
     * @param consumerGroup 消费组名称（必填）
     * @param topic 目标主题名称（必填）
     * @param timestamp 目标时间戳（毫秒，按时间戳重置模式必填）
     * @param resetToMax 是否直接重置到最新最大位点以跳过积压（默认 false）
     * @return 格式化后的 ResetOffsetResultDTO 实例
     * @throws Exception 当底层重置位点失败时抛出
     */
    ResetOffsetResultDTO resetOffset(String consumerGroup, String topic, Long timestamp, Boolean resetToMax) throws Exception;

    /**
     * 将指定死信队列中的单条死信消息重新投递回业务目标主题。
     *
     * @param consumerGroup 消费组名称（必填）
     * @param msgId 死信消息 ID（必填）
     * @param targetTopic 目标业务主题名称（可选，若为空则自动解析原真实主题）
     * @return 格式化后的 ResendDlqResultDTO 实例
     * @throws Exception 当底层消息检索或重投失败时抛出
     */
    ResendDlqResultDTO resendDlqMessage(String consumerGroup, String msgId, String targetTopic) throws Exception;
}
