package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

/**
 * 队列路由分布数据传输对象。
 * 承载单个 Broker 分片上的读写队列配额及权限配置。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class QueueDataDTO {

    /**
     * 队列所在的 Broker 名称。
     */
    private String brokerName;

    /**
     * 读队列数量。
     */
    private int readQueueNums;

    /**
     * 写队列数量。
     */
    private int writeQueueNums;

    /**
     * 读写权限标记（6 为读写正常，4 为只读，2 为只写）。
     */
    private int perm;

    public QueueDataDTO() {
    }

    public QueueDataDTO(String brokerName, int readQueueNums, int writeQueueNums, int perm) {
        this.brokerName = brokerName;
        this.readQueueNums = readQueueNums;
        this.writeQueueNums = writeQueueNums;
        this.perm = perm;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getReadQueueNums() {
        return readQueueNums;
    }

    public void setReadQueueNums(int readQueueNums) {
        this.readQueueNums = readQueueNums;
    }

    public int getWriteQueueNums() {
        return writeQueueNums;
    }

    public void setWriteQueueNums(int writeQueueNums) {
        this.writeQueueNums = writeQueueNums;
    }

    public int getPerm() {
        return perm;
    }

    public void setPerm(int perm) {
        this.perm = perm;
    }
}
