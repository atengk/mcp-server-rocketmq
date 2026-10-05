package io.github.atengk.mcp.rocketmq.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RocketMQ 专家诊断与健康巡检 Prompts 工作流暴露组件。
 * 遵循 Model Context Protocol 规范，向 AI 智能体暴露消费积压深度排障与集群巡检专家工作流。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class ExpertPrompts {

    private static final Logger log = LoggerFactory.getLogger(ExpertPrompts.class);

    private static final int DEFAULT_TOP_N = 5;

    /**
     * 消费堆积深度排障专家工作流提示词模版。
     * 自动串联积压详情查验、慢客户端/连接审计、死信队列扫描与处置策略建议。
     *
     * @param consumerGroup 待诊断的目标消费组名称（必填）
     * @param topic         待诊断的目标主题名称（可选）
     * @return 包含分步排障引导指令的 MCP Prompt 响应
     */
    @McpPrompt(
            name = "diagnose_consumer_lag",
            description = "RocketMQ 消费堆积深度排障专家工作流，自动串联积压分片定位、客户端连接审计、死信毒丸分析并输出处置策略。"
    )
    public McpSchema.GetPromptResult diagnoseConsumerLag(
            @McpArg(name = "consumerGroup", description = "待诊断的目标消费组名称", required = true) String consumerGroup,
            @McpArg(name = "topic", description = "待诊断的目标业务主题名称（可选）", required = false) String topic
    ) {
        log.info("Generating MCP Prompt: diagnose_consumer_lag for group: {}, topic: {}", consumerGroup, topic);

        // 1. 前置卫语句判空防御
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("Parameter 'consumerGroup' must not be blank");
        }

        String targetGroup = consumerGroup.trim();
        String targetTopicDesc = (topic != null && !topic.isBlank()) ? topic.trim() : "所有订阅主题";

        // 2. 组装多步深度排障引导工作流
        StringBuilder sb = new StringBuilder();
        sb.append("# 🚨 RocketMQ 消费积压深度排障专家工作流\n\n");
        sb.append("你是一名具备十年经验的 Apache RocketMQ 核心运维与架构专家。当前目标消费组 **`")
                .append(targetGroup).append("`**（关联主题：**`").append(targetTopicDesc)
                .append("`**）报告了消费积压异常。\n\n");
        sb.append("请严格按照以下标准化排障四步法，依次调用 MCP 工具开展闭环诊断并给出最终调优建议：\n\n");

        sb.append("## 步骤 1：精确查验分片积压分布 (Queue Lag)\n");
        sb.append("- 请调用工具 `rocketmq_consumer_lag(consumerGroup=\"").append(targetGroup).append("\"");
        if (topic != null && !topic.isBlank()) {
            sb.append(", topic=\"").append(topic.trim()).append("\"");
        }
        sb.append(")`；\n");
        sb.append("- 重点分析：各分片队列（Queue）的 `maxOffset` 与 `consumerOffset` 差值；\n");
        sb.append("- 识别是否存在严重单队列倾斜（如单 Broker 或单 Queue 积压达 90% 以上）。\n\n");

        sb.append("## 步骤 2：审计消费者客户端在线与连接状态 (Consumer Status)\n");
        sb.append("- 请调用工具 `rocketmq_consumer_status(consumerGroup=\"").append(targetGroup).append("\")`；\n");
        sb.append("- 检查客户端列表（`clients`）：\n");
        sb.append("  - 若列表为空：直接断定消费者处于**完全离线状态**；\n");
        sb.append("  - 若列表非空：核对客户端语言、版本及实例数量是否与队列总数匹配，是否存在分配死锁。\n\n");

        sb.append("## 步骤 3：扫描死信队列毒丸消息 (Dead Letter Queue)\n");
        sb.append("- 请调用工具 `rocketmq_query_dlq_messages(consumerGroup=\"").append(targetGroup).append("\")`；\n");
        sb.append("- 查验是否存在由于下游抛异常重试耗尽而进入死信队列（`%DLQ%").append(targetGroup).append("`）的消息；\n");
        sb.append("- 若存在死信，抽取单条关键死信的消息体与异常堆栈，分析业务消费失败根因。\n\n");

        sb.append("## 步骤 4：综合调优与处置策略输出\n");
        sb.append("请根据前三步工具执行结果，输出一份结构化 Markdown 排障报告，必须包含：\n");
        sb.append("1. **故障诊断定级**：【轻微堆积 / 严重阻塞 / 消费者离线 / 毒丸死信】；\n");
        sb.append("2. **核心指标摘要**：总积压量、最慢分片队列、在线实例数、死信积压数；\n");
        sb.append("3. **根因归纳分析**：下游耗时长、并发度不足、代码死锁或下游依赖挂起；\n");
        sb.append("4. **自愈与应急建议**：\n");
        sb.append("   - 若业务需要紧急跳过积压，提示在确认数据可丢弃时调用 `rocketmq_reset_consumer_offset(resetToMax=true)`；\n");
        sb.append("   - 若需要重投死信自愈，提示调用 `rocketmq_resend_dlq_message`。\n");

        McpSchema.PromptMessage promptMessage = McpSchema.PromptMessage.builder(
                McpSchema.Role.USER,
                McpSchema.TextContent.builder(sb.toString()).build()
        ).build();

        return McpSchema.GetPromptResult.builder(List.of(promptMessage))
                .description("消费积压深度排障工作流：" + targetGroup)
                .build();
    }

    /**
     * 集群全面健康体检巡检专家工作流提示词模版。
     * 自动串联 Broker 负载感知、主题路由均衡度体检、全集群积压 TopN 风险识别并输出巡检周报。
     *
     * @param topN 重点巡检的积压消费组数量（可选，默认 5）
     * @return 包含分步巡检引导指令的 MCP Prompt 响应
     */
    @McpPrompt(
            name = "cluster_health_check",
            description = "RocketMQ 集群全面健康体检专家工作流，覆盖 Broker 负载、主题队列分布、积压排行并输出 Markdown 巡检报告。"
    )
    public McpSchema.GetPromptResult clusterHealthCheck(
            @McpArg(name = "topN", description = "重点巡检的积压消费组排行数量（默认 5）", required = false) Integer topN
    ) {
        int evaluatedTopN = (topN != null && topN > 0) ? topN : DEFAULT_TOP_N;
        log.info("Generating MCP Prompt: cluster_health_check for topN: {}", evaluatedTopN);

        // 1. 组装集群巡检指引工作流
        StringBuilder sb = new StringBuilder();
        sb.append("# 🩺 RocketMQ 全局健康体检与巡检周报工作流\n\n");
        sb.append("你是一名经验丰富的 Apache RocketMQ 架构保障专家。请通过 MCP 工具链对当前集群开展全方位健康巡检并输出规范周报。\n\n");
        sb.append("请按以下四大检查维度逐步执行并汇总：\n\n");

        sb.append("## 维度 1：物理集群拓扑与 Broker 节点健康感知\n");
        sb.append("- 请调用工具 `rocketmq_cluster_info`（或直接查阅只读资源 `rocketmq://cluster/topology`）；\n");
        sb.append("- 查验 Broker 节点总数、Master/Slave 配对健全度及地址列表；\n");
        sb.append("- 针对各个 Master 节点，调用 `rocketmq_broker_stats(brokerName)`，重点提取：\n");
        sb.append("  - `putTps` 与 `getTransferedTps` 吞吐负荷；\n");
        sb.append("  - `commitLogDiskRatio` 磁盘水位（预警阈值：>= 75% 需重点标红）；\n");
        sb.append("  - 节点系统写入耗时与消息落盘延迟。\n\n");

        sb.append("## 维度 2：业务主题覆盖度与队列均衡度感知\n");
        sb.append("- 请调用工具 `rocketmq_list_topics`（或读取只读资源 `rocketmq://topics`），统计当前活跃业务主题清单；\n");
        sb.append("- 抽取核心高并发主题调用 `rocketmq_topic_route(topic)`，核验读写队列在各 Broker 上的分片分布是否对称均摊，是否存在单节点偏载。\n\n");

        sb.append("## 维度 3：集群级消费积压风险排查 (Top ").append(evaluatedTopN).append(")\n");
        sb.append("- 请调用工具 `rocketmq_top_consumer_lag(topN=").append(evaluatedTopN).append(")`；\n");
        sb.append("- 评估积压最多的 Top ").append(evaluatedTopN).append(" 消费组的总堆积量与最长阻塞队列；\n");
        sb.append("- 对总积压量大于 10,000 的消费组判定为【高危风险】，建议立即展开排查。\n\n");

        sb.append("## 维度 4：生成标准化巡检 Markdown 报告\n");
        sb.append("汇总上述采集数据，输出规范的巡检周报：\n");
        sb.append("```markdown\n");
        sb.append("# 📊 Apache RocketMQ 集群健康巡检报告\n");
        sb.append("- 巡检时间：YYYY-MM-DD HH:mm:ss\n");
        sb.append("- 集群总体健康评分：【优秀 (90+) / 良好 (80-89) / 警告 (60-79) / 危险 (<60)】\n\n");
        sb.append("### 1. 物理节点与负荷状态表\n");
        sb.append("| Broker 名称 | 角色 | IP地址 | 生产TPS | 消费TPS | 磁盘水位 | 状态 |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n\n");
        sb.append("### 2. 主题与路由架构评估\n");
        sb.append("- 业务主题总数、读写分片设计合理性点评\n\n");
        sb.append("### 3. Top ").append(evaluatedTopN).append(" 消费积压风险清单\n");
        sb.append("| 消费组名称 | 总积压量 | 最大积压主题 | 风险等级 | 处置建议 |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n\n");
        sb.append("### 4. 架构师优化与处置建议\n");
        sb.append("- 磁盘扩容、参数调优或客户端扩容指引\n");
        sb.append("```\n");

        // 2. 装配并返回 MCP Prompt 结果
        McpSchema.PromptMessage promptMessage = McpSchema.PromptMessage.builder(
                McpSchema.Role.USER,
                McpSchema.TextContent.builder(sb.toString()).build()
        ).build();

        return McpSchema.GetPromptResult.builder(List.of(promptMessage))
                .description("RocketMQ 全局健康体检与巡检周报工作流 (Top " + evaluatedTopN + ")")
                .build();
    }
}
