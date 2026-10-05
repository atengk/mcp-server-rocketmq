package io.github.atengk.mcp.rocketmq.rocketmq.admin.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 单条消息全景属性与内容数据传输对象。
 * 承载消息 ID、主题、Key、标签、分片位置、投递时间戳及受保护的消息体。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class MessageDetailDTO {

    /**
     * 消息全局唯一标识 ID。
     */
    private String msgId;

    /**
     * 消息物理位点 ID（offsetMsgId，可选）。
     */
    private String offsetMsgId;

    /**
     * 所属主题名称。
     */
    private String topic;

    /**
     * 消息标签 Tags。
     */
    private String tags;

    /**
     * 业务索引 Keys。
     */
    private String keys;

    /**
     * 消息体文本内容（已由 MessageBodyGuard 进行 UTF-8/Base64 解码与截断保护）。
     */
    private String body;

    /**
     * 原始消息体字节长度（bytes）。
     */
    private int bodySize = 0;

    /**
     * 客户端发送产生时间戳。
     */
    private long bornTimestamp = 0L;

    /**
     * 客户端生成来源主机地址（IP:PORT）。
     */
    private String bornHost;

    /**
     * Broker 存储落地时间戳。
     */
    private long storeTimestamp = 0L;

    /**
     * 存储该消息的 Broker 主机地址（IP:PORT）。
     */
    private String storeHost;

    /**
     * 分片队列编号。
     */
    private int queueId = 0;

    /**
     * 队列物理位点 Offset。
     */
    private long queueOffset = 0L;

    /**
     * 重试消费次数。
     */
    private int reconsumeTimes = 0;

    /**
     * 消息扩展用户与系统属性集合。
     */
    private Map<String, String> properties = new HashMap<>();

    public MessageDetailDTO() {
    }

    public MessageDetailDTO(String msgId, String topic) {
        this.msgId = msgId;
        this.topic = topic;
    }

    public MessageDetailDTO(String msgId, String offsetMsgId, String topic, String tags, String keys,
                            int queueId, long queueOffset, String bornHost, long bornTimestamp,
                            long storeTimestamp, int reconsumeTimes, int bodySize, String body,
                            boolean truncated, Map<String, ?> properties) {
        this.msgId = msgId;
        this.offsetMsgId = offsetMsgId;
        this.topic = topic;
        this.tags = tags;
        this.keys = keys;
        this.queueId = queueId;
        this.queueOffset = queueOffset;
        this.bornHost = bornHost;
        this.bornTimestamp = bornTimestamp;
        this.storeTimestamp = storeTimestamp;
        this.reconsumeTimes = reconsumeTimes;
        this.bodySize = bodySize;
        this.body = body;
        this.properties = new HashMap<>();
        if (properties != null) {
            properties.forEach((k, v) -> this.properties.put(String.valueOf(k), v != null ? String.valueOf(v) : null));
        }
    }

    public boolean isTruncated() {
        return body != null && body.contains("[Body Truncated:");
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getOffsetMsgId() {
        return offsetMsgId;
    }

    public void setOffsetMsgId(String offsetMsgId) {
        this.offsetMsgId = offsetMsgId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getBodySize() {
        return bodySize;
    }

    public void setBodySize(int bodySize) {
        this.bodySize = bodySize;
    }

    public long getBornTimestamp() {
        return bornTimestamp;
    }

    public void setBornTimestamp(long bornTimestamp) {
        this.bornTimestamp = bornTimestamp;
    }

    public String getBornHost() {
        return bornHost;
    }

    public void setBornHost(String bornHost) {
        this.bornHost = bornHost;
    }

    public long getStoreTimestamp() {
        return storeTimestamp;
    }

    public void setStoreTimestamp(long storeTimestamp) {
        this.storeTimestamp = storeTimestamp;
    }

    public String getStoreHost() {
        return storeHost;
    }

    public void setStoreHost(String storeHost) {
        this.storeHost = storeHost;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getQueueOffset() {
        return queueOffset;
    }

    public void setQueueOffset(long queueOffset) {
        this.queueOffset = queueOffset;
    }

    public int getReconsumeTimes() {
        return reconsumeTimes;
    }

    public void setReconsumeTimes(int reconsumeTimes) {
        this.reconsumeTimes = reconsumeTimes;
    }

    public Map<String, String> getProperties() {
        return properties != null ? properties : Collections.emptyMap();
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
    }
}
