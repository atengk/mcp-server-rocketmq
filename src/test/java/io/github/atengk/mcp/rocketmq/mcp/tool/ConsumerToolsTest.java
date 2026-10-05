package io.github.atengk.mcp.rocketmq.mcp.tool;

import io.github.atengk.mcp.rocketmq.rocketmq.admin.AdminClientService;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerClientDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerConnectionDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerGroupListDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerLagDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerLagSummaryDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ConsumerQueueLagDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.ResetOffsetResultDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.SubscriptionDTO;
import io.github.atengk.mcp.rocketmq.rocketmq.admin.dto.TopConsumerLagDTO;
import io.github.atengk.mcp.rocketmq.security.DestructiveOperationBlockedException;
import io.github.atengk.mcp.rocketmq.security.DualLayerGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消费组 MCP 工具单元测试。
 * 验证消费组列表、连接状态、队列 Lag 测算、TopN 积压排行与位点重置的工具调用与入参校验。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsumerTools MCP 工具单元测试")
class ConsumerToolsTest {

    @Mock
    private AdminClientService adminClientService;

    @Mock
    private DualLayerGuard dualLayerGuard;

    @InjectMocks
    private ConsumerTools consumerTools;

    @Test
    @DisplayName("验证 rocketmq_list_consumer_groups 默认过滤系统消费组")
    void shouldListConsumerGroupsWithDefaultFilter() throws Exception {
        ConsumerGroupListDTO mockDto = new ConsumerGroupListDTO(List.of("order_group", "pay_group"));
        when(adminClientService.listConsumerGroups(false)).thenReturn(mockDto);

        ConsumerGroupListDTO result = consumerTools.listConsumerGroups(null);

        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getGroups()).containsExactly("order_group", "pay_group");
        verify(adminClientService).listConsumerGroups(false);
    }

    @Test
    @DisplayName("验证 rocketmq_list_consumer_groups 指定包含系统消费组")
    void shouldListConsumerGroupsIncludingSystem() throws Exception {
        ConsumerGroupListDTO mockDto = new ConsumerGroupListDTO(List.of("order_group", "TOOLS_CONSUMER"));
        when(adminClientService.listConsumerGroups(true)).thenReturn(mockDto);

        ConsumerGroupListDTO result = consumerTools.listConsumerGroups(true);

        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getGroups()).containsExactly("order_group", "TOOLS_CONSUMER");
        verify(adminClientService).listConsumerGroups(true);
    }

    @Test
    @DisplayName("验证 rocketmq_consumer_status 工具调用返回客户端连接与订阅信息")
    void shouldReturnConsumerStatusSuccessfully() throws Exception {
        ConsumerConnectionDTO mockDto = new ConsumerConnectionDTO("order_group");
        mockDto.setClients(List.of(new ConsumerClientDTO("client-1", "192.168.1.100:54321", "JAVA", 440)));
        mockDto.setSubscriptions(List.of(new SubscriptionDTO("OrderTopic", "*", Set.of("TagA"))));
        mockDto.setConsumeType("CONSUME_PASSIVELY");
        mockDto.setMessageModel("CLUSTERING");
        when(adminClientService.getConsumerStatus("order_group")).thenReturn(mockDto);

        ConsumerConnectionDTO result = consumerTools.getConsumerStatus("order_group");

        assertThat(result).isNotNull();
        assertThat(result.getConsumerGroup()).isEqualTo("order_group");
        assertThat(result.isOnline()).isTrue();
        assertThat(result.getClients()).hasSize(1);
        assertThat(result.getClients().getFirst().getClientId()).isEqualTo("client-1");
        assertThat(result.getSubscriptions()).hasSize(1);
        assertThat(result.getSubscriptions().getFirst().getTopic()).isEqualTo("OrderTopic");
    }

    @Test
    @DisplayName("验证 rocketmq_consumer_status 传入空消费组名称时抛出异常")
    void shouldThrowExceptionWhenConsumerStatusGroupIsBlank() {
        assertThatThrownBy(() -> consumerTools.getConsumerStatus("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Consumer group name must not be blank");
    }

    @Test
    @DisplayName("验证 rocketmq_consumer_lag 工具调用返回队列位点积压明细")
    void shouldReturnConsumerLagSuccessfully() throws Exception {
        ConsumerLagDTO mockDto = new ConsumerLagDTO("order_group");
        mockDto.setTotalLag(500L);
        mockDto.setConsumeTps(120.5);
        ConsumerQueueLagDTO queueLag = new ConsumerQueueLagDTO("OrderTopic", "broker-a", 0, 1000L, 500L, 1728000000000L);
        mockDto.setQueues(List.of(queueLag));
        when(adminClientService.getConsumerLag("order_group", "OrderTopic")).thenReturn(mockDto);

        ConsumerLagDTO result = consumerTools.getConsumerLag("order_group", "OrderTopic");

        assertThat(result).isNotNull();
        assertThat(result.getConsumerGroup()).isEqualTo("order_group");
        assertThat(result.getTotalLag()).isEqualTo(500L);
        assertThat(result.getConsumeTps()).isEqualTo(120.5);
        assertThat(result.getQueues()).hasSize(1);
        assertThat(result.getQueues().getFirst().getLag()).isEqualTo(500L);
    }

    @Test
    @DisplayName("验证 rocketmq_consumer_lag 传入空消费组名称时抛出异常")
    void shouldThrowExceptionWhenConsumerLagGroupIsBlank() {
        assertThatThrownBy(() -> consumerTools.getConsumerLag(null, "OrderTopic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Consumer group name must not be blank");
    }

    @Test
    @DisplayName("验证 rocketmq_top_consumer_lag 默认截取 Top 10")
    void shouldReturnTopConsumerLagWithDefaultTopN() throws Exception {
        ConsumerLagSummaryDTO top1 = new ConsumerLagSummaryDTO("group-a", 10000L, 50.0);
        ConsumerLagSummaryDTO top2 = new ConsumerLagSummaryDTO("group-b", 500L, 10.0);
        TopConsumerLagDTO mockDto = new TopConsumerLagDTO(2, List.of(top1, top2));
        when(adminClientService.getTopConsumerLag(10)).thenReturn(mockDto);

        TopConsumerLagDTO result = consumerTools.getTopConsumerLag(null);

        assertThat(result).isNotNull();
        assertThat(result.getTotalEvaluatedGroups()).isEqualTo(2);
        assertThat(result.getTopLags()).hasSize(2);
        assertThat(result.getTopLags().getFirst().getConsumerGroup()).isEqualTo("group-a");
        assertThat(result.getTopLags().getFirst().getTotalLag()).isEqualTo(10000L);
        assertThat(result.getTopLags().getFirst().hasPendingMessages()).isTrue();
        verify(adminClientService).getTopConsumerLag(10);
    }

    @Test
    @DisplayName("验证 rocketmq_top_consumer_lag 传入指定 topN 时正确传递")
    void shouldReturnTopConsumerLagWithCustomTopN() throws Exception {
        TopConsumerLagDTO mockDto = new TopConsumerLagDTO(5, List.of());
        when(adminClientService.getTopConsumerLag(3)).thenReturn(mockDto);

        TopConsumerLagDTO result = consumerTools.getTopConsumerLag(3);

        assertThat(result).isNotNull();
        verify(adminClientService).getTopConsumerLag(3);
    }

    @Test
    @DisplayName("验证 rocketmq_reset_consumer_offset 正常执行时间戳与最大位点模式重置")
    void shouldResetConsumerOffsetSuccessfully() throws Exception {
        doNothing().when(dualLayerGuard).checkDestructiveOperation("rocketmq_reset_consumer_offset", true);
        ResetOffsetResultDTO mockResult = new ResetOffsetResultDTO("group-a", "OrderTopic", "TIMESTAMP", 1700000000000L, "SUCCESS", "ok");
        when(adminClientService.resetOffset("group-a", "OrderTopic", 1700000000000L, false)).thenReturn(mockResult);

        ResetOffsetResultDTO result = consumerTools.resetConsumerOffset("group-a", "OrderTopic", 1700000000000L, false, true);

        assertThat(result).isNotNull();
        assertThat(result.getConsumerGroup()).isEqualTo("group-a");
        assertThat(result.getResetMode()).isEqualTo("TIMESTAMP");
        verify(dualLayerGuard).checkDestructiveOperation("rocketmq_reset_consumer_offset", true);
        verify(adminClientService).resetOffset("group-a", "OrderTopic", 1700000000000L, false);

        // 最大位点跳过积压模式
        ResetOffsetResultDTO maxResultMock = new ResetOffsetResultDTO("group-a", "OrderTopic", "MAX_OFFSET", 1790000000000L, "SUCCESS", "ok");
        when(adminClientService.resetOffset("group-a", "OrderTopic", null, true)).thenReturn(maxResultMock);

        ResetOffsetResultDTO maxResult = consumerTools.resetConsumerOffset("group-a", "OrderTopic", null, true, true);
        assertThat(maxResult).isNotNull();
        assertThat(maxResult.getResetMode()).isEqualTo("MAX_OFFSET");
        verify(adminClientService).resetOffset("group-a", "OrderTopic", null, true);
    }

    @Test
    @DisplayName("验证 rocketmq_reset_consumer_offset 在防呆守卫拒绝时短路拦截")
    void shouldBlockResetConsumerOffsetWhenDualLayerGuardRejects() throws Exception {
        doThrow(new DestructiveOperationBlockedException("rocketmq_reset_consumer_offset", "blocked"))
                .when(dualLayerGuard).checkDestructiveOperation("rocketmq_reset_consumer_offset", false);

        assertThatThrownBy(() -> consumerTools.resetConsumerOffset("group-a", "OrderTopic", null, true, false))
                .isInstanceOf(DestructiveOperationBlockedException.class);

        verify(adminClientService, never()).resetOffset(any(), any(), any(), any());
    }

    @Test
    @DisplayName("验证 rocketmq_reset_consumer_offset 入参非法时前置抛出 IllegalArgumentException")
    void shouldThrowExceptionWhenResetOffsetParamsAreInvalid() {
        doNothing().when(dualLayerGuard).checkDestructiveOperation("rocketmq_reset_consumer_offset", true);

        assertThatThrownBy(() -> consumerTools.resetConsumerOffset("   ", "OrderTopic", 1000L, false, true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> consumerTools.resetConsumerOffset("group-a", "   ", 1000L, false, true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> consumerTools.resetConsumerOffset("group-a", "OrderTopic", null, false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'timestamp' must be greater than 0");
    }
}
