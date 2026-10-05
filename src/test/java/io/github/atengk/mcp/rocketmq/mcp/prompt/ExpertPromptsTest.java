package io.github.atengk.mcp.rocketmq.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 专家诊断工作流 Prompts 单元测试。
 * 验证消费堆积排障与集群巡检提示词模版的入参绑定与工作流编排内容生成。
 *
 * @author Ateng
 * @since 2026-10-04
 */
class ExpertPromptsTest {

    private ExpertPrompts expertPrompts;

    @BeforeEach
    void setUp() {
        expertPrompts = new ExpertPrompts();
    }

    @Test
    @DisplayName("验证 diagnoseConsumerLag 生成包含四步排障编排的结构化提示词")
    void shouldGenerateDiagnoseConsumerLagPrompt() {
        McpSchema.GetPromptResult result = expertPrompts.diagnoseConsumerLag("order_consumer_group", "OrderTopic");

        assertThat(result).isNotNull();
        assertThat(result.messages()).hasSize(1);
        McpSchema.PromptMessage msg = result.messages().getFirst();
        assertThat(msg.role()).isEqualTo(McpSchema.Role.USER);
        assertThat(msg.content()).isInstanceOf(McpSchema.TextContent.class);

        String text = ((McpSchema.TextContent) msg.content()).text();
        assertThat(text).contains("order_consumer_group");
        assertThat(text).contains("OrderTopic");
        assertThat(text).contains("rocketmq_consumer_lag");
        assertThat(text).contains("rocketmq_consumer_status");
        assertThat(text).contains("rocketmq_query_dlq_messages");
        assertThat(text).contains("rocketmq_reset_consumer_offset");
    }

    @Test
    @DisplayName("验证 diagnoseConsumerLag 当 topic 为空时生成缺省全主题诊断提示词")
    void shouldGenerateDiagnoseConsumerLagPromptWithoutTopic() {
        McpSchema.GetPromptResult result = expertPrompts.diagnoseConsumerLag("order_consumer_group", null);

        assertThat(result).isNotNull();
        McpSchema.PromptMessage msg = result.messages().getFirst();
        String text = ((McpSchema.TextContent) msg.content()).text();
        assertThat(text).contains("order_consumer_group");
        assertThat(text).contains("所有订阅主题");
    }

    @Test
    @DisplayName("验证 diagnoseConsumerLag 传入空白消费组时防御抛出异常")
    void shouldThrowExceptionWhenConsumerGroupIsBlank() {
        assertThatThrownBy(() -> expertPrompts.diagnoseConsumerLag("  ", "Topic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consumerGroup");
    }

    @Test
    @DisplayName("验证 clusterHealthCheck 生成包含集群、主题与积压巡检的完整周报提示词")
    void shouldGenerateClusterHealthCheckPrompt() {
        McpSchema.GetPromptResult result = expertPrompts.clusterHealthCheck(10);

        assertThat(result).isNotNull();
        assertThat(result.messages()).hasSize(1);
        McpSchema.PromptMessage msg = result.messages().getFirst();
        assertThat(msg.role()).isEqualTo(McpSchema.Role.USER);

        String text = ((McpSchema.TextContent) msg.content()).text();
        assertThat(text).contains("rocketmq_cluster_info");
        assertThat(text).contains("rocketmq_broker_stats");
        assertThat(text).contains("rocketmq_top_consumer_lag");
        assertThat(text).contains("Top 10");
        assertThat(text).contains("巡检 Markdown 报告");
    }

    @Test
    @DisplayName("验证 clusterHealthCheck 缺省 topN 时默认采用 Top 5")
    void shouldGenerateClusterHealthCheckPromptWithDefaultTopN() {
        McpSchema.GetPromptResult result = expertPrompts.clusterHealthCheck(null);

        assertThat(result).isNotNull();
        McpSchema.PromptMessage msg = result.messages().getFirst();
        String text = ((McpSchema.TextContent) msg.content()).text();
        assertThat(text).contains("Top 5");
    }
}
