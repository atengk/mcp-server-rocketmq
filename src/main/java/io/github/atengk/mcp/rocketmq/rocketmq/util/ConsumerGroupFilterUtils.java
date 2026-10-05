package io.github.atengk.mcp.rocketmq.rocketmq.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RocketMQ 消费组过滤与命名识别工具类。
 * 用于静默隐藏 RocketMQ 内部自带的管理、基准与定时调度消费组，聚焦业务消费组。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public final class ConsumerGroupFilterUtils {

    /**
     * 系统保留消费组全名黑名单。
     */
    private static final Set<String> SYSTEM_GROUP_EXACT = Set.of(
            "TOOLS_CONSUMER",
            "FILTERSRV_CONSUMER",
            "SCHEDULE_CONSUMER",
            "SELF_TEST_CONSUMER",
            "DEFAULT_CONSUMER"
    );

    /**
     * 系统消费组前缀黑名单。
     */
    private static final List<String> SYSTEM_GROUP_PREFIXES = List.of(
            "%SYS%",
            "CID_ONS-INTERNAL",
            "CID_RMQ_SYS_",
            "rmq_sys_",
            "BenchmarkConsumer"
    );

    private ConsumerGroupFilterUtils() {
    }

    /**
     * 判断指定消费组名称是否为系统内部保留消费组。
     *
     * @param groupName 消费组名称
     * @return 若为系统内部保留消费组则返回 true，否则返回 false
     */
    public static boolean isSystemConsumerGroup(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return true;
        }
        String trimmed = groupName.trim();
        if (SYSTEM_GROUP_EXACT.contains(trimmed)) {
            return true;
        }
        for (String prefix : SYSTEM_GROUP_PREFIXES) {
            if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤给定的消费组集合，仅保留业务消费组并进行字典序升序排序。
     *
     * @param groups 原始消费组集合
     * @return 过滤并排序后的业务消费组列表，输入为 null 或无匹配时返回空列表
     */
    public static List<String> filterBusinessConsumerGroups(Collection<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        return groups.stream()
                .filter(group -> !isSystemConsumerGroup(group))
                .sorted()
                .collect(Collectors.toList());
    }
}
