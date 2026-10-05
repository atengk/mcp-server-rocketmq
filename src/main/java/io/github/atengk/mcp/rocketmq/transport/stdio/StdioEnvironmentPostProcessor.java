package io.github.atengk.mcp.rocketmq.transport.stdio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Stdio 传输自适应与标准输出纯净化环境后置处理器。
 * 负责在探测到 stdio 运行模式时，关闭 Banner 与 Web 容器，并将日志重定向至 stderr。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@SuppressWarnings("deprecation")
public class StdioEnvironmentPostProcessor implements
        org.springframework.boot.EnvironmentPostProcessor,
        org.springframework.boot.env.EnvironmentPostProcessor,
        Ordered {

    public static final String STDIO_TRANSPORT_PROPERTY_SOURCE = "stdioTransportOverrides";
    public static final String MCP_TRANSPORT_KEY = "mcp.transport";
    public static final String MCP_TRANSPORT_ENV_KEY = "MCP_TRANSPORT";
    public static final String STDIO_MODE = "stdio";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String transport = environment.getProperty(MCP_TRANSPORT_KEY);
        if (transport == null || transport.isBlank()) {
            transport = environment.getProperty(MCP_TRANSPORT_ENV_KEY);
        }

        if (STDIO_MODE.equalsIgnoreCase(transport)) {
            // 1. 关闭 Spring Boot ASCII Banner
            application.setBannerMode(Banner.Mode.OFF);

            // 2. 关闭内嵌 Web 容器，降低常驻开销
            application.setWebApplicationType(WebApplicationType.NONE);

            // 3. 将控制台日志重定向至 System.err，保证 stdout 100% 纯净 (ADR 0003)
            System.setProperty("CONSOLE_LOG_TARGET", "System.err");

            // 4. 注入标准输出纯净化与 stdio 传输覆盖配置
            Map<String, Object> overrides = new HashMap<>();
            overrides.put("spring.ai.mcp.server.stdio", "true");
            overrides.put("spring.ai.mcp.server.protocol", "STREAMABLE");
            overrides.put("spring.main.banner-mode", "off");
            overrides.put("spring.main.web-application-type", "none");
            overrides.put("CONSOLE_LOG_TARGET", "System.err");
            overrides.put("console.log.target", "System.err");

            environment.getPropertySources().addFirst(new MapPropertySource(STDIO_TRANSPORT_PROPERTY_SOURCE, overrides));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
