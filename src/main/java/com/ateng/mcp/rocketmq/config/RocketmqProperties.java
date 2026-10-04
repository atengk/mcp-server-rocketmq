package com.ateng.mcp.rocketmq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Apache RocketMQ 服务端核心连接与安全防护配置属性。
 * 映射 rocketmq.* 前缀下的各项配置，支持命令行参数与环境变量注入。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@ConfigurationProperties(prefix = "rocketmq")
public class RocketmqProperties {

    /**
     * RocketMQ NameServer 集群地址，多个地址以分号分隔。
     */
    private String namesrvAddr = "127.0.0.1:9876";

    /**
     * RocketMQ 5.x gRPC Proxy 服务端点地址。
     */
    private String endpoints = "127.0.0.1:8081";

    /**
     * ACL 访问认证密钥 AccessKey（可选）。
     */
    private String accessKey;

    /**
     * ACL 访问认证密钥 SecretKey（可选）。
     */
    private String secretKey;

    /**
     * 全局只读守卫开关，默认关闭。若开启则禁止任何写操作与元数据变更。
     */
    private boolean readOnly = false;

    /**
     * 破坏性高危操作工具全局激活开关（双层防呆第一层），默认关闭。
     */
    private boolean enableDestructiveTools = false;

    public String getNamesrvAddr() {
        return namesrvAddr;
    }

    public void setNamesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
    }

    public String getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(String endpoints) {
        this.endpoints = endpoints;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isEnableDestructiveTools() {
        return enableDestructiveTools;
    }

    public void setEnableDestructiveTools(boolean enableDestructiveTools) {
        this.enableDestructiveTools = enableDestructiveTools;
    }
}
