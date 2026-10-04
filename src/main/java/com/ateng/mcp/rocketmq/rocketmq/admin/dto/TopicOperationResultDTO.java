package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

/**
 * 主题生命周期控制面操作结果数据传输对象。
 * 用于承载主题创建与删除操作的执行状态与回执信息。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class TopicOperationResultDTO {

    /**
     * 操作类型（例如 CREATE_TOPIC, DELETE_TOPIC）。
     */
    private String operation;

    /**
     * 目标主题名称。
     */
    private String topic;

    /**
     * 执行状态（SUCCESS 或 FAILED）。
     */
    private String status = "SUCCESS";

    /**
     * 提示说明信息。
     */
    private String message;

    /**
     * 执行完成时间戳（毫秒）。
     */
    private long timestamp = System.currentTimeMillis();

    public TopicOperationResultDTO() {
    }

    public TopicOperationResultDTO(String operation, String topic, String status, String message) {
        this.operation = operation;
        this.topic = topic;
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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
