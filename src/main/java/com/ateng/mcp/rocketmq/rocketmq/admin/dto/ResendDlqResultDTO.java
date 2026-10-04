package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

/**
 * 死信消息重新投递结果数据传输对象。
 * 承载死信消息重投至业务目标主题后的确认详情与消息 ID。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class ResendDlqResultDTO {

    /**
     * 所属消费组名称。
     */
    private String consumerGroup;

    /**
     * 原始死信消息 ID。
     */
    private String msgId;

    /**
     * 目标业务主题名称。
     */
    private String targetTopic;

    /**
     * 重新投递后新生成的消息 ID（可选）。
     */
    private String resendMessageId;

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

    public ResendDlqResultDTO() {
    }

    public ResendDlqResultDTO(String consumerGroup, String msgId, String targetTopic, String resendMessageId,
                             String status, String message) {
        this.consumerGroup = consumerGroup;
        this.msgId = msgId;
        this.targetTopic = targetTopic;
        this.resendMessageId = resendMessageId;
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

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getTargetTopic() {
        return targetTopic;
    }

    public void setTargetTopic(String targetTopic) {
        this.targetTopic = targetTopic;
    }

    public String getResendMessageId() {
        return resendMessageId;
    }

    public void setResendMessageId(String resendMessageId) {
        this.resendMessageId = resendMessageId;
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
