# 0004. 全协议要素（Tools/Resources/Prompts）与报文防爆截断体系

## 背景与上下文

Model Context Protocol (MCP) 是一套完整的 AI 交互协议，涵盖 Tools（工具调用）、Resources（只读上下文注入）与 Prompts（提示词工作流编排）三大支柱。单纯提供 Tools 会导致 AI 缺乏全局上下文视角且排障逻辑零散。此外，RocketMQ 消息体最大可达数兆（MB），直接向 AI 上下文输出过长字符串会引发 Token 爆表或解析超时。

## 架构决策

我们决定在 v1.0.0 中全面落地 MCP 全要素规范并内置上下文防爆体系：
1. **三维协议完整实现**：
   - **Tools (18项)**：覆盖集群拓扑、Topic/消费组生命周期、消息收发检索、积压排行与死信重投；
   - **Resources (3项)**：提供集群拓扑快照 (`rocketmq://cluster/topology`)、业务主题概览 (`rocketmq://topics`) 与服务运行状态 (`rocketmq://server/status`)；
   - **Prompts (2项)**：预置消费堆积深度排障 (`diagnose_consumer_lag`) 与集群健康巡检 (`cluster_health_check`) 专家工作流；
2. **消息体截断防护 (Message Body Guard)**：
   - 消息体解码策略采用 `UTF-8 优先 -> 失败降级 Base64`；
   - 施加全局可配置的最大字符截断阈值（默认 4KB），超出部分截断并显式追加 `[Body Truncated: original size X bytes]` 标记。

## 影响与权衡

- **积极影响**：大幅提升了 AI 智能体的交互效率与会话安全性，排障与巡检具备开箱即用的闭环能力；
- **妥协与代价**：对于超大报文需提供二次查询工具或提示用户关注截断标记。
