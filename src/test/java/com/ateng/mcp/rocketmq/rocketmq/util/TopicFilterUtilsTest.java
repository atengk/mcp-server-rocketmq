package com.ateng.mcp.rocketmq.rocketmq.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 主题过滤工具类单元测试。
 * 验证对系统主题与业务主题的识别与过滤准确性。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@DisplayName("TopicFilterUtils 单元测试")
class TopicFilterUtilsTest {

    @Test
    @DisplayName("验证正确识别各类系统保留主题")
    void shouldIdentifySystemTopics() {
        assertThat(TopicFilterUtils.isSystemTopic("%SYS%consumer_trace")).isTrue();
        assertThat(TopicFilterUtils.isSystemTopic("TBW102")).isTrue();
        assertThat(TopicFilterUtils.isSystemTopic("BenchmarkTest")).isTrue();
        assertThat(TopicFilterUtils.isSystemTopic("SCHEDULE_TOPIC_XXXX")).isTrue();
        assertThat(TopicFilterUtils.isSystemTopic("RMQ_SYS_TRANS_HALF_TOPIC")).isTrue();
        assertThat(TopicFilterUtils.isSystemTopic("SELF_TEST_TOPIC")).isTrue();
        assertThat(TopicFilterUtils.isSystemTopic("OFFSET_MOVED_EVENT")).isTrue();
        assertThat(TopicFilterUtils.isSystemTopic("DefaultCluster")).isTrue();

        assertThat(TopicFilterUtils.isSystemTopic("order_paid_topic")).isFalse();
        assertThat(TopicFilterUtils.isSystemTopic("user_signup_event")).isFalse();
        assertThat(TopicFilterUtils.isSystemTopic("")).isFalse();
        assertThat(TopicFilterUtils.isSystemTopic(null)).isFalse();
    }

    @Test
    @DisplayName("验证从原始主题集合中过滤出排序后的纯业务主题")
    void shouldFilterBusinessTopics() {
        List<String> rawTopics = List.of(
                "TBW102",
                "order_payment_topic",
                "%SYS%trace",
                "user_login_topic",
                "BenchmarkTest"
        );

        List<String> businessTopics = TopicFilterUtils.filterBusinessTopics(rawTopics);

        assertThat(businessTopics).containsExactly("order_payment_topic", "user_login_topic");
    }

    @Test
    @DisplayName("验证空集合输入时安全返回空列表")
    void shouldHandleNullOrEmptyInputSafely() {
        assertThat(TopicFilterUtils.filterBusinessTopics(null)).isEmpty();
        assertThat(TopicFilterUtils.filterBusinessTopics(List.of())).isEmpty();
    }
}
