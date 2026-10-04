package com.ateng.mcp.rocketmq.mcp.tool;

import com.ateng.mcp.rocketmq.rocketmq.messaging.MessagingClientService;
import com.ateng.mcp.rocketmq.rocketmq.messaging.dto.SendMessageResultDTO;
import com.ateng.mcp.rocketmq.security.ReadOnlyGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 5.x gRPC 测试消息生产与发送 MCP 工具。
 * 支持普通、顺序分区与定时延时消息在线发送与连通性验证，全局受只读安全守卫约束。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class MessageProduceTools {

    private static final Logger log = LoggerFactory.getLogger(MessageProduceTools.class);

    private final MessagingClientService messagingClientService;
    private final ReadOnlyGuard readOnlyGuard;

    public MessageProduceTools(MessagingClientService messagingClientService, ReadOnlyGuard readOnlyGuard) {
        this.messagingClientService = messagingClientService;
        this.readOnlyGuard = readOnlyGuard;
    }

    /**
     * 向指定 RocketMQ 主题发送联调测试消息。
     *
     * @param topic 目标主题名称
     * @param body 消息体内容文本
     * @param tag 消息标签（可选）
     * @param keys 业务索引键（可选）
     * @param messageGroup 分区顺序消息组（可选）
     * @param delaySeconds 延时投递秒数（可选）
     * @param deliveryTimestamp 定时投递绝对时间戳毫秒值（可选）
     * @return 消息发送确认结果 DTO
     * @throws Exception 当只读守卫拦截或底层通信异常时抛出
     */
    @McpTool(
            name = "rocketmq_send_message",
            description = "向指定 RocketMQ 主题发送联调测试消息（支持普通消息、按 messageGroup 分区顺序消息及延时定时消息），全局受只读模式保护。"
    )
    public SendMessageResultDTO sendMessage(
            @McpToolParam(description = "目标主题名称", required = true)
            String topic,
            @McpToolParam(description = "消息体内容文本", required = true)
            String body,
            @McpToolParam(description = "消息标签 Tag（可选，用于消费端属性过滤）", required = false)
            String tag,
            @McpToolParam(description = "业务索引键 Keys（可选，用于业务链路检索）", required = false)
            String keys,
            @McpToolParam(description = "顺序消息分区组 messageGroup（可选，指定时将严格按分区保序投递）", required = false)
            String messageGroup,
            @McpToolParam(description = "延时投递时间（秒，可选，例如 60 表示 1 分钟后投递）", required = false)
            Integer delaySeconds,
            @McpToolParam(description = "定时投递目标毫秒时间戳 deliveryTimestamp（可选，与 delaySeconds 二选一）", required = false)
            Long deliveryTimestamp) throws Exception {

        // 1. 全局只读安全拦截校验
        readOnlyGuard.checkWritable("rocketmq_send_message");

        // 2. 核心入参前置卫语句防御
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Parameter 'topic' must not be blank");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Parameter 'body' must not be blank");
        }

        // 3. 延时与定时投递参数合法性校验
        if (delaySeconds != null && delaySeconds <= 0) {
            throw new IllegalArgumentException("Parameter 'delaySeconds' must be greater than 0");
        }
        Long targetDeliveryTimestamp = deliveryTimestamp;
        if (targetDeliveryTimestamp != null && targetDeliveryTimestamp <= System.currentTimeMillis()) {
            throw new IllegalArgumentException("Parameter 'deliveryTimestamp' must be in the future");
        }
        if (targetDeliveryTimestamp == null && delaySeconds != null) {
            targetDeliveryTimestamp = System.currentTimeMillis() + (delaySeconds * 1000L);
        }

        // 4. 顺序消息分区组与延时消息互斥防呆（RocketMQ 5.x 规范限制）
        if (messageGroup != null && !messageGroup.isBlank() && targetDeliveryTimestamp != null) {
            throw new IllegalArgumentException(
                    "Cannot specify both 'messageGroup' and delay/delivery timestamp: FIFO and Delay messages are mutually exclusive in RocketMQ 5.x");
        }

        log.info("Executing MCP Tool: rocketmq_send_message to topic: {}, tag: {}, keys: {}, group: {}, delayTimestamp: {}",
                topic.trim(), tag, keys, messageGroup, targetDeliveryTimestamp);

        return messagingClientService.sendMessage(
                topic.trim(),
                body,
                tag,
                keys,
                messageGroup,
                targetDeliveryTimestamp
        );
    }
}
