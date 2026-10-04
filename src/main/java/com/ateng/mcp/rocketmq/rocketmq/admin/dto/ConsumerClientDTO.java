package com.ateng.mcp.rocketmq.rocketmq.admin.dto;

/**
 * 在线消费客户端连接明细数据传输对象。
 * 承载单个客户端实例的通信地址、客户端标识、开发语言与版本。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class ConsumerClientDTO {

    /**
     * 客户端唯一标识 ID（通常为 IP@PID 或自定义标识）。
     */
    private String clientId;

    /**
     * 客户端网络通信地址（IP:PORT）。
     */
    private String clientAddr;

    /**
     * 客户端开发语言（如 JAVA、CPP、GOLANG 等）。
     */
    private String language;

    /**
     * 客户端版本号（数字或版本字符串）。
     */
    private int version;

    public ConsumerClientDTO() {
    }

    public ConsumerClientDTO(String clientId, String clientAddr, String language, int version) {
        this.clientId = clientId;
        this.clientAddr = clientAddr;
        this.language = language;
        this.version = version;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientAddr() {
        return clientAddr;
    }

    public void setClientAddr(String clientAddr) {
        this.clientAddr = clientAddr;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
