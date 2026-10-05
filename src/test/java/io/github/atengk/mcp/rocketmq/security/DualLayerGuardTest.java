package io.github.atengk.mcp.rocketmq.security;

import io.github.atengk.mcp.rocketmq.config.RocketmqProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 双层防呆安全守卫单元测试。
 * 验证只读模式、环境变量开关与显式 confirm 二次确认拦截机制。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@DisplayName("DualLayerGuard 双层防呆安全守卫单元测试")
class DualLayerGuardTest {

    @Test
    @DisplayName("验证全授权模式（开启破坏性工具且确认 confirm=true）允许执行")
    void shouldPassWhenDestructiveEnabledAndConfirmed() {
        RocketmqProperties properties = new RocketmqProperties();
        properties.setReadOnly(false);
        properties.setEnableDestructiveTools(true);

        ReadOnlyGuard readOnlyGuard = new ReadOnlyGuard(properties);
        DualLayerGuard dualLayerGuard = new DualLayerGuard(properties, readOnlyGuard);

        assertThat(dualLayerGuard.isDestructiveEnabled()).isTrue();
        assertThatCode(() -> dualLayerGuard.checkDestructiveOperation("rocketmq_delete_topic", true))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("验证全局环境变量未开启破坏性工具时拒绝执行（第一层防呆）")
    void shouldBlockWhenDestructiveToolsDisabled() {
        RocketmqProperties properties = new RocketmqProperties();
        properties.setReadOnly(false);
        properties.setEnableDestructiveTools(false);

        ReadOnlyGuard readOnlyGuard = new ReadOnlyGuard(properties);
        DualLayerGuard dualLayerGuard = new DualLayerGuard(properties, readOnlyGuard);

        assertThat(dualLayerGuard.isDestructiveEnabled()).isFalse();
        assertThatThrownBy(() -> dualLayerGuard.checkDestructiveOperation("rocketmq_delete_topic", true))
                .isInstanceOf(DestructiveOperationBlockedException.class)
                .hasMessageContaining("ROCKETMQ_ENABLE_DESTRUCTIVE_TOOLS=true");
    }

    @Test
    @DisplayName("验证已开启破坏性工具但未显式传递 confirm=true 时拦截拒绝（第二层防呆）")
    void shouldBlockWhenConfirmIsNullOrEqualToFalse() {
        RocketmqProperties properties = new RocketmqProperties();
        properties.setReadOnly(false);
        properties.setEnableDestructiveTools(true);

        ReadOnlyGuard readOnlyGuard = new ReadOnlyGuard(properties);
        DualLayerGuard dualLayerGuard = new DualLayerGuard(properties, readOnlyGuard);

        assertThatThrownBy(() -> dualLayerGuard.checkDestructiveOperation("rocketmq_reset_consumer_offset", null))
                .isInstanceOf(DestructiveOperationBlockedException.class)
                .hasMessageContaining("confirm=true");

        assertThatThrownBy(() -> dualLayerGuard.checkDestructiveOperation("rocketmq_reset_consumer_offset", false))
                .isInstanceOf(DestructiveOperationBlockedException.class)
                .hasMessageContaining("confirm=true");
    }

    @Test
    @DisplayName("验证处于全局只读模式时破坏性操作直接被只读守卫短路拒绝")
    void shouldBlockWhenReadOnlyModeIsEnabled() {
        RocketmqProperties properties = new RocketmqProperties();
        properties.setReadOnly(true);
        properties.setEnableDestructiveTools(true);

        ReadOnlyGuard readOnlyGuard = new ReadOnlyGuard(properties);
        DualLayerGuard dualLayerGuard = new DualLayerGuard(properties, readOnlyGuard);

        assertThatThrownBy(() -> dualLayerGuard.checkDestructiveOperation("rocketmq_resend_dlq_message", true))
                .isInstanceOf(ReadOnlyException.class)
                .hasMessageContaining("rocketmq_resend_dlq_message");
    }
}
