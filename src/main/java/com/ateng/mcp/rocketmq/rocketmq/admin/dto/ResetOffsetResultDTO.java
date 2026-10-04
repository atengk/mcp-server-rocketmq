package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

/**
 * 消费位点重置结果数据传输对象。
 * 承载消费组在指定主题上按时间戳或跳过积压模式重置点位的回执详情。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class ResetOffsetResultDTO {

    /**
     * 目标消费组名称。
     */
    private String consumerGroup;

    /**
     * 目标主题名称。
     */
    private String topic;

    /**
     * 重置模式（TIMESTAMP 或 MAX_OFFSET）。
     */
    private String resetMode;

    /**
     * 目标时间戳（毫秒，按时间戳重置模式有效）。
     */
    private Long targetTimestamp;

    /**
     * 执行状态（SUCCESS 或 FAILED）。
     */
    private String status = "SUCCESS";

    /**
     * 结果提示信息。
     */
    private String message;

    /**
     * 操作执行时间戳（毫秒）。
     */
    private long timestamp = System.currentTimeMillis();

    public ResetOffsetResultDTO() {
    }

    public ResetOffsetResultDTO(String consumerGroup, String topic, String resetMode, Long targetTimestamp,
                                String status, String message) {
        this.consumerGroup = consumerGroup;
        this.topic = topic;
        this.resetMode = resetMode;
        this.targetTimestamp = targetTimestamp;
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getResetMode() {
        return resetMode;
    }

    public void setResetMode(String resetMode) {
        this.resetMode = resetMode;
    }

    public Long getTargetTimestamp() {
        return targetTimestamp;
    }

    public void setTargetTimestamp(Long targetTimestamp) {
        this.targetTimestamp = targetTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
