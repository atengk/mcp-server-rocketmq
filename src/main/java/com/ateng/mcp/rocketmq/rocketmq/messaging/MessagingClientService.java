package com.ateng.mcp.rocketmq.rocketmq.messaging;

import com.ateng.mcp.rocketmq.rocketmq.messaging.dto.SendMessageResultDTO;

/**
 * RocketMQ 5.x gRPC 消息收发核心服务接口。
 * 封装 rocketmq-client-java 驱动，承载控制面测试消息发送能力。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public interface MessagingClientService {

    /**
     * 向指定 RocketMQ 主题发送测试消息（支持普通、分区顺序与定时延时消息）。
     *
     * @param topic 目标主题名称（必填）
     * @param body 消息体文本内容（必填）
     * @param tag 消息所属标签（可选）
     * @param keys 业务索引键（可选）
     * @param messageGroup 分区顺序消息组标识（可选，用于顺序消息保序）
     * @param deliveryTimestamp 定时/延时投递目标时间戳毫秒值（可选，用于延时消息）
     * @return 格式化后的 SendMessageResultDTO 实例
     * @throws Exception 当底层 gRPC 通信或发送失败时抛出
     */
    SendMessageResultDTO sendMessage(String topic, String body, String tag, String keys,
                                     String messageGroup, Long deliveryTimestamp) throws Exception;
}
