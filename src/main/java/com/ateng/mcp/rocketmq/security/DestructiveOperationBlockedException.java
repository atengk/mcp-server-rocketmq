package com.ateng.mcp.rocketmq.security;

/**
 * 破坏性操作安全防呆拦截异常。
 * 当破坏性工具在未开启环境变量或未显式确认 confirm 时抛出此异常，阻断高危操作。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class DestructiveOperationBlockedException extends RuntimeException {

    /**
     * 被拦截的高危操作或工具名称。
     */
    private final String operationName;

    /**
     * 拦截阻断原因。
     */
    private final String reason;

    public DestructiveOperationBlockedException(String operationName, String reason) {
        super(String.format("Destructive operation '%s' blocked: %s", operationName, reason));
        this.operationName = operationName;
        this.reason = reason;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getReason() {
        return reason;
    }
}
