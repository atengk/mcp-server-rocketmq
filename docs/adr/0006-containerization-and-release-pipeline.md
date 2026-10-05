# 0006. 容器化多阶段构建与双轨精准发版流水线

## 背景与上下文

为了降低大语言模型宿主（如 Claude Desktop、Cursor 等）及云原生基础设施集成 `mcp-server-rocketmq` 的接入与部署成本，服务端需要标准化容器交付与自动化发版流水线。
在分发场景中存在以下需求与挑战：
1. **多环境容器交付**：支持在无本地 JDK 构建环境的宿主上一键通过 Docker 构建并运行，并满足不同 CPU 架构（x86_64 与 ARM64/Apple Silicon）的部署诉求；
2. **容器安全基线**：杜绝以 root 用户身份在生产容器内运行 Java 进程，并合理配置 JVM 容器内存感知；
3. **外部网络互联**：容器化运行时需要灵活穿透宿主机或直连远程 RocketMQ 集群；
4. **多渠道软件分发**：作为独立服务端应用程序，精准提供 GitHub Release 可执行 Fat Jar 附件下载及 GHCR 官方多架构镜像托管。

## 架构决策

我们确立了以下交付与发布架构决策：

1. **多阶段容器构建规范 (Multi-Stage Build)**：
   - **构建阶段**：基于 `maven:3.9-eclipse-temurin-21-alpine` 离线预载依赖并编译打包应用 Fat Jar，屏蔽宿主环境差异；
   - **运行阶段**：基于 `eclipse-temurin:21-jre-alpine` 最小化运行环境，创建 UID/GID 10001 的非特权系统用户 `mcp` 运行；
   - **信号与调优**：使用 `exec java` 作为 PID 1 启动，保障容器停止时 `SIGTERM` 信号直接透传至 Spring Boot 触发优雅停机；默认注入 `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`。
2. **轻量自聚焦编排与网络穿透 (docker-compose)**：
   - `docker-compose.yml` 聚焦于 `mcp-server-rocketmq` 服务自身，降低维护负担；
   - 显式声明 `extra_hosts: host.docker.internal:host-gateway`，默认配置 `ROCKETMQ_NAMESRV_ADDR=host.docker.internal:9876`，无缝连通宿主机 RocketMQ 服务，同时通过 `.env` 支持远程集群覆盖。
3. **双轨精准自动化发版流水线 (GitHub Actions Release Workflow)**：
   - **触发机制**：基于语义化标签 `v*`（如 `v1.0.0`）推送或 `workflow_dispatch` 手动触发；
   - **语义化版本对齐**：流水线内动态执行 `mvn versions:set` 剔除 `-SNAPSHOT` 后缀并对齐为 Tag 目标版本；
   - **双轨分发矩阵**：
     1. **GitHub Release**：基于 `git-cliff` 自动提取 Changelog，挂载可执行 Fat Jar 与安全校验清单（`checksums.txt`）；
     2. **GHCR (GitHub Container Registry)**：通过 Docker Buildx + QEMU 构建并推送 `linux/amd64` 与 `linux/arm64` 双架构镜像至 `ghcr.io`；
     *(注：本项目定位为独立服务端应用而非 SDK 依赖库，特意裁撤 GitHub Packages Maven 构件发布，避免过度设计与权限摩擦)*。

## 影响与权衡

- **积极影响**：
  - 开发者与终端用户可直接拉取 GHCR 镜像或下载 Fat Jar 即可运行，达到分钟级集成体验；
  - 生产镜像尺寸保持在 200MB 以内，满足最小特权安全合规；
  - 多架构自动编译消除跨平台芯片兼容性问题。
- **妥协与代价**：
  - 多架构镜像构建（尤其是 QEMU 模拟跨架构编译）会在 CI 中增加若干分钟的构建时间。
