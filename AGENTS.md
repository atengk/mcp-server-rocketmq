# 项目智能体行为与技能配置 (Agents Guide)

本文件是所有 AI 智能体（包括 Antigravity、Claude Code、Cursor 等）在维护和拓展 `mcp-server-rocketmq` 代码库时的行为准则与技术指引。

---

## 1. 项目概览与技术基线 (Baseline)

- **项目定位**：基于 Model Context Protocol (MCP) 与 Spring AI 构建的 Apache RocketMQ 5 智能化全生命周期控制面服务端。
- **技术栈核心**：
  - **语言与运行时**：`JDK 21`（充分利用虚拟线程 Virtual Threads 与模式匹配）；
  - **微服务框架**：`Spring Boot 4.1.0`（`spring-boot-starter-parent`）；
  - **AI 基础设施**：`Spring AI 2.0.1`（`spring-ai-bom`）；
  - **RocketMQ 驱动**：
    - 运维端：`org.apache.rocketmq:rocketmq-tools:5.3.1`（基于 Remoting 协议，承载 `DefaultMQAdminExt`）；
    - 消息端：`org.apache.rocketmq:rocketmq-client-java:5.2.2`（基于 gRPC 协议）；
  - **构建系统**：单模块 Maven (`pom.xml`)。

---

## 2. 核心架构不变式 (Architectural Invariants)

智能体在生成或修改代码时，必须严格遵守以下已确立的架构决策（ADR）：

1. **混合双驱动分工 (ADR 0001)**：
   - 集群拓扑、Broker 指标、Topic 增删改查、消费组状态、Lag 积压计算、消费位点重置一律走 `DefaultMQAdminExt`；
   - 调试测试消息发送与消费拉取走 `rocketmq-client-java` gRPC 端点；
2. **破坏性操作双层防呆 (ADR 0002)**：
   - `rocketmq_delete_topic`、`rocketmq_reset_consumer_offset`、`rocketmq_resend_dlq_message` 为破坏性工具；
   - 必须通过全局配置 `rocketmq.enable-destructive-tools` 激活，且方法入参中必须显式校验 `confirm == Boolean.TRUE`，否则直接拦截拒绝；
3. **stdio 模式标准输出绝对纯净 (ADR 0003)**：
   - 当启动参数包含 `--mcp.transport=stdio` 或配置为 stdio 时，`System.out` 仅用于流通 JSON-RPC 报文；
   - **严禁向 `System.out` 打印任何调试信息、日志或 Banner**！业务日志统一通过 SLF4J 记录，并由配置将其定向至 `System.err`；
4. **全协议要素落地与报文防爆 (ADR 0004)**：
   - 完整提供 18 项 Tools、3 项 Resources (`rocketmq://...`) 与 2 项专家 Prompts；
   - 所有返回消息体的操作，必须施加 4KB 防爆截断与 UTF-8/Base64 安全解码；
5. **管理客户端单例受管 (ADR 0005)**：
   - `DefaultMQAdminExt` 统一由 Spring 容器作为单例 Bean 生命周期受管，严禁在 Tool 方法内部频繁创建与销毁；
6. **双轨精准容器化与发版流水线 (ADR 0006)**：
   - Docker 镜像采用 Temurin JRE 21 Alpine 多阶段构建，强制以非 root 用户 (`mcp:mcp`, UID 10001) 运行；
   - 随 Tag 自动化发版，聚焦双轨精准分发：GitHub Release 挂载 Fat Jar 附件及 SHA-256、GHCR 发布多架构容器镜像；摒弃非必要的 Maven Packages 发布；
7. **包命名空间与 Maven 坐标规范 (ADR 0007)**：
   - 全局包名统一规范为 `io.github.atengk.mcp.rocketmq`；Maven GAV 坐标规范为 `io.github.atengk:mcp-server-rocketmq`；
8. **客户端生命周期自愈与韧性加固 (ADR 0008)**：
   - Remoting Admin 客户端引入 5000ms 重连冷却防抖与 ACL RPCHook 自动注入；
   - gRPC 消息客户端具备 channel 损坏自愈重建机制；
   - 消息体严格执行 4MB 前置体积防爆防御；创建 Topic 前置强校验活跃 Master 存活。

---

## 3. 代码质量与注释规范 (Code & Comments)

- **文件/类级标准注释**：
  新建 Java 源码时必须严格包含 Doc 注释，包含职责说明、作者（固定为 `Ateng`）和当天日期（`@since 2026-10-04`）：
  ```java
  /**
   * RocketMQ 运维管理服务封装，提供集群感知与Topic生命周期操作。
   *
   * @author Ateng
   * @since 2026-10-04
   */
  ```
- **空安全契约**：
  - 集合查询接口无匹配时，统一返回空集合（如 `Collections.emptyList()`），严禁返回 `null`；
  - 单实体查询统一使用前置卫语句进行空指针拦截；
- **异常与日志标准**：
  - 严禁空 `catch` 块；
  - 统一通过 SLF4J 记录日志，严禁直接使用 `System.out.println`。

---

## 4. Agent 技能配置 (Agent Skills)

### 任务与工单跟踪 (Issue tracker)

使用 GitHub Issues 管理任务与规范，通过 `gh` CLI 操作。详见 [docs/agents/issue-tracker.md](./docs/agents/issue-tracker.md)。

### 分流标签 (Triage labels)

使用 5 个标准分流角色标签（`needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human`、`wontfix`）。详见 [docs/agents/triage-labels.md](./docs/agents/triage-labels.md)。

### 领域文档 (Domain docs)

采用单上下文结构（根目录 [CONTEXT.md](./CONTEXT.md) 与 [docs/adr/](./docs/adr/)）。详见 [docs/agents/domain.md](./docs/agents/domain.md)。
