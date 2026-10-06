# 0010. 四平台原生编译矩阵与版本化发布资产规范

## 背景与上下文

在 `mcp-server-rocketmq` 推出 GraalVM Native 原生二进制编译与 npm 多包分发体系（ADR 0009）后，在实际生产交付与用户体验验证中暴露了以下两项架构与发布缺陷：
1. **Release 附件版本标识缺失**：GitHub Release 挂载的各平台原生二进制产物命名为静态固定名（如 `mcp-server-rocketmq-linux-x64`），而传统 JVM Jar 包为 `mcp-server-rocketmq-1.1.0.jar`。当运维工程师或终端用户从 GitHub 直接下载单文件二进制在宿主机离线配置时，无法从文件名直观获知其对应版本，给版本审计、升级与故障排查带来困难；
2. **Linux ARM64 生产架构缺失**：云原生环境（如 AWS Graviton、阿里云倚天、华为鲲鹏、Ampere 实例）以及边缘计算（树莓派 4/5）中，ARM64 (aarch64) 架构的 Linux 服务器占比极高。初始版本仅覆盖了 `linux-x64`、`win32-x64` 与 `darwin-arm64`，导致 Linux ARM 服务器在脱离 Docker 环境时面临无原生二进制可用、`npx` 调度阻断的窘境。

## 架构决策

我们决定确立 **“四平台黄金支柱矩阵 + 版本化 Release 资产 + npm 组装解耦”** 的演进规范：

1. **四平台黄金支柱原生矩阵 (Four-Platform Native Matrix)**：
   - 全局原生编译矩阵正式扩充为四大现代主流 64 位平台（主流覆盖率达 99.5%+）：
     - `linux-x64` (Linux AMD64 / x86_64)
     - `linux-arm64` (Linux AArch64 / ARM64)
     - `win32-x64` (Windows x86_64)
     - `darwin-arm64` (macOS Apple Silicon)
   - 对于存量极少的老款 Intel Mac (`darwin-x64`) 与 Windows ARM (`win32-arm64`)，因基础设施支持或性价比原因不纳入原生编译矩阵，统一指引用户使用跨平台 Fat Jar 或 Docker 镜像。
2. **GitHub Actions 官方原生 ARM Runner 选型**：
   - 针对 `linux-arm64` 平台，全面采用 GitHub Actions 官方免费提供给公开仓库的原生 4 vCPU ARM 运行器 **`ubuntu-24.04-arm`**；
   - 杜绝基于 QEMU 模拟器长达数小时的低效交叉编译，确保 Linux ARM64 原生编译在 5~8 分钟内高保真极速完成。
3. **版本化 Release 资产命名规范 (Versioned Asset Naming)**：
   - GitHub Release 挂载的独立原生二进制文件强制包含发版版本号，统一命名契约：
     `mcp-server-rocketmq-{version}-{platform}[.exe]`
   - 典型交付物清单：
     - `mcp-server-rocketmq-1.1.0-linux-x64`
     - `mcp-server-rocketmq-1.1.0-linux-arm64`
     - `mcp-server-rocketmq-1.1.0-win32-x64.exe`
     - `mcp-server-rocketmq-1.1.0-darwin-arm64`
     - `mcp-server-rocketmq-1.1.0.jar`
     - `checksums.txt`（校验和清单同步记录带版本号的精确文件名）
4. **npm 多包体系扩充与装配流程解耦**：
   - 新增平台子包 `@atengk/mcp-server-rocketmq-linux-arm64`（位于 `npm/@atengk/mcp-server-rocketmq-linux-arm64`），声明 `"os": ["linux"]`, `"cpu": ["arm64"]`；
   - 主协调包 `@atengk/mcp-server-rocketmq` 的 `optionalDependencies` 补充对 `linux-arm64` 子包的联动受管；
   - **装配解耦设计**：Release 产物采用带版本号名称以便于人类直接下载；在 `publish-npm` 流水线装配阶段，自动将下载的各平台二进制规范化重命名为子包内部固定的 `mcp-server-rocketmq`（Windows 下为 `mcp-server-rocketmq.exe`），无需动态修改各子包 `package.json` 的 `"bin"` 声明，保持调度器与包契约绝对整洁稳定。

## 影响与权衡

- **积极影响**：
  - 资产语义清晰：用户从 GitHub Releases 手动下载的原生单文件自带精确版本号，多版本共存与维护更直观；
  - 云原生全域覆盖：Linux ARM 架构服务器与云实例获得一级支持，`npx` 秒开能力对齐 x86 与 Apple Silicon；
  - 构建开销可控：依托 GitHub 官方 `ubuntu-24.04-arm` 硬件运行器，流水线维持在全并行 6~8 分钟健康区间。
- **妥协与代价**：
  - npm 子包维护量增加：由 3 个平台子包增至 4 个（win32-x64, linux-x64, linux-arm64, darwin-arm64），需同步维护流水线下载与发布阶段。
