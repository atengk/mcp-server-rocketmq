package com.ateng.mcp.rocketmq.security;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 全局只读安全守卫拦截器。
 * 针对消息发送、主题变更、位点重置等写操作施加全局安全屏障，杜绝生产核心环境误写入。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class ReadOnlyGuard {

    private static final Logger log = LoggerFactory.getLogger(ReadOnlyGuard.class);

    private final RocketmqProperties properties;

    public ReadOnlyGuard(RocketmqProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断当前 MCP 服务端是否处于全局只读模式。
     *
     * @return 若只读则返回 true，否则返回 false
     */
    public boolean isReadOnly() {
        return properties != null && properties.isReadOnly();
    }

    /**
     * 校验目标操作是否允许写入。若处于只读模式则直接抛出 ReadOnlyException 终止执行。
     *
     * @param operationName 待校验的操作名称或工具名（如 rocketmq_send_message）
     * @throws ReadOnlyException 当服务端处于只读模式时抛出
     */
    public void checkWritable(String operationName) {
        if (isReadOnly()) {
            String op = (operationName != null && !operationName.isBlank()) ? operationName.trim() : "write_operation";
            log.warn("Blocked write attempt on '{}' due to global read-only mode (ROCKETMQ_READ_ONLY=true).", op);
            throw new ReadOnlyException(op, "Write operations are strictly forbidden (ROCKETMQ_READ_ONLY=true).");
        }
    }
}
