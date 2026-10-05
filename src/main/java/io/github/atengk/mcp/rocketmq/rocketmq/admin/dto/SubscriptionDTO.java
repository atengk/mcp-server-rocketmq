package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 消费组订阅关系数据传输对象。
 * 承载消费组对指定主题的订阅表达式及 Tag 过滤集合。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class SubscriptionDTO {

    /**
     * 订阅的目标主题。
     */
    private String topic;

    /**
     * 订阅表达式字符串（如 *、TagA || TagB 等）。
     */
    private String subString;

    /**
     * 解析后的 Tag 标签集合。
     */
    private Set<String> tagsSet = new HashSet<>();

    public SubscriptionDTO() {
    }

    public SubscriptionDTO(String topic, String subString, Set<String> tagsSet) {
        this.topic = topic;
        this.subString = subString;
        setTagsSet(tagsSet);
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSubString() {
        return subString;
    }

    public void setSubString(String subString) {
        this.subString = subString;
    }

    public Set<String> getTagsSet() {
        return tagsSet != null ? tagsSet : Collections.emptySet();
    }

    public void setTagsSet(Set<String> tagsSet) {
        this.tagsSet = tagsSet != null ? new HashSet<>(tagsSet) : new HashSet<>();
    }
}
