package io.github.atengk.mcp.rocketmq.rocketmq.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 消费组过滤工具类单元测试。
 * 验证对 RocketMQ 系统内置保留消费组的精准识别与业务消费组提取。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@DisplayName("ConsumerGroupFilterUtils 单元测试")
class ConsumerGroupFilterUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "TOOLS_CONSUMER",
            "FILTERSRV_CONSUMER",
            "SCHEDULE_CONSUMER",
            "SELF_TEST_CONSUMER",
            "DEFAULT_CONSUMER",
            "%SYS%_consumer_group",
            "CID_ONS-INTERNAL_test",
            "CID_RMQ_SYS_trace",
            "rmq_sys_trans_check",
            "BenchmarkConsumer_group",
            "",
            "   "
    })
    @DisplayName("验证系统内置消费组名称被正确识别为系统组")
    void shouldRecognizeSystemConsumerGroups(String systemGroup) {
        assertThat(ConsumerGroupFilterUtils.isSystemConsumerGroup(systemGroup)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "order_payment_group",
            "user_center_consumer",
            "inventory_sync_group",
            "canal_rocketmq_consumer"
    })
    @DisplayName("验证业务消费组名称不被误识别为系统组")
    void shouldRecognizeBusinessConsumerGroups(String businessGroup) {
        assertThat(ConsumerGroupFilterUtils.isSystemConsumerGroup(businessGroup)).isFalse();
    }

    @Test
    @DisplayName("验证过滤业务消费组列表并自动排序")
    void shouldFilterAndSortBusinessConsumerGroups() {
        List<String> rawGroups = List.of(
                "order_payment_group",
                "TOOLS_CONSUMER",
                "user_center_consumer",
                "%SYS%group",
                "analytics_group"
        );

        List<String> filtered = ConsumerGroupFilterUtils.filterBusinessConsumerGroups(rawGroups);

        assertThat(filtered)
                .isNotNull()
                .containsExactly("analytics_group", "order_payment_group", "user_center_consumer");
    }

    @Test
    @DisplayName("验证空输入或全系统组输入时返回空集合")
    void shouldReturnEmptyListForNullOrAllSystem() {
        assertThat(ConsumerGroupFilterUtils.filterBusinessConsumerGroups(null)).isEmpty();
        assertThat(ConsumerGroupFilterUtils.filterBusinessConsumerGroups(Collections.emptyList())).isEmpty();
        assertThat(ConsumerGroupFilterUtils.filterBusinessConsumerGroups(List.of("TOOLS_CONSUMER", "%SYS%test"))).isEmpty();
    }
}
