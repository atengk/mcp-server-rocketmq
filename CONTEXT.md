# mcp-server-rocketmq 领域术语表 (Context Glossary)

本项目作为基于 Model Context Protocol (MCP) 与 Spring AI 构建的 Apache RocketMQ 智能化连接服务，旨在为大语言模型智能体提供具备全生命周期控制面能力的标准化上下文与工具集。

## 核心领域语言 (Language)

**MCP Server (MCP 服务端)**:
遵循 Model Context Protocol 规范向 AI 宿主暴露 Tools、Resources 与 Prompts 的服务载体。
_Avoid_: Agent, AI 客户端, 代理服务

**Admin Client (运维管理驱动)**:
基于 Apache RocketMQ Remoting 协议与 `DefaultMQAdminExt` 构建的管理客户端，专门承载集群拓扑探针、Topic/消费组生命周期管理及积压分析。
_Avoid_: 消息客户端, Producer, Consumer

**Messaging Client (消息收发驱动)**:
基于 Apache RocketMQ 5.x gRPC 协议（`rocketmq-client-java`）构建的消息客户端，负责测试消息生产与消费拉取验证。
_Avoid_: AdminExt, 管理客户端

**Read-Only Guard (只读守卫)**:
全局安全拦截机制，当开启时直接拒绝任何对集群产生写操作或元数据变更的 MCP Tool 调用。
_Avoid_: 权限校验, ACL, 鉴权中心

**Dual-Layer Guard (双层防呆机制)**:
针对破坏性工具（如删除 Topic、重置位点）建立的安全防护体系，必须同时满足启动级环境变量激活与调用级 `confirm: true` 参数显式确认方可执行。
_Avoid_: 单重拦截, 普通校验

**Destructive Tool (破坏性工具)**:
能够删除数据、修改集群元数据或大幅改变消费状态的 MCP 工具（如 `rocketmq_delete_topic`、`rocketmq_reset_consumer_offset`、`rocketmq_resend_dlq_message`），受双层防呆机制约束。
_Avoid_: 普通写工具, 危险指令

**Stdio Transport (标准 I/O 传输模式)**:
本地进程间通信形态，依赖系统的 `stdin` 与 `stdout` 传输 JSON-RPC 报文。要求服务端的 `stdout` 保持 100% 纯净，严禁任何业务日志与启动 Banner 输出至标准输出。
_Avoid_: 命令行交互, 控制台输出

**SSE Transport (Server-Sent Events 传输模式)**:
基于 HTTP 协议的长连接网络端点通信形态，适用于远程微服务或容器化网络部署。
_Avoid_: WebSocket, 普通 HTTP 轮询

**MCP Resource (MCP 资源)**:
MCP 协议提供的只读上下文快照（如 `rocketmq://cluster/topology`），供 AI 宿主直接挂载至会话上下文而无需执行工具调用。
_Avoid_: 静态配置, 本地文件

**MCP Prompt (MCP 提示词模版)**:
MCP 协议提供的预置专家排障或巡检工作流（如 `diagnose_consumer_lag`），供用户或宿主一键触发多步智能体编排。
_Avoid_: 系统提示词, 普通文本

**Message Body Guard (消息体截断防护)**:
消息检索时的安全防爆机制。优先以 UTF-8 尝试解析，解析失败自动降级为 Base64，且对超过阈值（默认 4KB）的内容进行安全截断并附带元数据提示。
_Avoid_: 文本解析器, 字符串截取

**Consumer Lag (消费积压)**:
消费组当前已消费位点（Consume Offset）与 Broker 最大消息位点（Max Offset）之间的差值，是度量消费延迟的核心指标。
_Avoid_: 消息阻塞, 消息延迟

**Message Trace (消息轨迹)**:
包含消息在 Producer 发送、Broker 存储、Consumer 投递及消费耗时等全生命周期流转的时间线记录。
_Avoid_: 日志追踪, 调用链

**System Topic (系统主题)**:
RocketMQ 内部保留的主题（如以 `%SYS%`、`TBW102`、`BenchmarkTest` 开头的主题），在 MCP 工具输出中默认被过滤屏蔽。
_Avoid_: 内部消息, 内置队列

**Multi-Stage Container Build (多阶段容器构建)**:
分离源码编译与生产运行环境的容器构建范式。前置阶段基于 Maven 编译打包，运行时阶段采用极简 JRE 基础镜像并以非 root 专有低特权用户启动。
_Avoid_: 单阶段粗暴打包, 宿主直接复制

**GHCR Distribution (GHCR 镜像分发)**:
依托 GitHub Container Registry 进行多架构（`linux/amd64`, `linux/arm64`）镜像托管与分发的标准化渠道。
_Avoid_: 私有镜像站, 手工镜像分发

**Release Artifact (发行版附件构件)**:
随 Git Tag 自动化发版流程构建的可执行单一 Fat Jar 及其 SHA-256 完整性校验文件，供用户直接下载部署运行。
_Avoid_: 源码压缩包, 中间构建包

**Multi-Instance MCP Configuration (多实例/多环境 MCP 配置)**:
在同一 AI 宿主客户端中并列挂载多个独立运行的 MCP Server 进程或网络端点（如 `rocketmq-dev`、`rocketmq-prod`），实现单会话跨环境统一受控运维与集群对比。
_Avoid_: 多开客户端, 手动切配置

**Tiered Safety Policy (分级安全策略)**:
针对开发、测试与生产等不同生命周期集群实施的差异化安全准入矩阵。开发环境赋予写操作与防呆确认工具，生产环境强制施加全局只读守卫与硬锁定。
_Avoid_: 一刀切安全, 无序权限

