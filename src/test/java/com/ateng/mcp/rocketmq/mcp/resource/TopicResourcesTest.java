package com.ateng.mcp.rocketmq.mcp.resource;

import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicOverviewDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业务主题列表与队列概览 MCP 资源单元测试。
 * 验证 rocketmq://topics 资源序列化与主题队列概览输出。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TopicResources MCP 资源单元测试")
class TopicResourcesTest {

    @Mock
    private AdminClientService adminClientService;

    @InjectMocks
    private TopicResources topicResources;

    @Test
    @DisplayName("验证 rocketmq://topics 返回正确的业务主题与队列概览快照 JSON")
    void shouldReturnTopicsOverviewResourceJson() throws Exception {
        TopicOverviewDTO.TopicSummaryDTO summaryA = new TopicOverviewDTO.TopicSummaryDTO("TopicA", 8, 8, List.of("broker-a"));
        TopicOverviewDTO.TopicSummaryDTO summaryB = new TopicOverviewDTO.TopicSummaryDTO("TopicB", 4, 4, List.of("broker-b"));
        TopicOverviewDTO mockDto = new TopicOverviewDTO(List.of(summaryA, summaryB));
        when(adminClientService.getTopicsOverview()).thenReturn(mockDto);

        String json = topicResources.getTopicsOverview();

        assertThat(json).isNotBlank();
        assertThat(json).contains("\"TopicA\"");
        assertThat(json).contains("\"totalReadQueues\":8");
        assertThat(json).contains("\"broker-a\"");
        assertThat(json).contains("\"TopicB\"");
        assertThat(json).contains("\"totalCount\":2");
        verify(adminClientService).getTopicsOverview();
    }

    @Test
    @DisplayName("当底层查询主题异常时应返回受控的 JSON 错误提示")
    void shouldHandleExceptionGracefully() throws Exception {
        when(adminClientService.getTopicsOverview()).thenThrow(new RuntimeException("Remoting timeout"));

        String json = topicResources.getTopicsOverview();

        assertThat(json).isNotBlank();
        assertThat(json).contains("\"error\":\"Remoting timeout\"");
    }
}
