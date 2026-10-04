# 0005. 核心依赖基线：Spring Boot 4 + Spring AI 2 + RocketMQ 5

## 背景与上下文

在技术栈选型与基线对齐中，确认了项目全面基于现代化云原生与智能化技术栈。Spring AI 已正式迈入 2.x 时代（`2.0.1` 稳定版），基于 Spring Boot 4.x 运行时构建，全面演进了 MCP 协议支持（包括 `STREAMABLE` 流式传输与标准注解模型）。

## 架构决策

我们决定确立以下官方推荐的版本基线：
1. **运行时环境**：`JDK 21 (LTS)`，全面启用虚拟线程支持；
2. **微服务框架**：`Spring Boot 4.1.0`（`spring-boot-starter-parent`）；
3. **AI 基础设施**：`Spring AI BOM 2.0.1`，引入 `spring-ai-starter-mcp-server-webmvc` 与 `spring-ai-starter-mcp-server`；
4. **RocketMQ 客户端驱动**：
   - 运维管理驱动：`org.apache.rocketmq:rocketmq-tools:5.3.1`（Remoting 协议）；
   - 消息收发驱动：`org.apache.rocketmq:rocketmq-client-java:5.2.2`（gRPC 协议）；
5. **部署形态纯粹性**：纯粹定位为轻量级独立 MCP Server 承载体，不内置多余的测试容器编排，保持工程纯净。

## 影响与权衡

- **积极影响**：站在最新一代 Spring AI 2.x 与 Spring Boot 4.x 的技术前沿，具备最高的技术生命周期与现代 MCP 传输特性；
- **妥协与代价**：需保持与 Spring Boot 4 新特性的兼容，避免引入已过时的老旧第三方依赖。
