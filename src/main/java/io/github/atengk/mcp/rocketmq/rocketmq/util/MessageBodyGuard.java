package io.github.atengk.mcp.rocketmq.rocketmq.util;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * RocketMQ 消息体报文防爆截断与安全解码保护器。
 * 遵循 UTF-8 优先、失败降级 Base64 策略，施加 4KB 长度限制以防止大报文破坏 AI 上下文。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public final class MessageBodyGuard {

    /**
     * 默认消息体最大安全截断长度（4KB = 4096 字符）。
     */
    public static final int DEFAULT_MAX_BODY_CHARS = 4096;

    /**
     * 截断提示后缀模板。
     */
    private static final String TRUNCATED_SUFFIX_TEMPLATE = " [Body Truncated: original size %d bytes]";

    private MessageBodyGuard() {
    }

    /**
     * 对原生消息体字节数组进行安全解码并应用 4KB 截断保护。
     *
     * @param body 原生消息体字节数组
     * @return 解码并保护后的安全消息体字符串
     */
    public static String protect(byte[] body) {
        return protect(body, DEFAULT_MAX_BODY_CHARS);
    }

    /**
     * 对原生消息体字节数组进行安全解码并应用指定阈值截断保护。
     *
     * @param body 原生消息体字节数组
     * @param maxChars 最大允许字符长度
     * @return 解码并保护后的安全消息体字符串
     */
    public static String protect(byte[] body, int maxChars) {
        if (body == null || body.length == 0) {
            return "";
        }

        int limit = maxChars > 0 ? maxChars : DEFAULT_MAX_BODY_CHARS;

        // 1. 尝试严格 UTF-8 解码，失败或含有控制字符则降级 Base64
        String decodedText = decodeUtf8OrFallbackBase64(body);

        // 2. 检查是否超出截断阈值
        if (decodedText.length() > limit) {
            String truncated = decodedText.substring(0, limit);
            return truncated + String.format(TRUNCATED_SUFFIX_TEMPLATE, body.length);
        }

        return decodedText;
    }

    /**
     * 尝试采用 UTF-8 进行安全文本解码，若存在非法编码或控制字符则降级为 Base64。
     *
     * @param body 字节数组
     * @return 解码后的字符串
     */
    private static String decodeUtf8OrFallbackBase64(byte[] body) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            String candidate = decoder.decode(java.nio.ByteBuffer.wrap(body)).toString();

            // 检查二进制内容（若包含多量空字节或控制字符则降级为 Base64）
            if (isBinaryData(candidate)) {
                return Base64.getEncoder().encodeToString(body);
            }
            return candidate;
        } catch (CharacterCodingException e) {
            return Base64.getEncoder().encodeToString(body);
        }
    }

    /**
     * 简单启发式判断是否属于二进制流数据。
     *
     * @param text 解码文本
     * @return 是否应视为二进制流
     */
    private static boolean isBinaryData(String text) {
        int checkLen = Math.min(text.length(), 256);
        int unprintableCount = 0;
        for (int i = 0; i < checkLen; i++) {
            char c = text.charAt(i);
            if (c == 0 || (c < 32 && c != '\t' && c != '\n' && c != '\r')) {
                unprintableCount++;
            }
        }
        return unprintableCount > 0;
    }
}
