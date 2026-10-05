package io.github.atengk.mcp.rocketmq.config.aot;

import com.alibaba.fastjson.parser.ParserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * RocketMQ AOT 编译运行环境后置处理器。
 * 负责在 Spring Boot 启动早期全局禁用 Fastjson 1.x ASM 动态字节码生成，
 * 强制退化为标准安全反射模式，防止 GraalVM Native 原生镜像运行时抛出 Unsafe.defineClass 异常。
 *
 * @author Ateng
 * @since 2026-10-05
 */
@SuppressWarnings("deprecation")
public class RocketmqAotEnvironmentPostProcessor implements
        org.springframework.boot.EnvironmentPostProcessor,
        org.springframework.boot.env.EnvironmentPostProcessor,
        Ordered {

    private static final Logger log = LoggerFactory.getLogger(RocketmqAotEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 1. 全局禁用 Fastjson ASM 字节码生成器，强制启用静态反射模式
        try {
            ParserConfig.getGlobalInstance().setAsmEnable(false);
            System.setProperty("fastjson.parser.autoTypeSupport", "true");
        } catch (Throwable t) {
            log.warn("配置 Fastjson AOT 兼容参数时发生异常: {}", t.getMessage());
        }
    }

    @Override
    public int getOrder() {
        // 保证在最早期执行，先于任何业务 Bean 与配置加载
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
