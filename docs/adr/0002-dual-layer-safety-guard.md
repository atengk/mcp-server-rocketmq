# 0002. 破坏性操作的双层防呆安全机制

## 背景与上下文

在提供全生命周期控制面能力时，包含删除主题（`rocketmq_delete_topic`）以及重置消费点位（`rocketmq_reset_consumer_offset`）等破坏性动作。考虑到大语言模型具有生成幻觉与理解歧义的风险，如果在生产或核心测试环境中直接放开此类工具，极易引发严重的数据丢失与业务中断。

## 架构决策

我们决定为所有破坏性操作建立**双层防呆安全机制 (Dual-Layer Guard)**：
1. **启动级环境变量守卫 (Environment Guard)**：默认关闭破坏性工具。必须在服务启动时显式配置环境变量 `ROCKETMQ_ENABLE_DESTRUCTIVE_TOOLS=true`（或命令行参数 `--rocketmq.enable-destructive-tools=true`），相应工具才会在 MCP 协议中暴露或允许执行；
2. **调用级参数显式确认 (Parameter Confirmation)**：破坏性工具的入参中强制包含布尔型 `confirm` 参数（如 `confirm: true`）。若调用时未传递或传值为 `false`，工具将直接短路拦截并返回安全警示。

## 影响与权衡

- **积极影响**：杜绝了 AI 智能体在未授权环境下自主误删元数据或随意篡改消费进度，保障企业资产安全；
- **妥协与代价**：在自动化脚本或宿主智能体执行时需要多一步确认交互，略微增加客户端编排复杂度。
