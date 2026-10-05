# 领域文档消费规范 (Domain Docs)

工程技能在探索和理解本代码库时，遵循以下领域文档消费规则：

## 探索代码库前必须调阅

- 仓库根目录下的 **`CONTEXT.md`**：阅读核心领域术语表与统一语言；
- 仓库根目录下的 **`docs/adr/`**：阅读与当前正在处理的模块相关的架构决策记录（ADR）。

如果以上文件或目录不存在，**静默继续推进**，切勿强行报错或在未达成共识前主动创建。`/domain-modeling` 技能（通过 `/grill-with-docs` 触发）会在实际解决概念或决策时懒加载创建它们。

## 目录结构

本项目为单上下文结构（Single-context repo）：

```text
.
├── CONTEXT.md                 # 领域术语统一字典（无实现细节）
├── docs/adr/                  # 架构决策记录（系统级 ADR）
│   ├── 0001-hybrid-client-architecture.md
│   ├── 0002-dual-layer-safety-guard.md
│   ├── 0003-single-jar-dual-mode-transport.md
│   ├── 0004-mcp-full-specification-and-body-truncation.md
│   ├── 0005-dependency-matrix-and-runtime-baseline.md
│   ├── 0006-containerization-and-release-pipeline.md
│   ├── 0007-package-namespace-and-maven-coordinates.md
│   └── 0008-client-lifecycle-hardening-and-resilience.md
└── src/                       # 业务源码
```

## 严格遵循统一领域语言

当输出中命名领域概念时（包括 Issue 标题、重构建议、设计假设或测试用例名称），必须使用 `CONTEXT.md` 中定义的规范术语，严禁使用词汇表中已显式标注 `_Avoid_` 的近义词。

如果所需的领域概念在术语表中尚未收录，应评估：这是本工程未曾采用的自创词汇（需避免），还是确实存在概念缺失（需通过 `/domain-modeling` 补充收录）。

## 主动暴露 ADR 冲突

如果您的技术方案或代码修改违背了已有 ADR 的决策，必须显式抛出冲突与原因，严禁隐式静默覆盖：

> _与 ADR-0001（Remoting/gRPC 混合双驱动架构）存在冲突——但值得重新审视，原因为……_
