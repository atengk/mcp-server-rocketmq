package io.github.atengk.mcp.rocketmq.mcp.tool;

import io.github.atengk.mcp.rocketmq.rocketmq.admin.AdminClientService;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicRouteDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicStatusDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopicOperationResultDTO;
import io.github.atengk.mcp.rocketmq.security.DualLayerGuard;
import io.github.atengk.mcp.rocketmq.security.ReadOnlyGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 主题元数据、队列路由、位点状态与主题管控 MCP 工具集。
 * 提供主题发现、读写队列分布拓扑、以及声明式主题创建与破坏性主题删除控制能力。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class TopicTools {

    private static final Logger log = LoggerFactory.getLogger(TopicTools.class);

    private final AdminClientService adminClientService;
    private final ReadOnlyGuard readOnlyGuard;
    private final DualLayerGuard dualLayerGuard;

    public TopicTools(AdminClientService adminClientService, ReadOnlyGuard readOnlyGuard, DualLayerGuard dualLayerGuard) {
        this.adminClientService = adminClientService;
        this.readOnlyGuard = readOnlyGuard;
        this.dualLayerGuard = dualLayerGuard;
    }

    /**
     * 查询 RocketMQ 集群中的所有主题列表，默认自动过滤系统内部主题。
     *
     * @param includeSystemTopics 是否包含系统内置主题（如 %SYS%、TBW102 等），默认为 false
     * @return 格式化后的主题列表与统计对象
     * @throws Exception 当底层通信或查询异常时抛出
     */
    @McpTool(
            name = "rocketmq_list_topics",
            description = "查询 RocketMQ 集群中的主题列表，默认自动过滤系统内置主题（如 %SYS%、TBW102 等）。"
    )
    public TopicListDTO listTopics(
            @McpToolParam(description = "是否包含系统内部保留主题，默认为 false", required = false)
            Boolean includeSystemTopics) throws Exception {
        boolean includeSystem = Boolean.TRUE.equals(includeSystemTopics);
        log.info("Executing MCP Tool: rocketmq_list_topics with includeSystem: {}", includeSystem);
        return adminClientService.listTopics(includeSystem);
    }

    /**
     * 查询指定主题的读写队列分布与 Broker 路由详情。
     *
     * @param topic 目标主题名称
     * @return 格式化后的 TopicRouteDTO 路由详情对象
     * @throws Exception 当底层通信或路由查询异常时抛出
     */
    @McpTool(
            name = "rocketmq_topic_route",
            description = "查询指定 RocketMQ 主题的读写队列分布、Broker 物理路由拓扑与权限配置。"
    )
    public TopicRouteDTO getTopicRoute(
            @McpToolParam(description = "目标主题名称", required = true)
            String topic) throws Exception {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic name must not be blank");
        }
        log.info("Executing MCP Tool: rocketmq_topic_route with topic: {}", topic);
        return adminClientService.getTopicRoute(topic.trim());
    }

    /**
     * 查询指定主题各分片队列的位点统计与消息留存容量。
     *
     * @param topic 目标主题名称
     * @return 格式化后的 TopicStatusDTO 位点状态与容量统计对象
     * @throws Exception 当底层通信或位点计算异常时抛出
     */
    @McpTool(
            name = "rocketmq_topic_status",
            description = "查询指定 RocketMQ 主题各分片队列的最小/最大位点与消息留存总量统计。"
    )
    public TopicStatusDTO getTopicStatus(
            @McpToolParam(description = "目标主题名称", required = true)
            String topic) throws Exception {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic name must not be blank");
        }
        log.info("Executing MCP Tool: rocketmq_topic_status with topic: {}", topic);
        return adminClientService.getTopicStatus(topic.trim());
    }

    /**
     * 声明式创建或修改 RocketMQ 业务主题，支持指定读写队列数与读写权限。
     * 受全局只读守卫保护。
     *
     * @param topic 目标主题名称（必填）
     * @param readQueueNums 读队列数量（可选，默认为 8）
     * @param writeQueueNums 写队列数量（可选，默认为 8）
     * @param perm 权限模式（可选，默认 6 即读写）
     * @return 格式化后的 TopicOperationResultDTO 操作回执对象
     * @throws Exception 当底层创建失败时抛出
     */
    @McpTool(
            name = "rocketmq_create_topic",
            description = "声明式创建或修改 RocketMQ 业务主题，支持指定读写队列数与读写权限。"
    )
    public TopicOperationResultDTO createTopic(
            @McpToolParam(description = "目标主题名称", required = true)
            String topic,
            @McpToolParam(description = "读队列数量（可选，默认为 8）", required = false)
            Integer readQueueNums,
            @McpToolParam(description = "写队列数量（可选，默认为 8）", required = false)
            Integer writeQueueNums,
            @McpToolParam(description = "权限模式（可选，默认 6 即读写）", required = false)
            Integer perm) throws Exception {
        readOnlyGuard.checkWritable("rocketmq_create_topic");
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Parameter 'topic' must not be blank");
        }
        log.info("Executing MCP Tool: rocketmq_create_topic for topic: {}", topic.trim());
        return adminClientService.createTopic(topic.trim(), readQueueNums, writeQueueNums, perm);
    }

    /**
     * 删除指定的 RocketMQ 业务主题（高危破坏性操作，受双层防呆保护）。
     *
     * @param topic 待删除的目标主题名称（必填）
     * @param confirm 破坏性操作显式确认参数，必须显式传入 true 方可执行
     * @return 格式化后的 TopicOperationResultDTO 操作回执对象
     * @throws Exception 当底层删除失败或防呆拦截时抛出
     */
    @McpTool(
            name = "rocketmq_delete_topic",
            description = "删除指定的 RocketMQ 业务主题（高危破坏性操作，受双层防呆保护）。"
    )
    public TopicOperationResultDTO deleteTopic(
            @McpToolParam(description = "待删除的目标主题名称", required = true)
            String topic,
            @McpToolParam(description = "破坏性操作显式确认参数，必须传 true 方可执行", required = true)
            Boolean confirm) throws Exception {
        dualLayerGuard.checkDestructiveOperation("rocketmq_delete_topic", confirm);
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Parameter 'topic' must not be blank");
        }
        log.info("Executing MCP Tool: rocketmq_delete_topic for topic: {}", topic.trim());
        return adminClientService.deleteTopic(topic.trim());
    }
}
