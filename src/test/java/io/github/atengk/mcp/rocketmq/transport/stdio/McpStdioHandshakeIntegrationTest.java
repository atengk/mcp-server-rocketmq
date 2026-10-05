package io.github.atengk.mcp.rocketmq.transport.stdio;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP Stdio 传输端到端原生握手集成测试（接缝 2）。
 * 验证在 stdio 运行模式下，进程标准输入输出符合 MCP JSON-RPC 协议契约，
 * 并严格保证 stdout 标准输出 100% 纯净（零 Banner、零非协议日志污染）。
 *
 * @author Ateng
 * @since 2026-10-05
 */
@DisplayName("MCP Stdio 端到端原生握手集成测试 (接缝 2)")
class McpStdioHandshakeIntegrationTest {

    @Test
    @DisplayName("验证 Stdio 模式下 MCP initialize 握手与 tools/list 纯净输出")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldPerformMcpHandshakeWithPureStdout() throws Exception {
        String javaHome = System.getProperty("java.home");
        String javaBin = Path.of(javaHome, "bin", "java").toString();

        ProcessBuilder pb;
        Path jarPath = Path.of("target", "mcp-server-rocketmq-1.0.0.jar");
        if (Files.exists(jarPath)) {
            pb = new ProcessBuilder(
                    javaBin,
                    "-jar",
                    jarPath.toAbsolutePath().toString(),
                    "--mcp.transport=stdio"
            );
        } else {
            String surefireCp = System.getProperty("surefire.test.class.path");
            String cp = (surefireCp != null && !surefireCp.isBlank()) ? surefireCp : System.getProperty("java.class.path");
            cp = Path.of("target", "classes").toAbsolutePath() + File.pathSeparator + cp;
            pb = new ProcessBuilder(
                    javaBin,
                    "-cp",
                    cp,
                    "io.github.atengk.mcp.rocketmq.McpServerRocketmqApplication",
                    "--mcp.transport=stdio"
            );
        }

        Process process = pb.start();

        ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
        Thread errThread = new Thread(() -> {
            try {
                process.getErrorStream().transferTo(errCapture);
            } catch (IOException ignored) {
            }
        });
        errThread.setDaemon(true);
        errThread.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {

            // 1. 发送 MCP 初始化请求 (initialize)
            String initRequest = """
                    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}
                    """;
            writer.write(initRequest.trim());
            writer.newLine();
            writer.flush();

            // 2. 读取第一行响应，必须为纯净的 JSON-RPC 握手响应
            String initResponseLine = reader.readLine();
            assertThat(initResponseLine)
                    .withFailMessage("标准输出未能接收到有效响应行。子进程错误日志:\n" + errCapture.toString(StandardCharsets.UTF_8))
                    .isNotNull()
                    .startsWith("{")
                    .contains("\"jsonrpc\":\"2.0\"");

            JSONObject initJson = JSON.parseObject(initResponseLine);
            assertThat(initJson.getIntValue("id")).isEqualTo(1);
            JSONObject resultObj = initJson.getJSONObject("result");
            assertThat(resultObj).isNotNull();
            assertThat(resultObj.getJSONObject("serverInfo").getString("name"))
                    .contains("mcp-server-rocketmq");
            assertThat(resultObj.getJSONObject("capabilities").containsKey("tools")).isTrue();

            // 3. 发送 initialized 通知
            String initializedNotification = """
                    {"jsonrpc":"2.0","method":"notifications/initialized"}
                    """;
            writer.write(initializedNotification.trim());
            writer.newLine();
            writer.flush();

            // 4. 发送 tools/list 查询工具清单
            String listToolsRequest = """
                    {"jsonrpc":"2.0","id":2,"method":"tools/list"}
                    """;
            writer.write(listToolsRequest.trim());
            writer.newLine();
            writer.flush();

            String listToolsResponseLine = reader.readLine();
            assertThat(listToolsResponseLine)
                    .withFailMessage("标准输出未能接收到 tools/list 响应。子进程错误日志:\n" + errCapture.toString(StandardCharsets.UTF_8))
                    .isNotNull()
                    .startsWith("{")
                    .contains("\"jsonrpc\":\"2.0\"");

            JSONObject toolsJson = JSON.parseObject(listToolsResponseLine);
            assertThat(toolsJson.getIntValue("id")).isEqualTo(2);
            JSONArray toolsArray = toolsJson.getJSONObject("result").getJSONArray("tools");
            assertThat(toolsArray).isNotNull();
            assertThat(toolsArray.size()).isGreaterThanOrEqualTo(10);
        } finally {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }
}
