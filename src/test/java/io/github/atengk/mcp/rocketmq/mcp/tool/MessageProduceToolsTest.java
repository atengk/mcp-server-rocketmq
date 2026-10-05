package io.github.atengk.mcp.rocketmq.mcp.tool;

import io.github.atengk.mcp.rocketmq.rocketmq.messaging.MessagingClientService;
import io.github.atengk.mcp.rocketmq.rocketmq.messaging.dto.SendMessageResultDTO;
import io.github.atengk.mcp.rocketmq.security.ReadOnlyException;
import io.github.atengk.mcp.rocketmq.security.ReadOnlyGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息发送 MCP 工具单元测试。
 * 验证 rocketmq_send_message 工具的普通/顺序/延时投递调用、参数前置拦截与全局只读守卫短路防御。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageProduceTools MCP 工具单元测试")
class MessageProduceToolsTest {

    @Mock
    private MessagingClientService messagingClientService;

    @Mock
    private ReadOnlyGuard readOnlyGuard;

    @InjectMocks
    private MessageProduceTools produceTools;

    @Test
    @DisplayName("验证普通测试消息发送成功返回确认结果")
    void shouldSendNormalMessageSuccessfully() throws Exception {
        doNothing().when(readOnlyGuard).checkWritable("rocketmq_send_message");
        SendMessageResultDTO mockResult = new SendMessageResultDTO(
                "01000000000000000000000001",
                "OrderTopic",
                "SUCCESS",
                "TagA",
                "KEY_01",
                null,
                null,
                16
        );
        when(messagingClientService.sendMessage(eq("OrderTopic"), eq("hello"), eq("TagA"), eq("KEY_01"), eq(null), eq(null)))
                .thenReturn(mockResult);

        SendMessageResultDTO result = produceTools.sendMessage("OrderTopic", "hello", "TagA", "KEY_01", null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isEqualTo("01000000000000000000000001");
        assertThat(result.getTopic()).isEqualTo("OrderTopic");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(readOnlyGuard).checkWritable("rocketmq_send_message");
        verify(messagingClientService).sendMessage("OrderTopic", "hello", "TagA", "KEY_01", null, null);
    }

    @Test
    @DisplayName("验证带分区组 messageGroup 的顺序消息发送")
    void shouldSendFifoOrderMessageSuccessfully() throws Exception {
        doNothing().when(readOnlyGuard).checkWritable("rocketmq_send_message");
        SendMessageResultDTO mockResult = new SendMessageResultDTO(
                "01000000000000000000000002",
                "OrderTopic",
                "SUCCESS",
                null,
                "ORDER_888",
                "GROUP_SHARD_1",
                null,
                32
        );
        when(messagingClientService.sendMessage(eq("OrderTopic"), eq("order payload"), eq(null), eq("ORDER_888"), eq("GROUP_SHARD_1"), eq(null)))
                .thenReturn(mockResult);

        SendMessageResultDTO result = produceTools.sendMessage("OrderTopic", "order payload", null, "ORDER_888", "GROUP_SHARD_1", null, null);

        assertThat(result).isNotNull();
        assertThat(result.getMessageGroup()).isEqualTo("GROUP_SHARD_1");
        verify(messagingClientService).sendMessage("OrderTopic", "order payload", null, "ORDER_888", "GROUP_SHARD_1", null);
    }

    @Test
    @DisplayName("验证指定 delaySeconds 的延时消息正确折算目标时间戳并发送")
    void shouldSendDelayMessageWithDelaySeconds() throws Exception {
        doNothing().when(readOnlyGuard).checkWritable("rocketmq_send_message");
        SendMessageResultDTO mockResult = new SendMessageResultDTO(
                "01000000000000000000000003",
                "OrderTopic",
                "SUCCESS"
        );
        ArgumentCaptor<Long> timestampCaptor = ArgumentCaptor.forClass(Long.class);
        when(messagingClientService.sendMessage(eq("OrderTopic"), eq("delay payload"), eq(null), eq(null), eq(null), timestampCaptor.capture()))
                .thenReturn(mockResult);

        long before = System.currentTimeMillis();
        SendMessageResultDTO result = produceTools.sendMessage("OrderTopic", "delay payload", null, null, null, 60, null);
        long after = System.currentTimeMillis();

        assertThat(result).isNotNull();
        Long capturedTimestamp = timestampCaptor.getValue();
        assertThat(capturedTimestamp).isNotNull();
        assertThat(capturedTimestamp).isGreaterThanOrEqualTo(before + 60_000L);
        assertThat(capturedTimestamp).isLessThanOrEqualTo(after + 60_000L);
    }

    @Test
    @DisplayName("验证直接指定 deliveryTimestamp 的定时消息发送")
    void shouldSendScheduledMessageWithDeliveryTimestamp() throws Exception {
        doNothing().when(readOnlyGuard).checkWritable("rocketmq_send_message");
        long targetTime = System.currentTimeMillis() + 100_000L;
        SendMessageResultDTO mockResult = new SendMessageResultDTO(
                "01000000000000000000000004",
                "OrderTopic",
                "SUCCESS"
        );
        when(messagingClientService.sendMessage("OrderTopic", "schedule payload", null, null, null, targetTime))
                .thenReturn(mockResult);

        SendMessageResultDTO result = produceTools.sendMessage("OrderTopic", "schedule payload", null, null, null, null, targetTime);

        assertThat(result).isNotNull();
        verify(messagingClientService).sendMessage("OrderTopic", "schedule payload", null, null, null, targetTime);
    }

    @Test
    @DisplayName("验证全局只读守卫拦截写操作并拒绝底层发送")
    void shouldBlockSendingWhenReadOnlyGuardRejects() throws Exception {
        doThrow(new ReadOnlyException("rocketmq_send_message", "Write operations forbidden"))
                .when(readOnlyGuard).checkWritable("rocketmq_send_message");

        assertThatThrownBy(() -> produceTools.sendMessage("OrderTopic", "body", null, null, null, null, null))
                .isInstanceOf(ReadOnlyException.class)
                .hasMessageContaining("rocketmq_send_message");

        verify(messagingClientService, never()).sendMessage(anyString(), anyString(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("验证缺少必填参数 topic 或 body 时前置抛出 IllegalArgumentException")
    void shouldThrowExceptionWhenRequiredParamsAreBlank() {
        doNothing().when(readOnlyGuard).checkWritable("rocketmq_send_message");

        assertThatThrownBy(() -> produceTools.sendMessage(null, "body", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'topic' must not be blank");

        assertThatThrownBy(() -> produceTools.sendMessage("   ", "body", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'topic' must not be blank");

        assertThatThrownBy(() -> produceTools.sendMessage("OrderTopic", null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'body' must not be blank");

        assertThatThrownBy(() -> produceTools.sendMessage("OrderTopic", "   ", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'body' must not be blank");
    }

    @Test
    @DisplayName("验证延时秒数为 0 或负数时拦截并抛出 IllegalArgumentException")
    void shouldThrowExceptionWhenDelaySecondsIsNonPositive() {
        doNothing().when(readOnlyGuard).checkWritable("rocketmq_send_message");

        assertThatThrownBy(() -> produceTools.sendMessage("OrderTopic", "body", null, null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'delaySeconds' must be greater than 0");

        assertThatThrownBy(() -> produceTools.sendMessage("OrderTopic", "body", null, null, null, -10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'delaySeconds' must be greater than 0");
    }

    @Test
    @DisplayName("验证定时投递时间戳小于当前时间时拦截并抛出 IllegalArgumentException")
    void shouldThrowExceptionWhenDeliveryTimestampIsInThePast() {
        doNothing().when(readOnlyGuard).checkWritable("rocketmq_send_message");

        assertThatThrownBy(() -> produceTools.sendMessage("OrderTopic", "body", null, null, null, null, System.currentTimeMillis() - 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'deliveryTimestamp' must be in the future");
    }

    @Test
    @DisplayName("验证同时指定 messageGroup 与延时参数时防呆拦截并抛出 IllegalArgumentException")
    void shouldThrowExceptionWhenFifoAndDelaySpecifiedTogether() {
        doNothing().when(readOnlyGuard).checkWritable("rocketmq_send_message");

        assertThatThrownBy(() -> produceTools.sendMessage("OrderTopic", "body", null, null, "GRP_01", 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FIFO and Delay messages are mutually exclusive in RocketMQ 5.x");

        assertThatThrownBy(() -> produceTools.sendMessage("OrderTopic", "body", null, null, "GRP_01", null, System.currentTimeMillis() + 10_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FIFO and Delay messages are mutually exclusive in RocketMQ 5.x");
    }
}
