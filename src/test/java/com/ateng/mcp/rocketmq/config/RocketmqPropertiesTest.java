package com.ateng.mcp.rocketmq.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RocketMQ 核心配置属性绑定测试。
 * 验证默认值以及通过属性前缀 rocketmq.* 进行自定义绑定能力。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@DisplayName("RocketmqProperties 配置属性绑定测试")
class RocketmqPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @EnableConfigurationProperties(RocketmqProperties.class)
    static class TestConfig {
    }

    @Test
    @DisplayName("验证默认配置项符合规范预期")
    void shouldHaveDefaultValues() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RocketmqProperties properties = context.getBean(RocketmqProperties.class);
            assertThat(properties.getNamesrvAddr()).isEqualTo("127.0.0.1:9876");
            assertThat(properties.getEndpoints()).isEqualTo("127.0.0.1:8081");
            assertThat(properties.isReadOnly()).isFalse();
            assertThat(properties.isEnableDestructiveTools()).isFalse();
        });
    }

    @Test
    @DisplayName("验证自定义环境配置可成功绑定")
    void shouldBindCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "rocketmq.namesrv-addr=192.168.1.100:9876",
                        "rocketmq.endpoints=192.168.1.100:8081",
                        "rocketmq.access-key=ak-test",
                        "rocketmq.secret-key=sk-test",
                        "rocketmq.read-only=true",
                        "rocketmq.enable-destructive-tools=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RocketmqProperties properties = context.getBean(RocketmqProperties.class);
                    assertThat(properties.getNamesrvAddr()).isEqualTo("192.168.1.100:9876");
                    assertThat(properties.getEndpoints()).isEqualTo("192.168.1.100:8081");
                    assertThat(properties.getAccessKey()).isEqualTo("ak-test");
                    assertThat(properties.getSecretKey()).isEqualTo("sk-test");
                    assertThat(properties.isReadOnly()).isTrue();
                    assertThat(properties.isEnableDestructiveTools()).isTrue();
                });
    }
}
