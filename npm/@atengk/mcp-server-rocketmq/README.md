# @atengk/mcp-server-rocketmq

Apache RocketMQ 5 Model Context Protocol (MCP) 原生二进制启动包装器。提供零 JRE 依赖、毫秒级冷启动与 `npx` 即用接入能力。

## 快速使用

### 1. 使用 npx 秒开运行 (无需预装 Java 环境)

```bash
npx @atengk/mcp-server-rocketmq --rocketmq.namesrv-addr="127.0.0.1:9876"
```

### 2. 在 Claude Desktop 中配置

编辑 `claude_desktop_config.json`：

```json
{
  "mcpServers": {
    "rocketmq": {
      "command": "npx",
      "args": [
        "-y",
        "@atengk/mcp-server-rocketmq",
        "--rocketmq.namesrv-addr=127.0.0.1:9876",
        "--rocketmq.endpoints=127.0.0.1:8081"
      ]
    }
  }
}
```

### 3. 在 Cursor / Cline / Windsurf 中配置

```json
{
  "command": "npx",
  "args": [
    "-y",
    "@atengk/mcp-server-rocketmq",
    "--rocketmq.namesrv-addr=127.0.0.1:9876"
  ]
}
```

## 支持的平台

| 操作系统 | 架构 | 二进制平台包 |
| :--- | :--- | :--- |
| Windows | x64 | `@atengk/mcp-server-rocketmq-win32-x64` |
| Linux | x64 (glibc) | `@atengk/mcp-server-rocketmq-linux-x64` |
| Linux | arm64 (glibc / aarch64) | `@atengk/mcp-server-rocketmq-linux-arm64` |
| macOS | arm64 (Apple Silicon) | `@atengk/mcp-server-rocketmq-darwin-arm64` |

> 若当前运行环境不在上述列表中（例如 Linux musl / Alpine），包装器将在 `stderr` 输出明确指引，推荐使用官方 Docker 镜像 `ghcr.io/atengk/mcp-server-rocketmq:latest` 运行。

## 许可证

Apache License 2.0
