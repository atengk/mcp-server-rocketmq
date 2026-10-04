package com.ateng.mcp.rocketmq.rocketmq.messaging;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import com.ateng.mcp.rocketmq.rocketmq.messaging.dto.SendMessageResultDTO;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 5.x gRPC 消息客户端服务默认实现。
 * 封装 ClientServiceProvider 与 Producer 生命周期，支持发送普通、分区顺序与定时延时消息。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Service
public class DefaultMessagingClientService implements MessagingClientService, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DefaultMessagingClientService.class);

    private final RocketmqProperties properties;
    private final Object lock = new Object();
    private volatile Producer producer;

    public DefaultMessagingClientService(RocketmqProperties properties) {
        this.properties = properties;
    }

    @Override
    public SendMessageResultDTO sendMessage(String topic, String body, String tag, String keys,
                                            String messageGroup, Long deliveryTimestamp) throws Exception {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic 不能为空");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body 不能为空");
        }

        String cleanTopic = topic.trim();
        String cleanTag = (tag != null && !tag.isBlank()) ? tag.trim() : null;
        String cleanKeys = (keys != null && !keys.isBlank()) ? keys.trim() : null;
        String cleanGroup = (messageGroup != null && !messageGroup.isBlank()) ? messageGroup.trim() : null;

        ClientServiceProvider provider = getClientServiceProvider();
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        MessageBuilder messageBuilder = provider.newMessageBuilder()
                .setTopic(cleanTopic)
                .setBody(bodyBytes);

        if (cleanTag != null) {
            messageBuilder.setTag(cleanTag);
        }
        if (cleanKeys != null) {
            messageBuilder.setKeys(cleanKeys);
        }
        if (cleanGroup != null) {
            messageBuilder.setMessageGroup(cleanGroup);
        }
        if (deliveryTimestamp != null && deliveryTimestamp > 0) {
            messageBuilder.setDeliveryTimestamp(deliveryTimestamp);
        }

        Message message = messageBuilder.build();
        Producer activeProducer = getOrCreateProducer();

        log.info("Sending message via gRPC to topic: {}, tag: {}, keys: {}, group: {}, deliveryTimestamp: {}",
                cleanTopic, cleanTag, cleanKeys, cleanGroup, deliveryTimestamp);

        SendReceipt receipt = activeProducer.send(message);
        String messageId = receipt != null && receipt.getMessageId() != null
                ? receipt.getMessageId().toString()
                : "UNKNOWN";

        log.info("Successfully sent message to topic: {}, messageId: {}", cleanTopic, messageId);

        return new SendMessageResultDTO(
                messageId,
                cleanTopic,
                "SUCCESS",
                cleanTag,
                cleanKeys,
                cleanGroup,
                deliveryTimestamp,
                bodyBytes.length
        );
    }

    /**
     * 获取或延迟构建 gRPC Producer 实例。
     *
     * @return 激活状态的 Producer 实例
     * @throws Exception 当初始化配置或建立连接失败时抛出
     */
    protected Producer getOrCreateProducer() throws Exception {
        if (producer == null) {
            synchronized (lock) {
                if (producer == null) {
                    producer = createProducer();
                }
            }
        }
        return producer;
    }

    /**
     * 构建 Producer 单例，支持子类测试覆写以实现 Mock 注入。
     *
     * @return 新建的 Producer 实例
     * @throws Exception 当构造失败时抛出
     */
    protected Producer createProducer() throws Exception {
        ClientServiceProvider provider = getClientServiceProvider();
        String endpoints = (properties != null && properties.getEndpoints() != null)
                ? properties.getEndpoints().trim()
                : "127.0.0.1:8081";

        ClientConfigurationBuilder configBuilder = ClientConfiguration.newBuilder()
                .setEndpoints(endpoints);

        if (properties != null && properties.getAccessKey() != null && properties.getSecretKey() != null
                && !properties.getAccessKey().isBlank() && !properties.getSecretKey().isBlank()) {
            configBuilder.setCredentialProvider(new StaticSessionCredentialsProvider(
                    properties.getAccessKey().trim(), properties.getSecretKey().trim()));
        }

        ClientConfiguration clientConfiguration = configBuilder.build();
        log.info("Initializing RocketMQ 5.x gRPC Producer with endpoints: {}", endpoints);

        return provider.newProducerBuilder()
                .setClientConfiguration(clientConfiguration)
                .build();
    }

    /**
     * 获取 ClientServiceProvider 提供器实例，支持子类测试覆写。
     *
     * @return ClientServiceProvider 实例
     */
    protected ClientServiceProvider getClientServiceProvider() {
        return ClientServiceProvider.loadService();
    }

    @Override
    public void destroy() {
        synchronized (lock) {
            if (producer != null) {
                try {
                    producer.close();
                    log.info("RocketMQ 5.x gRPC Producer closed successfully.");
                } catch (Exception e) {
                    log.warn("Error while closing RocketMQ 5.x gRPC Producer: {}", e.getMessage());
                } finally {
                    producer = null;
                }
            }
        }
    }
}
