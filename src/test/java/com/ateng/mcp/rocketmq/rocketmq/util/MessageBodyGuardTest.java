package com.ateng.mcp.rocketmq.rocketmq.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 消息体安全解码与防爆截断单元测试。
 * 验证 UTF-8 解码、Base64 降级策略、4KB 超长截断与空值安全。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@DisplayName("MessageBodyGuard 单元测试")
class MessageBodyGuardTest {

    @Test
    @DisplayName("验证常规 UTF-8 文本消息正常解码且不截断")
    void shouldDecodeNormalUtf8TextWithoutTruncation() {
        String normalText = "{\"orderId\":\"12345678\",\"amount\":99.9,\"status\":\"PAID\"}";
        byte[] body = normalText.getBytes(StandardCharsets.UTF_8);

        String result = MessageBodyGuard.protect(body);

        assertThat(result).isEqualTo(normalText);
        assertThat(result).doesNotContain("[Body Truncated");
    }

    @Test
    @DisplayName("验证超过 4KB 的超大文本报文被安全截断并注入标记")
    void shouldTruncateLargeTextExceedingThreshold() {
        // 构造一个长度为 8192 的长文本
        String longText = "A".repeat(8192);
        byte[] body = longText.getBytes(StandardCharsets.UTF_8);

        String result = MessageBodyGuard.protect(body);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("A".repeat(4096));
        assertThat(result).contains("[Body Truncated: original size 8192 bytes]");
    }

    @Test
    @DisplayName("验证不可打印二进制流数据优雅降级为 Base64 编码")
    void shouldFallbackToBase64ForBinaryData() {
        byte[] binaryData = new byte[] {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE, 0x00};

        String result = MessageBodyGuard.protect(binaryData);

        String expectedBase64 = Base64.getEncoder().encodeToString(binaryData);
        assertThat(result).isEqualTo(expectedBase64);
    }

    @Test
    @DisplayName("验证超长二进制流在 Base64 编码后亦应用截断防护")
    void shouldTruncateLargeBinaryDataAfterBase64() {
        byte[] largeBinary = new byte[6000];
        for (int i = 0; i < largeBinary.length; i++) {
            largeBinary[i] = (byte) (i % 256);
        }

        String result = MessageBodyGuard.protect(largeBinary);

        assertThat(result).contains("[Body Truncated: original size 6000 bytes]");
    }

    @Test
    @DisplayName("验证自定义字符截断阈值生效")
    void shouldRespectCustomMaxCharsLimit() {
        String text = "1234567890abcdefghij";
        byte[] body = text.getBytes(StandardCharsets.UTF_8);

        String result = MessageBodyGuard.protect(body, 10);

        assertThat(result).isEqualTo("1234567890 [Body Truncated: original size 20 bytes]");
    }

    @Test
    @DisplayName("验证空字节数组与 null 输入防御性返回空字符串")
    void shouldHandleNullOrEmptySafely() {
        assertThat(MessageBodyGuard.protect(null)).isEmpty();
        assertThat(MessageBodyGuard.protect(new byte[0])).isEmpty();
    }
}
