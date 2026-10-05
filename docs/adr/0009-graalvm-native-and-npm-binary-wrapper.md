# 0009. GraalVM Native 原生二进制编译与 npm 多包分发架构

## 背景与上下文

在推进 `mcp-server-rocketmq` 的端侧集成与推广体验时，用户在以 Stdio Transport 模式（如 Claude Desktop、Cursor 等宿主集成）启动 MCP 服务端时面临以下工程挑战：
1. **JVM 运行时冷启动与依赖包袱**：传统 Spring Boot 依赖宿主机预装 JDK 21，启动时延（约 1.5s ~ 2.5s）对交互式 CLI / Stdio 工具体验不够轻盈；
2. **Quarkus 迁移的重写税与生态断层**：若迁移至 Quarkus，虽然同属 GraalVM 友好型框架，但需推翻重写现有的 18 项 Tools、3 项 Resources、2 项 Prompts 以及 138 项测试用例，且 Quarkus 并无针对 RocketMQ Remoting 运维端（`DefaultMQAdminExt`）的官方扩展，无法自动解决底层反射与 Netty 元数据配置问题；
3. **分发门槛与生态协同**：AI 宿主生态（尤其是 Claude Desktop、Cursor、Cline）普遍支持基于 `npx @scope/pkg` 的零配置拉起方式。

## 架构决策

我们决定确立 **“Spring Boot 4 Native AOT + Tracing Agent 元数据捕获 + npm 多包原生包装器”** 的联合分发架构：

1. **框架宿主坚守与 Native AOT 激活**：
   - 拒绝重写为 Quarkus，全面保留现有的 Spring Boot 4.1.0 与 Spring AI 2.0.1 技术栈及全部业务与测试资产；
   - 激活 `org.graalvm.buildtools:native-maven-plugin`，基于 Spring Boot 4 原生 AOT 引擎生成跨平台原生二进制（Native Binary，脱离 JRE 依赖，冷启动降至毫秒级，内存开销降至 30MB~50MB 级）。
2. **RocketMQ 混合双驱动的 AOT 元数据治理策略**：
   - **5.x gRPC 消息客户端**：利用 RocketMQ 5 官方的 Protobuf/gRPC 静态规范原生支持 Native Image；
   - **Remoting 运维管理客户端**：利用现有的 138 项高覆盖率单元与集成测试，借助 `native-image-agent` 自动运行捕获 95% 以上的 `reflect-config.json` 与 `resource-config.json`，辅以 Spring `RuntimeHintsRegistrar` 对关键类（如 `RemotingCommand`、`CustomHeader`、ACL RPCHook）进行显式防御性注册。
3. **npm 多包结构 (Optional Dependencies) 分发模式**：
   - 采用业界标准的分包模式（对标 `esbuild`、`swc`、`@prisma/engines`）：
     - **主引导包**：`@atengk/mcp-server-rocketmq`（内含极简 Node.js 启动器 `bin/cli.js`，通过 `process.platform` 与 `process.arch` 解析并以 `{ stdio: 'inherit' }` 调度目标平台原生可执行文件）；
     - **平台二进制包**：作为 optionalDependencies 分发，包含 `@atengk/mcp-server-rocketmq-win32-x64`、`@atengk/mcp-server-rocketmq-linux-x64`、`@atengk/mcp-server-rocketmq-darwin-arm64`；用户通过 `npx` 执行时，npm 仅按需拉取当前平台二进制。
4. **硬性平台边界与 Stdio 纯净性守卫**：
   - 严格支持 `win32-x64`、`linux-x64`、`darwin-arm64` 三大主流平台；若遇到不支持的环境或本地运行库缺失，包装器在 `stderr` 输出明确错误指引并以非零码退出，严禁在标准输出打印非 JSON-RPC 字符，绝不引发 Stdio 管道污染。
5. **CI/CD 三轨联合发版矩阵**：
   - 在 `.github/workflows/release.yml` 中新增基于 GitHub Actions 跨平台矩阵（`ubuntu-latest`、`windows-latest`、`macos-latest`）的构建 Job，随 Git Tag 自动化并行发布：
     - 轨 1：GitHub Release 挂载 Fat Jar 与 SHA-256；
     - 轨 2：GHCR 多架构容器镜像；
     - 轨 3：npm 原生二进制多包发布（支持 `npx @atengk/mcp-server-rocketmq` 零依赖即用）。

## 影响与权衡

- **积极影响**：
  - 用户体验大幅跃升：支持 `npx @atengk/mcp-server-rocketmq` 一键秒开，无需宿主机配置 Java 环境；
  - 零代码推翻：完全复用 100% 既有 Spring AI 工具链与测试资产，无 Quarkus 迁移成本；
  - 规范分发：多包架构将单平台下载体积控制在 30MB~50MB，避免单包全平台打包含量膨胀。
- **妥协与代价**：
  - CI/CD 构建时长增加：Native Image 跨平台静态编译各平台需耗时 3~8 分钟；
  - 维护边界收敛：对 Linux musl/Alpine 等小众平台保持阻断，小众环境仍指引使用 Docker 或 Fat Jar。
