package com.ateng.mcp.rocketmq.mcp.tool;

import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.DlqMessageListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.MessageDetailDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.MessageListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.MessageTraceDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.MessageTraceNodeDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息检索、死信查验与轨迹追踪 MCP 工具单元测试。
 * 验证 rocketmq_query_message_by_id、rocketmq_query_message_by_key、rocketmq_query_dlq_messages 与 rocketmq_query_message_trace 工具调用逻辑与参数校验。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageTools MCP 工具单元测试")
class MessageToolsTest {

    @Mock
    private AdminClientService adminClientService;

    @InjectMocks
    private MessageTools messageTools;

    @Test
    @DisplayName("验证 rocketmq_query_message_by_id 正常调用")
    void shouldQueryMessageByIdSuccessfully() throws Exception {
        MessageDetailDTO mockDto = new MessageDetailDTO(
                "0A00000100002A9F0000000000000001",
                "0A00000100002A9F0000000000000001",
                "OrderTopic",
                "TagA",
                "ORDER_1001",
                0,
                100L,
                "127.0.0.1:10911",
                1700000000000L,
                1700000001000L,
                0,
                128,
                "{\"orderId\": 1001}",
                false,
                Map.of()
        );
        when(adminClientService.queryMessageById("0A00000100002A9F0000000000000001", "OrderTopic"))
                .thenReturn(mockDto);

        MessageDetailDTO result = messageTools.queryMessageById("0A00000100002A9F0000000000000001", "OrderTopic");

        assertThat(result).isNotNull();
        assertThat(result.getMsgId()).isEqualTo("0A00000100002A9F0000000000000001");
        assertThat(result.getTopic()).isEqualTo("OrderTopic");
        assertThat(result.getKeys()).isEqualTo("ORDER_1001");
        verify(adminClientService).queryMessageById("0A00000100002A9F0000000000000001", "OrderTopic");
    }

    @Test
    @DisplayName("验证 rocketmq_query_message_by_id 空白参数防御校验")
    void shouldFailQueryMessageByIdWhenMsgIdIsBlank() {
        assertThatThrownBy(() -> messageTools.queryMessageById(null, "OrderTopic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'msgId' must not be blank");

        assertThatThrownBy(() -> messageTools.queryMessageById("   ", "OrderTopic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'msgId' must not be blank");
    }

    @Test
    @DisplayName("验证 rocketmq_query_message_by_key 正常调用")
    void shouldQueryMessageByKeySuccessfully() throws Exception {
        MessageDetailDTO msg = new MessageDetailDTO(
                "MSG_KEY_01",
                "MSG_OFFSET_01",
                "OrderTopic",
                "TagA",
                "KEY_001",
                1,
                200L,
                "127.0.0.1:10911",
                1700000000000L,
                1700000001000L,
                0,
                64,
                "test-body",
                false,
                Map.of()
        );
        MessageListDTO mockList = new MessageListDTO(List.of(msg));
        when(adminClientService.queryMessageByKey("OrderTopic", "KEY_001", 1000L, 2000L, 32))
                .thenReturn(mockList);

        MessageListDTO result = messageTools.queryMessageByKey("OrderTopic", "KEY_001", 1000L, 2000L, 32);

        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getMessages().get(0).getKeys()).isEqualTo("KEY_001");
        verify(adminClientService).queryMessageByKey("OrderTopic", "KEY_001", 1000L, 2000L, 32);
    }

    @Test
    @DisplayName("验证 rocketmq_query_message_by_key 参数空白校验")
    void shouldFailQueryMessageByKeyWhenTopicOrKeyIsBlank() {
        assertThatThrownBy(() -> messageTools.queryMessageByKey(null, "KEY_001", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'topic' must not be blank");

        assertThatThrownBy(() -> messageTools.queryMessageByKey("OrderTopic", "  ", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'key' must not be blank");
    }

    @Test
    @DisplayName("验证 rocketmq_query_dlq_messages 正常调用")
    void shouldQueryDlqMessagesSuccessfully() throws Exception {
        MessageDetailDTO dlqMsg = new MessageDetailDTO(
                "DLQ_MSG_01",
                "DLQ_OFFSET_01",
                "%DLQ%OrderGroup",
                null,
                "KEY_FAIL",
                0,
                1L,
                "127.0.0.1:10911",
                1700000000000L,
                1700000001000L,
                16,
                64,
                "failed-payload",
                false,
                Map.of()
        );
        DlqMessageListDTO mockDlq = new DlqMessageListDTO("OrderGroup", "%DLQ%OrderGroup", List.of(dlqMsg));
        when(adminClientService.queryDlqMessages("OrderGroup", null, null, null))
                .thenReturn(mockDlq);

        DlqMessageListDTO result = messageTools.queryDlqMessages("OrderGroup", null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getConsumerGroup()).isEqualTo("OrderGroup");
        assertThat(result.getDlqTopic()).isEqualTo("%DLQ%OrderGroup");
        assertThat(result.getTotalCount()).isEqualTo(1);
        verify(adminClientService).queryDlqMessages("OrderGroup", null, null, null);
    }

    @Test
    @DisplayName("验证 rocketmq_query_dlq_messages 参数空白校验")
    void shouldFailQueryDlqMessagesWhenConsumerGroupIsBlank() {
        assertThatThrownBy(() -> messageTools.queryDlqMessages("   ", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'consumerGroup' must not be blank");
    }

    @Test
    @DisplayName("验证 rocketmq_query_message_trace 正常调用")
    void shouldQueryMessageTraceSuccessfully() throws Exception {
        MessageTraceNodeDTO node = new MessageTraceNodeDTO(
                "Pub",
                "192.168.1.100",
                15,
                1700000000000L,
                "SUCCESS",
                "OrderProducerGroup"
        );
        MessageTraceDTO mockTrace = new MessageTraceDTO("MSG_TRACE_01", "OrderTopic", List.of(node));
        when(adminClientService.queryMessageTrace("MSG_TRACE_01", "OrderTopic"))
                .thenReturn(mockTrace);

        MessageTraceDTO result = messageTools.queryMessageTrace("MSG_TRACE_01", "OrderTopic");

        assertThat(result).isNotNull();
        assertThat(result.getMsgId()).isEqualTo("MSG_TRACE_01");
        assertThat(result.getTopic()).isEqualTo("OrderTopic");
        assertThat(result.getTraceNodes()).hasSize(1);
        verify(adminClientService).queryMessageTrace("MSG_TRACE_01", "OrderTopic");
    }

    @Test
    @DisplayName("验证 rocketmq_query_message_trace 参数空白校验")
    void shouldFailQueryMessageTraceWhenMsgIdIsBlank() {
        assertThatThrownBy(() -> messageTools.queryMessageTrace(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'msgId' must not be blank");
    }
}
