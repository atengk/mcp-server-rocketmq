# 0001. 混合通信架构：Remoting 运维管理与 gRPC 消息收发双驱动

## 背景与上下文

在 Apache RocketMQ 5.x 体系中，官方推荐的轻量级云原生 SDK（`rocketmq-client-java` 基于 gRPC 与无状态 Proxy）专注于高性能消息收发，并不提供针对集群拓扑感知、Topic 与消费组生命周期管控、消费积压（Lag）分析等深层 Admin API。如果单纯使用 5.x gRPC 客户端，无法满足 MCP 服务端对集群深度运维与全生命周期控制面的需求。

## 架构决策

我们决定在服务端采用**混合双驱动通信架构**：
1. **运维管理面 (Admin Operations)**：采用经典 Remoting 协议驱动（基于 `DefaultMQAdminExt`），直接连接 NameServer / Broker，负责集群拓扑探查、Topic 增删改查、消费组状态监控、位点重置与积压计算；
2. **数据消息面 (Messaging Operations)**：采用 RocketMQ 5.x 原生 gRPC 协议驱动（基于 `rocketmq-client-java`），通过 RocketMQ 5.x Proxy 端点负责测试消息发送与消费验证。

## 影响与权衡

- **积极影响**：实现了全生命周期控制面（Control Plane）与现代云原生消息收发（Data Plane）的兼顾，功能完备度达到企业级管理控制台级别；
- **妥协与代价**：服务端部署时需同时配置 NameServer 地址（Remoting）与 Proxy 服务端点（gRPC），连接凭证需统一双向适配。
