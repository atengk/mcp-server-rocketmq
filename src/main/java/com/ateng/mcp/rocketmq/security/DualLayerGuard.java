package com.ateng.mcp.rocketmq.security;

import com.ateng.mcp.rocketmq.config.RocketmqProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 破坏性操作双层防呆安全守卫。
 * 针对删除主题、重置消费位点、死信重新投递等高危破坏性操作提供双层保护：
 * 1. 启动级环境开关守卫（ROCKETMQ_ENABLE_DESTRUCTIVE_TOOLS=true）；
 * 2. 调用级参数显式确认守卫（confirm == Boolean.TRUE）。
 * 同时前置受全局只读守卫约束。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@Component
public class DualLayerGuard {

    private static final Logger log = LoggerFactory.getLogger(DualLayerGuard.class);

    private final RocketmqProperties properties;
    private final ReadOnlyGuard readOnlyGuard;

    public DualLayerGuard(RocketmqProperties properties, ReadOnlyGuard readOnlyGuard) {
        this.properties = properties;
        this.readOnlyGuard = readOnlyGuard;
    }

    /**
     * 判断当前 MCP 服务端是否激活了破坏性操作支持开关。
     *
     * @return 若激活则返回 true，否则返回 false
     */
    public boolean isDestructiveEnabled() {
        return properties != null && properties.isEnableDestructiveTools();
    }

    /**
     * 校验目标破坏性操作的执行合法性。
     * 顺序执行：全局只读拦截 -> 环境开关拦截 -> 显式 confirm 参数确认拦截。
     *
     * @param operationName 破坏性操作或工具名称（例如 rocketmq_delete_topic）
     * @param confirm 客户端显式传入的确认标识
     * @throws ReadOnlyException 当服务端处于全局只读模式时抛出
     * @throws DestructiveOperationBlockedException 当环境开关未激活或确认标识不合法时抛出
     */
    public void checkDestructiveOperation(String operationName, Boolean confirm) {
        String op = (operationName != null && !operationName.isBlank()) ? operationName.trim() : "destructive_operation";

        // 1. 全局只读安全拦截
        readOnlyGuard.checkWritable(op);

        // 2. 第一层防呆：启动级环境开关校验
        if (!isDestructiveEnabled()) {
            log.warn("Blocked destructive attempt on '{}' because destructive tools are disabled. " +
                    "Set ROCKETMQ_ENABLE_DESTRUCTIVE_TOOLS=true to enable.", op);
            throw new DestructiveOperationBlockedException(op,
                    "Destructive operations are disabled by default. Set ROCKETMQ_ENABLE_DESTRUCTIVE_TOOLS=true " +
                            "(or rocketmq.enable-destructive-tools=true) to enable.");
        }

        // 3. 第二层防呆：调用级显式确认校验
        if (confirm == null || !confirm) {
            log.warn("Blocked destructive attempt on '{}' because 'confirm' parameter was not explicitly set to true.", op);
            throw new DestructiveOperationBlockedException(op,
                    "Confirmation required. You must explicitly pass 'confirm=true' to execute this destructive operation.");
        }
    }
}
