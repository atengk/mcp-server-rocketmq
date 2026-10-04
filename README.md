# mcp-server-rocketmq

<p align="center">
  <strong>🚀 Model Context Protocol (MCP) server for Apache RocketMQ (5.x & 4.x)</strong>
</p>

<p align="center">
  <a href="https://github.com/atengk/mcp-server-rocketmq/actions/workflows/ci.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/atengk/mcp-server-rocketmq/ci.yml?branch=main&label=CI&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://github.com/atengk/mcp-server-rocketmq/releases">
    <img src="https://img.shields.io/github/v/release/atengk/mcp-server-rocketmq?style=flat-square" alt="Release" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=flat-square" alt="License" />
  </a>
  <a href="./CONTRIBUTING.md">
    <img src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square" alt="PRs Welcome" />
  </a>
</p>

---

## 📖 项目简介

`mcp-server-rocketmq` 是基于 [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) 开放协议标准构建的 Apache RocketMQ 智能化服务，旨在使大语言模型智能体能够连接并操作 Apache RocketMQ (5.x & 4.x) 集群。

> 📌 **项目状态**：
> 当前仓库处于**基础工程初始化阶段**。具体的技术架构、功能定义、MCP 工具契约与接入指南将在需求对齐与技术设计讨论完成后正式发布。

---

## 📂 仓库目录结构

```text
.
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md           # Bug 缺陷反馈模版
│   │   └── feature_request.md      # 新特性建议模版
│   ├── workflows/
│   │   ├── ci.yml                  # 业务构建与 PR 标题校验流水线
│   │   └── release.yml             # 自动化发版、生成更新日志与分发流水线
│   └── PULL_REQUEST_TEMPLATE.md    # Pull Request 提交模版
├── .cliff.toml                     # git-cliff 变更日志提取与分类配置
├── .dockerignore                   # Docker 镜像构建忽略清单
├── .editorconfig                   # 跨编辑器编码规范
├── .gitattributes                  # 跨平台换行符归一化 (强制 LF)
├── .gitignore                      # 跨语言通用忽略清单
├── CONTRIBUTING.md                 # 贡献指南与 Commit 规范
├── LICENSE                         # 开源许可证 (Apache-2.0)
└── README.md                       # 项目主文档
```

---

## 🤝 参与贡献

欢迎提出任何改进建议或参与贡献！提交代码前请先查阅 [贡献指南 (CONTRIBUTING.md)](./CONTRIBUTING.md)。

---

## 📄 开源许可证

本项目基于 [Apache License 2.0](./LICENSE) 协议开源。
