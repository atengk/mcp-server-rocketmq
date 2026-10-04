package com.ateng.mcp.rocketmq.transport.stdio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stdio 传输模式环境后置处理器单元测试。
 * 验证在 stdio 模式下 Banner 关闭、Web 容器关闭与输出流重定向逻辑。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@DisplayName("Stdio 传输模式环境后置处理器测试")
class StdioEnvironmentPostProcessorTest {

    private final StdioEnvironmentPostProcessor processor = new StdioEnvironmentPostProcessor();

    @Test
    @DisplayName("当 mcp.transport=stdio 时应正确关闭 Banner、关闭 Web 容器并重定向至 stderr")
    void shouldConfigureStdioModeWhenPropertySet() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("mcp.transport", "stdio");

        SpringApplication application = new SpringApplication();
        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("spring.ai.mcp.server.stdio")).isEqualTo("true");
        assertThat(environment.getProperty("CONSOLE_LOG_TARGET")).isEqualTo("System.err");
        assertThat(environment.getProperty("spring.main.banner-mode")).isEqualTo("off");
        assertThat(environment.getProperty("spring.main.web-application-type")).isEqualTo("none");
    }

    @Test
    @DisplayName("当非 stdio 模式时不应强制覆写 stdio 相关属性")
    void shouldNotConfigureStdioModeWhenPropertyNotSet() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("mcp.transport", "sse");

        SpringApplication application = new SpringApplication();
        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("spring.ai.mcp.server.stdio")).isNull();
        assertThat(environment.getProperty("CONSOLE_LOG_TARGET")).isNull();
    }
}
