package io.github.atengk.mcp.rocketmq.mcp.tool;

import io.github.atengk.mcp.rocketmq.rocketmq.admin.AdminClientService;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerConnectionDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerGroupListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerLagDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ResetOffsetResultDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopConsumerLagDTO;
import io.github.atengk.mcp.rocketmq.security.DualLayerGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消费组全景审计、连接状态、实时积压排行与消费位点重置 MCP 工具集。
 * 提供活跃消费组发现、在线实例连接查验、分片队列精准 Lag 测算、集群 TopN 积压排行榜与位点回溯重置能力。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class ConsumerTools {

    private static final Logger log = LoggerFactory.getLogger(ConsumerTools.class);

    private final AdminClientService adminClientService;
    private final DualLayerGuard dualLayerGuard;

    public ConsumerTools(AdminClientService adminClientService, DualLayerGuard dualLayerGuard) {
        this.adminClientService = adminClientService;
        this.dualLayerGuard = dualLayerGuard;
    }

    /**
     * 查询 RocketMQ 集群中的所有消费组清单，默认自动过滤系统内部消费组。
     *
     * @param includeSystemGroups 是否包含系统内置消费组，默认为 false
     * @return 格式化后的消费组清单与总量统计
     * @throws Exception 当底层通信或查询异常时抛出
     */
    @McpTool(
            name = "rocketmq_list_consumer_groups",
            description = "查询 RocketMQ 集群中的所有消费组清单，默认自动过滤系统内部消费组（如 TOOLS_CONSUMER、%SYS% 等）。"
    )
    public ConsumerGroupListDTO listConsumerGroups(
            @McpToolParam(description = "是否包含系统内置消费组，默认为 false", required = false)
            Boolean includeSystemGroups) throws Exception {
        boolean includeSystem = Boolean.TRUE.equals(includeSystemGroups);
        log.info("Executing MCP Tool: rocketmq_list_consumer_groups with includeSystem: {}", includeSystem);
        return adminClientService.listConsumerGroups(includeSystem);
    }

    /**
     * 查询指定消费组的在线客户端连接分布、协议版本与订阅表达式健康度。
     *
     * @param consumerGroup 目标消费组名称
     * @return 格式化后的 ConsumerConnectionDTO 实例
     * @throws Exception 当底层通信或查询异常时抛出
     */
    @McpTool(
            name = "rocketmq_consumer_status",
            description = "查询指定 RocketMQ 消费组的在线实例分布、网络连接、消费模式与订阅关系。"
    )
    public ConsumerConnectionDTO getConsumerStatus(
            @McpToolParam(description = "目标消费组名称", required = true)
            String consumerGroup) throws Exception {
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("Consumer group name must not be blank");
        }
        log.info("Executing MCP Tool: rocketmq_consumer_status with group: {}", consumerGroup);
        return adminClientService.getConsumerStatus(consumerGroup.trim());
    }

    /**
     * 查询指定消费组在各分片队列上的最大点位、消费位点与未消费积压差值 (Lag)。
     *
     * @param consumerGroup 目标消费组名称
     * @param topic 指定过滤的主题名称（可选，若不指定则统计所有主题）
     * @return 格式化后的 ConsumerLagDTO 实例
     * @throws Exception 当底层通信或计算异常时抛出
     */
    @McpTool(
            name = "rocketmq_consumer_lag",
            description = "精确计算并展示指定 RocketMQ 消费组各分片队列的最大位点、消费位点与未消费积压差值 (Lag) 以及实时 TPS。"
    )
    public ConsumerLagDTO getConsumerLag(
            @McpToolParam(description = "目标消费组名称", required = true)
            String consumerGroup,
            @McpToolParam(description = "指定过滤的主题名称（可选，若不指定则统计该消费组的所有主题）", required = false)
            String topic) throws Exception {
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("Consumer group name must not be blank");
        }
        String cleanTopic = (topic != null && !topic.isBlank()) ? topic.trim() : null;
        log.info("Executing MCP Tool: rocketmq_consumer_lag with group: {}, topic: {}", consumerGroup, cleanTopic);
        return adminClientService.getConsumerLag(consumerGroup.trim(), cleanTopic);
    }

    /**
     * 按全集群消费组总未消费积压量降序排列并截取 TopN 排行榜。
     *
     * @param topN 截取数量，小于等于 0 或不传时默认为 10
     * @return 格式化后的 TopConsumerLagDTO 实例
     * @throws Exception 当底层通信或计算异常时抛出
     */
    @McpTool(
            name = "rocketmq_top_consumer_lag",
            description = "按全集群业务消费组总积压消息量降序排列，秒级锁定堆积量最大的 TopN 消费组与对应消费速率。"
    )
    public TopConsumerLagDTO getTopConsumerLag(
            @McpToolParam(description = "截取的最大数量（TopN），默认为 10", required = false)
            Integer topN) throws Exception {
        int limit = (topN != null && topN > 0) ? topN : 10;
        log.info("Executing MCP Tool: rocketmq_top_consumer_lag with topN: {}", limit);
        return adminClientService.getTopConsumerLag(limit);
    }

    /**
     * 重置指定消费组在目标主题上的消费位点（支持按时间戳回溯或跳过积压至最大位点，高危破坏性操作，受双层防呆保护）。
     *
     * @param consumerGroup 目标消费组名称（必填）
     * @param topic 目标主题名称（必填）
     * @param timestamp 目标回溯时间戳（毫秒，按时间戳模式必填）
     * @param resetToMax 是否跳过积压直接重置到最大位点（默认 false）
     * @param confirm 破坏性操作显式确认参数，必须传 true 方可执行
     * @return 格式化后的 ResetOffsetResultDTO 回执对象
     * @throws Exception 当底层重置位点失败或防呆拦截时抛出
     */
    @McpTool(
            name = "rocketmq_reset_consumer_offset",
            description = "重置指定消费组的消费位点（支持按时间戳回溯或跳过积压至最大位点，高危破坏性操作，受双层防呆保护）。"
    )
    public ResetOffsetResultDTO resetConsumerOffset(
            @McpToolParam(description = "目标消费组名称", required = true)
            String consumerGroup,
            @McpToolParam(description = "目标主题名称", required = true)
            String topic,
            @McpToolParam(description = "目标回溯时间戳（毫秒，按时间戳模式必填）", required = false)
            Long timestamp,
            @McpToolParam(description = "是否跳过积压直接重置到最大位点（默认 false）", required = false)
            Boolean resetToMax,
            @McpToolParam(description = "破坏性操作显式确认参数，必须传 true 方可执行", required = true)
            Boolean confirm) throws Exception {
        dualLayerGuard.checkDestructiveOperation("rocketmq_reset_consumer_offset", confirm);
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("Parameter 'consumerGroup' must not be blank");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Parameter 'topic' must not be blank");
        }
        boolean toMax = Boolean.TRUE.equals(resetToMax);
        if (!toMax && (timestamp == null || timestamp <= 0)) {
            throw new IllegalArgumentException("Parameter 'timestamp' must be greater than 0 when 'resetToMax' is false");
        }
        log.info("Executing MCP Tool: rocketmq_reset_consumer_offset for group: {}, topic: {}, timestamp: {}, resetToMax: {}",
                consumerGroup.trim(), topic.trim(), timestamp, toMax);
        return adminClientService.resetOffset(consumerGroup.trim(), topic.trim(), timestamp, toMax);
    }
}
