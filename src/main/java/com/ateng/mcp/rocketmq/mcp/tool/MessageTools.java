package com.ateng.mcp.rocketmq.mcp.tool;

import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.DlqMessageListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.MessageDetailDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.MessageListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.MessageTraceDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.ResendDlqResultDTO;
import com.ateng.mcp.rocketmq.security.DualLayerGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消息检索、死信查验、死信重投与轨迹追踪 MCP 工具集。
 * 提供按 ID/Key 检索消息、死信队列扫描、死信重新投递与消息生命周期轨迹追踪能力。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class MessageTools {

    private static final Logger log = LoggerFactory.getLogger(MessageTools.class);

    private final AdminClientService adminClientService;
    private final DualLayerGuard dualLayerGuard;

    public MessageTools(AdminClientService adminClientService, DualLayerGuard dualLayerGuard) {
        this.adminClientService = adminClientService;
        this.dualLayerGuard = dualLayerGuard;
    }

    /**
     * 按消息全局唯一 ID (msgId 或 offsetMsgId) 精确检索单条消息详情及解码报文体。
     *
     * @param msgId 待查询的消息 ID
     * @param topic 消息所属主题（可选）
     * @return 消息明细 DTO
     * @throws Exception 当查询异常或入参无效时抛出
     */
    @McpTool(
            name = "rocketmq_query_message_by_id",
            description = "按消息全局唯一 Message ID (msgId) 或 offsetMsgId 精确检索单条消息详情及解码报文体。"
    )
    public MessageDetailDTO queryMessageById(
            @McpToolParam(description = "待查询的消息全局唯一 ID (msgId 或 offsetMsgId)", required = true)
            String msgId,
            @McpToolParam(description = "消息所属主题（可选，传入可加速定位与路由）", required = false)
            String topic) throws Exception {
        if (msgId == null || msgId.isBlank()) {
            throw new IllegalArgumentException("Parameter 'msgId' must not be blank");
        }
        String cleanTopic = (topic != null && !topic.isBlank()) ? topic.trim() : null;
        log.info("Executing MCP Tool: rocketmq_query_message_by_id with msgId: {}, topic: {}", msgId.trim(), cleanTopic);
        return adminClientService.queryMessageById(msgId.trim(), cleanTopic);
    }

    /**
     * 根据业务索引键 (Message Key) 与所属主题检索消息列表。
     *
     * @param topic 消息所属主题名称
     * @param key 业务索引键
     * @param beginTimestamp 起始时间戳（毫秒）
     * @param endTimestamp 结束时间戳（毫秒）
     * @param maxNum 最多返回消息条数
     * @return 消息列表 DTO
     * @throws Exception 当查询异常或入参无效时抛出
     */
    @McpTool(
            name = "rocketmq_query_message_by_key",
            description = "根据业务索引键 (Message Key) 与所属主题检索消息列表。"
    )
    public MessageListDTO queryMessageByKey(
            @McpToolParam(description = "消息所属主题名称", required = true)
            String topic,
            @McpToolParam(description = "业务索引键 (Message Key)", required = true)
            String key,
            @McpToolParam(description = "起始时间戳毫秒值（可选，默认当前时间前 3 天）", required = false)
            Long beginTimestamp,
            @McpToolParam(description = "结束时间戳毫秒值（可选，默认当前时间）", required = false)
            Long endTimestamp,
            @McpToolParam(description = "最多返回消息条数（可选，默认 32，上限 64）", required = false)
            Integer maxNum) throws Exception {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Parameter 'topic' must not be blank");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Parameter 'key' must not be blank");
        }
        log.info("Executing MCP Tool: rocketmq_query_message_by_key with topic: {}, key: {}", topic.trim(), key.trim());
        return adminClientService.queryMessageByKey(topic.trim(), key.trim(), beginTimestamp, endTimestamp, maxNum);
    }

    /**
     * 按消费组查询死信队列（DLQ，%DLQ%Group）中的积压消息列表。
     *
     * @param consumerGroup 发生消费失败与死信的消费组名称
     * @param beginTimestamp 起始时间戳（毫秒）
     * @param endTimestamp 结束时间戳（毫秒）
     * @param maxNum 最多返回死信条数
     * @return 死信消息列表 DTO
     * @throws Exception 当查询异常或入参无效时抛出
     */
    @McpTool(
            name = "rocketmq_query_dlq_messages",
            description = "按消费组查询死信队列（DLQ，%DLQ%Group）中的积压消息列表。"
    )
    public DlqMessageListDTO queryDlqMessages(
            @McpToolParam(description = "发生消费失败与死信的消费组名称", required = true)
            String consumerGroup,
            @McpToolParam(description = "起始时间戳毫秒值（可选，默认当前时间前 3 天）", required = false)
            Long beginTimestamp,
            @McpToolParam(description = "结束时间戳毫秒值（可选，默认当前时间）", required = false)
            Long endTimestamp,
            @McpToolParam(description = "最多返回死信消息条数（可选，默认 32，上限 64）", required = false)
            Integer maxNum) throws Exception {
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("Parameter 'consumerGroup' must not be blank");
        }
        log.info("Executing MCP Tool: rocketmq_query_dlq_messages with consumerGroup: {}", consumerGroup.trim());
        return adminClientService.queryDlqMessages(consumerGroup.trim(), beginTimestamp, endTimestamp, maxNum);
    }

    /**
     * 按消息 ID 查询消息生命周期轨迹，包括发送、存储与消费状态。
     *
     * @param msgId 待查询轨迹的消息 ID
     * @param topic 消息所属主题（可选）
     * @return 消息轨迹 DTO
     * @throws Exception 当查询异常或入参无效时抛出
     */
    @McpTool(
            name = "rocketmq_query_message_trace",
            description = "按消息 ID 查询消息生命周期轨迹，包括发送、存储与消费状态。"
    )
    public MessageTraceDTO queryMessageTrace(
            @McpToolParam(description = "待查询轨迹的消息 ID", required = true)
            String msgId,
            @McpToolParam(description = "消息所属主题（可选）", required = false)
            String topic) throws Exception {
        if (msgId == null || msgId.isBlank()) {
            throw new IllegalArgumentException("Parameter 'msgId' must not be blank");
        }
        String cleanTopic = (topic != null && !topic.isBlank()) ? topic.trim() : null;
        log.info("Executing MCP Tool: rocketmq_query_message_trace with msgId: {}, topic: {}", msgId.trim(), cleanTopic);
        return adminClientService.queryMessageTrace(msgId.trim(), cleanTopic);
    }

    /**
     * 将死信队列中的死信消息重新投递回业务目标主题（高危破坏性操作，受双层防呆保护）。
     *
     * @param consumerGroup 所属消费组名称（必填）
     * @param msgId 待重投的死信消息 ID（必填）
     * @param targetTopic 目标业务主题名称（可选，若为空则自动解析原真实主题）
     * @param confirm 破坏性操作显式确认参数，必须传 true 方可执行
     * @return 格式化后的 ResendDlqResultDTO 回执对象
     * @throws Exception 当底层检索或重新投递失败或防呆拦截时抛出
     */
    @McpTool(
            name = "rocketmq_resend_dlq_message",
            description = "将死信队列中的死信消息重新投递回业务目标主题（高危破坏性操作，受双层防呆保护）。"
    )
    public ResendDlqResultDTO resendDlqMessage(
            @McpToolParam(description = "所属消费组名称", required = true)
            String consumerGroup,
            @McpToolParam(description = "待重投的死信消息 ID", required = true)
            String msgId,
            @McpToolParam(description = "目标业务主题名称（可选，若为空则自动解析原真实主题）", required = false)
            String targetTopic,
            @McpToolParam(description = "破坏性操作显式确认参数，必须传 true 方可执行", required = true)
            Boolean confirm) throws Exception {
        dualLayerGuard.checkDestructiveOperation("rocketmq_resend_dlq_message", confirm);
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("Parameter 'consumerGroup' must not be blank");
        }
        if (msgId == null || msgId.isBlank()) {
            throw new IllegalArgumentException("Parameter 'msgId' must not be blank");
        }
        String cleanTargetTopic = (targetTopic != null && !targetTopic.isBlank()) ? targetTopic.trim() : null;
        log.info("Executing MCP Tool: rocketmq_resend_dlq_message for group: {}, msgId: {}, targetTopic: {}",
                consumerGroup.trim(), msgId.trim(), cleanTargetTopic);
        return adminClientService.resendDlqMessage(consumerGroup.trim(), msgId.trim(), cleanTargetTopic);
    }
}
