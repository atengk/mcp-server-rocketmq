package com.ateng.mcp.rocketmq.security;

/**
 * 全局只读模式安全防御异常。
 * 当 MCP 服务端处于只读保护状态（ROCKETMQ_READ_ONLY=true）且尝试执行任何写入或变更操作时抛出。
 *
 * @author Ateng
 * @since 2026-10-04
 */
public class ReadOnlyException extends RuntimeException {

    public ReadOnlyException(String message) {
        super(message);
    }

    public ReadOnlyException(String operationName, String detail) {
        super(String.format("Operation '%s' rejected: RocketMQ MCP Server is running in READ-ONLY mode. %s",
                operationName, detail));
    }
}
