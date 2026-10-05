# 0007. 包命名空间与 Maven 坐标全局规范化

## 背景与上下文

在早期开发迭代中，工程包名和 Maven 坐标采用了临时性的 `com.ateng` 与 `com.ateng.mcp.rocketmq`。
随着项目推进至容器化与公开发布阶段，需要对外发布到 GitHub Packages（`maven.pkg.github.com`）、Maven Central（Sonatype Central Portal）以及 GitHub Container Registry (GHCR)。
在公开发布生态中：
1. **域名所有权校验**：Maven Central 与 GitHub Packages 要求使用经过验证的反向域名。对于托管在 GitHub 上的开源项目，规范前缀为 `io.github.<username>`；
2. **所有权一致性**：当前 GitHub 仓库为 `atengk/mcp-server-rocketmq`，命名空间必须与实际所属账号 `atengk` 严格对应；
3. **长期扩展性**：组织根包应保持整洁，避免不同子项目或未来其他中间件 MCP 扩展互相污染。

## 架构决策

我们决定实施全局原子性的包名与 Maven 坐标重命名重构：

1. **Java 根包层级规范**：
   - 统一采用 `io.github.atengk.mcp.rocketmq` 作为项目根包；
   - 源码物理目录整体平移至 `src/main/java/io/github/atengk/mcp/rocketmq/`；
   - 测试代码物理目录整体平移至 `src/test/java/io/github/atengk/mcp/rocketmq/`；
2. **Maven GAV 坐标规范**：
   - 将 `pom.xml` 中的 `<groupId>` 从 `com.ateng` 更新为 `io.github.atengk`；
   - 将 `<distributionManagement>` 的发布目标 URL 对齐为 `https://maven.pkg.github.com/atengk/mcp-server-rocketmq`；
3. **框架与容器配置全量原子对齐**：
   - 更新 Spring SPI 引导文件 `src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` 中的类全限定名；
   - 同步更新 `docker-compose.yml` 与容器镜像标签为 `ghcr.io/atengk/mcp-server-rocketmq`；
   - 保持类级别 Doc 注释中的 `@author Ateng` 人员标识不变。

## 影响与权衡

- **积极影响**：
  - 符合 Maven Central 及 GitHub Packages 官方发布准入要求；
  - 杜绝命名冲突与权限校验失败风险；
  - 全套 131 项单元测试与集成测试保持全绿通过。
- **妥协与代价**：
  - 源码与测试代码的目录结构发生深层变更，历史未合入分支若存在需进行冲突对齐。
