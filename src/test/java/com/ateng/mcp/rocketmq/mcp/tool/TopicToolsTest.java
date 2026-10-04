package com.ateng.mcp.rocketmq.mcp.tool;

import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.QueueDataDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicQueueOffsetDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicRouteDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicStatusDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 主题元数据与队列路由 MCP 工具单元测试。
 * 验证 rocketmq_list_topics、rocketmq_topic_route 与 rocketmq_topic_status 工具调用逻辑与参数校验。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TopicTools MCP 工具单元测试")
class TopicToolsTest {

    @Mock
    private AdminClientService adminClientService;

    @InjectMocks
    private TopicTools topicTools;

    @Test
    @DisplayName("验证 rocketmq_list_topics 工具调用默认过滤系统主题")
    void shouldListTopicsWithDefaultFilter() throws Exception {
        TopicListDTO mockDto = new TopicListDTO(List.of("OrderTopic", "PaymentTopic"));
        when(adminClientService.listTopics(false)).thenReturn(mockDto);

        TopicListDTO result = topicTools.listTopics(null);

        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getTopics()).containsExactly("OrderTopic", "PaymentTopic");
        verify(adminClientService).listTopics(false);
    }

    @Test
    @DisplayName("验证 rocketmq_list_topics 工具指定包含系统主题")
    void shouldListTopicsIncludingSystem() throws Exception {
        TopicListDTO mockDto = new TopicListDTO(List.of("OrderTopic", "TBW102"));
        when(adminClientService.listTopics(true)).thenReturn(mockDto);

        TopicListDTO result = topicTools.listTopics(true);

        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getTopics()).containsExactly("OrderTopic", "TBW102");
        verify(adminClientService).listTopics(true);
    }

    @Test
    @DisplayName("验证 rocketmq_topic_route 工具调用成功返回路由分布")
    void shouldReturnTopicRouteSuccessfully() throws Exception {
        TopicRouteDTO mockRoute = new TopicRouteDTO("OrderTopic");
        QueueDataDTO queueData = new QueueDataDTO("broker-a", 4, 4, 6);
        mockRoute.setQueueDatas(List.of(queueData));
        when(adminClientService.getTopicRoute("OrderTopic")).thenReturn(mockRoute);

        TopicRouteDTO result = topicTools.getTopicRoute("OrderTopic");

        assertThat(result).isNotNull();
        assertThat(result.getTopic()).isEqualTo("OrderTopic");
        assertThat(result.getQueueDatas()).hasSize(1);
        assertThat(result.getQueueDatas().getFirst().getBrokerName()).isEqualTo("broker-a");
        assertThat(result.getQueueDatas().getFirst().getReadQueueNums()).isEqualTo(4);
    }

    @Test
    @DisplayName("验证 rocketmq_topic_route 传入空主题名称时抛出参数异常")
    void shouldThrowExceptionWhenTopicRouteParamIsBlank() {
        assertThatThrownBy(() -> topicTools.getTopicRoute("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Topic name must not be blank");
    }

    @Test
    @DisplayName("验证 rocketmq_topic_status 工具调用成功返回位点与容量统计")
    void shouldReturnTopicStatusSuccessfully() throws Exception {
        TopicStatusDTO mockStatus = new TopicStatusDTO("OrderTopic");
        TopicQueueOffsetDTO queueOffset = new TopicQueueOffsetDTO("broker-a", 0, 0L, 1000L, 1728000000000L);
        mockStatus.setQueues(List.of(queueOffset));
        mockStatus.setTotalMessages(1000L);
        when(adminClientService.getTopicStatus("OrderTopic")).thenReturn(mockStatus);

        TopicStatusDTO result = topicTools.getTopicStatus("OrderTopic");

        assertThat(result).isNotNull();
        assertThat(result.getTopic()).isEqualTo("OrderTopic");
        assertThat(result.getTotalMessages()).isEqualTo(1000L);
        assertThat(result.getQueues()).hasSize(1);
        assertThat(result.getQueues().getFirst().getMaxOffset()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("验证 rocketmq_topic_status 传入空主题名称时抛出参数异常")
    void shouldThrowExceptionWhenTopicStatusParamIsBlank() {
        assertThatThrownBy(() -> topicTools.getTopicStatus(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Topic name must not be blank");
    }
}
