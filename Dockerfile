# ==============================================================================
# 阶段 1：构建阶段 (Builder)
# 使用集成 JDK 21 与 Maven 的 Alpine 基础镜像进行源码编译与打包
# ==============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 先复制 pom.xml 下载依赖以利用 Docker 层缓存
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 复制源码并执行构建打包（跳过测试以加速镜像构建，测试已由 CI 门禁保障）
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# 阶段 2：运行阶段 (Runner)
# 基于轻量级 Temurin JRE 21 Alpine 镜像，践行非 root 最小特权安全规范
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

LABEL maintainer="Ateng" \
      description="Model Context Protocol (MCP) Server for Apache RocketMQ 5"

# 创建专有低特权运行用户与用户组
RUN addgroup -S -g 10001 mcp && \
    adduser -S -u 10001 -G mcp mcp

WORKDIR /app

# 从构建阶段复制打包出的可执行 Fat Jar
COPY --from=builder --chown=mcp:mcp /build/target/mcp-server-rocketmq-*.jar /app/app.jar

# 切换为非 root 用户运行
USER mcp

# 默认运行时环境变量（默认以 SSE 网络模式启动，暴露 8080 端口）
ENV SERVER_PORT=8080 \
    MCP_TRANSPORT=sse \
    ROCKETMQ_NAMESRV_ADDR="127.0.0.1:9876" \
    ROCKETMQ_ENDPOINTS="127.0.0.1:8081" \
    ROCKETMQ_READ_ONLY=false \
    ROCKETMQ_ENABLE_DESTRUCTIVE_TOOLS=false \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# 暴露 SSE 模式 HTTP 端口
EXPOSE 8080

# 优雅停机信号支持 (通过 exec 保证 PID 1 直接透传 SIGTERM)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
