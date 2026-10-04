package com.ateng.mcp.rocketmq.rocketmq.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RocketMQ 主题过滤工具类。
 * 提供对系统内部管理保留主题的判定与静默过滤能力。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public final class TopicFilterUtils {

    private static final Set<String> SYSTEM_TOPIC_PREFIXES = Set.of(
            "%SYS%",
            "TBW102",
            "BENCHMARKTEST",
            "SCHEDULE_TOPIC",
            "RMQ_SYS_",
            "SELF_TEST_TOPIC",
            "OFFSET_MOVED_EVENT",
            "DEFAULTCLUSTER"
    );

    private TopicFilterUtils() {
    }

    /**
     * 判断指定主题是否为系统内部保留主题。
     *
     * @param topic 主题名称
     * @return 如果是系统内部主题则返回 true，否则返回 false
     */
    public static boolean isSystemTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return false;
        }
        String upper = topic.toUpperCase();
        for (String prefix : SYSTEM_TOPIC_PREFIXES) {
            if (upper.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤出纯业务主题列表（排除所有系统保留主题）。
     *
     * @param topics 原始主题集合
     * @return 过滤后的业务主题列表，保证非 null
     */
    public static List<String> filterBusinessTopics(Collection<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return Collections.emptyList();
        }
        return topics.stream()
                .filter(topic -> topic != null && !topic.isBlank() && !isSystemTopic(topic))
                .sorted()
                .collect(Collectors.toList());
    }
}
