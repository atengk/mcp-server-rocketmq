package io.github.atengk.mcp.rocketmq.rocketmq.messaging;

import io.github.atengk.mcp.rocketmq.config.RocketmqProperties;
import io.github.atengk.mcp.rocketmq.rocketmq.messaging.dto.SendMessageResultDTO;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.apache.rocketmq.client.apis.message.MessageId;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * gRPC 消息发送客户端服务单元测试。
 * 验证 Producer 客户端构建、消息发送委托及生命周期关闭逻辑。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@DisplayName("DefaultMessagingClientService 单元测试")
class DefaultMessagingClientServiceTest {

    @Test
    @DisplayName("验证通过 gRPC 成功发送测试消息并返回 SendMessageResultDTO")
    void shouldSendMessageSuccessfully() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        properties.setEndpoints("127.0.0.1:8081");
        properties.setAccessKey("ak");
        properties.setSecretKey("sk");

        Producer mockProducer = mock(Producer.class);
        ClientServiceProvider mockProvider = mock(ClientServiceProvider.class);
        MessageBuilder mockMessageBuilder = mock(MessageBuilder.class);
        Message mockMessage = mock(Message.class);
        SendReceipt mockReceipt = mock(SendReceipt.class);
        MessageId mockMessageId = mock(MessageId.class);

        when(mockMessageId.toString()).thenReturn("01000000000000000000000001");
        when(mockReceipt.getMessageId()).thenReturn(mockMessageId);

        when(mockMessageBuilder.setTopic(any())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.setBody(any())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.setTag(any())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.setKeys(any())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.setMessageGroup(any())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.setDeliveryTimestamp(anyLong())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.build()).thenReturn(mockMessage);

        when(mockProvider.newMessageBuilder()).thenReturn(mockMessageBuilder);
        when(mockProducer.send(any(Message.class))).thenReturn(mockReceipt);

        TestableMessagingClientService service = new TestableMessagingClientService(properties, mockProducer, mockProvider);

        SendMessageResultDTO result = service.sendMessage(
                "TestTopic",
                "hello gRPC",
                "TagA",
                "KEY_TEST",
                "GRP_ORDER",
                1700000000000L
        );

        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isEqualTo("01000000000000000000000001");
        assertThat(result.getTopic()).isEqualTo("TestTopic");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getTag()).isEqualTo("TagA");
        assertThat(result.getKeys()).isEqualTo("KEY_TEST");
        assertThat(result.getMessageGroup()).isEqualTo("GRP_ORDER");
        assertThat(result.getDeliveryTimestamp()).isEqualTo(1700000000000L);
        assertThat(result.getBodySize()).isEqualTo("hello gRPC".getBytes().length);

        verify(mockProducer).send(mockMessage);
    }

    @Test
    @DisplayName("验证入参 topic 或 body 为空白时抛出 IllegalArgumentException")
    void shouldThrowExceptionWhenParamsAreBlank() {
        RocketmqProperties properties = new RocketmqProperties();
        Producer mockProducer = mock(Producer.class);
        ClientServiceProvider mockProvider = mock(ClientServiceProvider.class);
        TestableMessagingClientService service = new TestableMessagingClientService(properties, mockProducer, mockProvider);

        assertThatThrownBy(() -> service.sendMessage(null, "body", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topic 不能为空");

        assertThatThrownBy(() -> service.sendMessage("   ", "body", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topic 不能为空");

        assertThatThrownBy(() -> service.sendMessage("Topic", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("body 不能为空");

        assertThatThrownBy(() -> service.sendMessage("Topic", "  ", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("body 不能为空");
    }

    @Test
    @DisplayName("验证 destroy 生命周期安全调用 producer.close()")
    void shouldCloseProducerOnDestroy() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        Producer mockProducer = mock(Producer.class);
        ClientServiceProvider mockProvider = mock(ClientServiceProvider.class);
        TestableMessagingClientService service = new TestableMessagingClientService(properties, mockProducer, mockProvider);

        // 触发获取 producer
        service.getOrCreateProducer();
        service.destroy();

        verify(mockProducer).close();
    }

    @Test
    @DisplayName("验证超过 4MB 限制的消息体被拦截并抛出 IllegalArgumentException")
    void shouldRejectMessageExceedingMaxBodySize() {
        RocketmqProperties properties = new RocketmqProperties();
        Producer mockProducer = mock(Producer.class);
        ClientServiceProvider mockProvider = mock(ClientServiceProvider.class);
        TestableMessagingClientService service = new TestableMessagingClientService(properties, mockProducer, mockProvider);

        // 构造大于 4MB 的字符串 (4 * 1024 * 1024 + 1 bytes)
        String giantBody = "A".repeat(4 * 1024 * 1024 + 1);

        assertThatThrownBy(() -> service.sendMessage("TestTopic", giantBody, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("超过最大允许上限 4MB");
    }

    @Test
    @DisplayName("验证发送失败时触发自愈重连并关闭旧 Producer")
    void shouldInvalidateProducerOnSendFailure() throws Exception {
        RocketmqProperties properties = new RocketmqProperties();
        Producer mockProducer = mock(Producer.class);
        ClientServiceProvider mockProvider = mock(ClientServiceProvider.class);
        MessageBuilder mockMessageBuilder = mock(MessageBuilder.class);

        when(mockMessageBuilder.setTopic(any())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.setBody(any())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.build()).thenReturn(mock(Message.class));
        when(mockProvider.newMessageBuilder()).thenReturn(mockMessageBuilder);
        when(mockProducer.send(any())).thenThrow(new RuntimeException("gRPC channel broken"));

        TestableMessagingClientService service = new TestableMessagingClientService(properties, mockProducer, mockProvider);

        assertThatThrownBy(() -> service.sendMessage("TestTopic", "payload", null, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("gRPC channel broken");

        // 验证旧 producer 已被调用 close 进行自愈清理
        verify(mockProducer).close();
    }

    private static class TestableMessagingClientService extends DefaultMessagingClientService {
        private final Producer mockProducer;
        private final ClientServiceProvider mockProvider;

        public TestableMessagingClientService(RocketmqProperties properties, Producer mockProducer, ClientServiceProvider mockProvider) {
            super(properties);
            this.mockProducer = mockProducer;
            this.mockProvider = mockProvider;
        }

        @Override
        protected Producer createProducer() {
            return mockProducer;
        }

        @Override
        protected ClientServiceProvider getClientServiceProvider() {
            return mockProvider;
        }
    }
}
