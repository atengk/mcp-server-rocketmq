package io.github.atengk.mcp.rocketmq.rocketmq.messaging.dto;

/**
 * 消息发送结果数据传输对象。
 * 承载 gRPC 客户端发送测试消息后返回的消息 ID、状态、路由分片及发送时序信息。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class SendMessageResultDTO {

    /**
     * 服务端确认生成的全局唯一消息标识 ID。
     */
    private String messageId;

    /**
     * 目标主题名称。
     */
    private String topic;

    /**
     * 发送状态（如 SUCCESS、FAILED）。
     */
    private String status = "SUCCESS";

    /**
     * 消息所属标签 Tag。
     */
    private String tag;

    /**
     * 业务索引键 Keys。
     */
    private String keys;

    /**
     * 顺序分区消息组标识（MessageGroup，可选）。
     */
    private String messageGroup;

    /**
     * 定时/延时投递目标时间戳毫秒值（可选）。
     */
    private Long deliveryTimestamp;

    /**
     * 客户端发送完成时间戳毫秒值。
     */
    private long sendTimestamp = System.currentTimeMillis();

    /**
     * 发送消息体字节长度。
     */
    private int bodySize = 0;

    public SendMessageResultDTO() {
    }

    public SendMessageResultDTO(String messageId, String topic, String status) {
        this.messageId = messageId;
        this.topic = topic;
        this.status = status;
        this.sendTimestamp = System.currentTimeMillis();
    }

    public SendMessageResultDTO(String messageId, String topic, String status, String tag, String keys,
                                String messageGroup, Long deliveryTimestamp, int bodySize) {
        this.messageId = messageId;
        this.topic = topic;
        this.status = status;
        this.tag = tag;
        this.keys = keys;
        this.messageGroup = messageGroup;
        this.deliveryTimestamp = deliveryTimestamp;
        this.bodySize = bodySize;
        this.sendTimestamp = System.currentTimeMillis();
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    public String getMessageGroup() {
        return messageGroup;
    }

    public void setMessageGroup(String messageGroup) {
        this.messageGroup = messageGroup;
    }

    public Long getDeliveryTimestamp() {
        return deliveryTimestamp;
    }

    public void setDeliveryTimestamp(Long deliveryTimestamp) {
        this.deliveryTimestamp = deliveryTimestamp;
    }

    public long getSendTimestamp() {
        return sendTimestamp;
    }

    public void setSendTimestamp(long sendTimestamp) {
        this.sendTimestamp = sendTimestamp;
    }

    public int getBodySize() {
        return bodySize;
    }

    public void setBodySize(int bodySize) {
        this.bodySize = bodySize;
    }
}
