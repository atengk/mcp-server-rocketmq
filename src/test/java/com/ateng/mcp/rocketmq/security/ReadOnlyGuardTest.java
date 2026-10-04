package com.ateng.mcp.rocketmq.security;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 全局只读安全守卫单元测试。
 * 验证读写模式检测及写操作短路拦截行为。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@DisplayName("ReadOnlyGuard 单元测试")
class ReadOnlyGuardTest {

    @Test
    @DisplayName("验证默认读写模式下校验正常通过不抛出异常")
    void shouldAllowOperationWhenReadOnlyIsFalse() {
        RocketmqProperties properties = new RocketmqProperties();
        properties.setReadOnly(false);
        ReadOnlyGuard guard = new ReadOnlyGuard(properties);

        assertThat(guard.isReadOnly()).isFalse();
        assertThatCode(() -> guard.checkWritable("rocketmq_send_message"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("验证开启只读模式时写操作被拦截并抛出 ReadOnlyException")
    void shouldBlockOperationWhenReadOnlyIsTrue() {
        RocketmqProperties properties = new RocketmqProperties();
        properties.setReadOnly(true);
        ReadOnlyGuard guard = new ReadOnlyGuard(properties);

        assertThat(guard.isReadOnly()).isTrue();
        assertThatThrownBy(() -> guard.checkWritable("rocketmq_send_message"))
                .isInstanceOf(ReadOnlyException.class)
                .hasMessageContaining("rocketmq_send_message")
                .hasMessageContaining("READ-ONLY mode");
    }

    @Test
    @DisplayName("验证操作名为空时依然安全拦截并提供默认提示")
    void shouldBlockOperationWhenNameIsBlank() {
        RocketmqProperties properties = new RocketmqProperties();
        properties.setReadOnly(true);
        ReadOnlyGuard guard = new ReadOnlyGuard(properties);

        assertThatThrownBy(() -> guard.checkWritable(null))
                .isInstanceOf(ReadOnlyException.class)
                .hasMessageContaining("write_operation")
                .hasMessageContaining("READ-ONLY mode");
    }
}
