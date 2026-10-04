# 0003. 单一可执行 Jar 双模传输与标准输出纯净化

## 背景与上下文

Model Context Protocol (MCP) 规范要求在 Stdio（标准 I/O）传输模式下，进程的标准输出（`System.out`）必须 100% 保持为纯净的 JSON-RPC 报文。Spring Boot 默认在启动时会向标准输出打印 ASCII Banner、启动日志及诊断信息，这会导致 Claude Desktop / Cursor 等 MCP 客户端在解析 JSON 报文时发生格式解析崩溃。与此同时，服务端又需支持基于 WebMVC 的 SSE 网络长连接通信。

## 架构决策

我们决定采用**单一 Fat Jar 自适应净化架构**：
1. **传输自适应**：通过启动参数 `--mcp.transport=stdio`（或环境变量 `MCP_TRANSPORT=stdio`）自动判断运行模式；
2. **Web 容器动态关闭**：在 stdio 模式下，设置 `spring.main.web-application-type=none`，关闭内嵌 Tomcat/Jetty 容器以降低内存占用与启动开销；
3. **标准输出纯净化 (Stdout Purification)**：在 stdio 模式下，彻底关闭 Spring Boot Banner，并将 Logback/SLF4J 的 ConsoleAppender 输出流强制重定向至标准错误流 (`System.err`) 或本地落盘文件，确保 `System.out` 仅流通 MCP 协议报文；
4. **SSE 模式默认就绪**：若未指定 stdio 模式，服务默认以 WebMVC 模式启动并在配置端口监听 SSE 连接。

## 影响与权衡

- **积极影响**：只需打包并发布单一 Jar 文件或 Docker 镜像，即可同时胜任本地桌面客户端伴生进程与企业中心化微服务集群部署；
- **妥协与代价**：需要在 Spring Boot 初始化早期通过 `EnvironmentPostProcessor` 或自定义 `ApplicationListener` 精确介入日志框架流向的控制。
