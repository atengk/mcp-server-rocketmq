package com.ateng.mcp.rocketmq.mcp.tool;

import com.ateng.mcp.rocketmq.rocketmq.admin.AdminClientService;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.QueueDataDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicListDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicQueueOffsetDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicRouteDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicStatusDTO;
import com.ateng.mcp.rocketmq.rocketmq.admin.dto.TopicOperationResultDTO;
import com.ateng.mcp.rocketmq.security.DestructiveOperationBlockedException;
import com.ateng.mcp.rocketmq.security.DualLayerGuard;
import com.ateng.mcp.rocketmq.security.ReadOnlyException;
import com.ateng.mcp.rocketmq.security.ReadOnlyGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 主题元数据与队列路由 MCP 工具单元测试。
 * 验证 rocketmq_list_topics、rocketmq_topic_route、rocketmq_topic_status、rocketmq_create_topic 与 rocketmq_delete_topic 工具调用逻辑与参数校验。
 *
 * @author Ateng
 * @since 2026-10-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TopicTools MCP 工具单元测试")
class TopicToolsTest {

    @Mock
    private AdminClientService adminClientService;

    @Mock
    private ReadOnlyGuard readOnlyGuard;

    @Mock
    private DualLayerGuard dualLayerGuard;

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

    @Test
    @DisplayName("验证 rocketmq_create_topic 正常执行并调用 AdminClientService")
    void shouldCreateTopicSuccessfully() throws Exception {
        doNothing().when(readOnlyGuard).checkWritable("rocketmq_create_topic");
        TopicOperationResultDTO mockResult = new TopicOperationResultDTO("CREATE_TOPIC", "OrderTopic", "SUCCESS", "ok");
        when(adminClientService.createTopic("OrderTopic", 8, 8, 6)).thenReturn(mockResult);

        TopicOperationResultDTO result = topicTools.createTopic("OrderTopic", 8, 8, 6);

        assertThat(result).isNotNull();
        assertThat(result.getOperation()).isEqualTo("CREATE_TOPIC");
        assertThat(result.getTopic()).isEqualTo("OrderTopic");
        verify(readOnlyGuard).checkWritable("rocketmq_create_topic");
        verify(adminClientService).createTopic("OrderTopic", 8, 8, 6);
    }

    @Test
    @DisplayName("验证 rocketmq_create_topic 在只读模式下被前置拦截拒绝")
    void shouldBlockCreateTopicWhenReadOnly() throws Exception {
        doThrow(new ReadOnlyException("rocketmq_create_topic", "read only"))
                .when(readOnlyGuard).checkWritable("rocketmq_create_topic");

        assertThatThrownBy(() -> topicTools.createTopic("OrderTopic", 8, 8, 6))
                .isInstanceOf(ReadOnlyException.class);

        verify(adminClientService, never()).createTopic(any(), any(), any(), any());
    }

    @Test
    @DisplayName("验证 rocketmq_delete_topic 全授权并确认时正常删除主题")
    void shouldDeleteTopicSuccessfully() throws Exception {
        doNothing().when(dualLayerGuard).checkDestructiveOperation("rocketmq_delete_topic", true);
        TopicOperationResultDTO mockResult = new TopicOperationResultDTO("DELETE_TOPIC", "OldTopic", "SUCCESS", "deleted");
        when(adminClientService.deleteTopic("OldTopic")).thenReturn(mockResult);

        TopicOperationResultDTO result = topicTools.deleteTopic("OldTopic", true);

        assertThat(result).isNotNull();
        assertThat(result.getOperation()).isEqualTo("DELETE_TOPIC");
        assertThat(result.getTopic()).isEqualTo("OldTopic");
        verify(dualLayerGuard).checkDestructiveOperation("rocketmq_delete_topic", true);
        verify(adminClientService).deleteTopic("OldTopic");
    }

    @Test
    @DisplayName("验证 rocketmq_delete_topic 在未授权或未确认时被双层防呆拦截")
    void shouldBlockDeleteTopicWhenDualLayerGuardRejects() throws Exception {
        doThrow(new DestructiveOperationBlockedException("rocketmq_delete_topic", "blocked"))
                .when(dualLayerGuard).checkDestructiveOperation("rocketmq_delete_topic", false);

        assertThatThrownBy(() -> topicTools.deleteTopic("OldTopic", false))
                .isInstanceOf(DestructiveOperationBlockedException.class);

        verify(adminClientService, never()).deleteTopic(any());
    }

    @Test
    @DisplayName("验证主题创建与删除入参为空时前置抛出 IllegalArgumentException")
    void shouldThrowExceptionWhenTopicParamsAreBlank() {
        doNothing().when(readOnlyGuard).checkWritable("rocketmq_create_topic");
        assertThatThrownBy(() -> topicTools.createTopic("   ", 8, 8, 6))
                .isInstanceOf(IllegalArgumentException.class);

        doNothing().when(dualLayerGuard).checkDestructiveOperation("rocketmq_delete_topic", true);
        assertThatThrownBy(() -> topicTools.deleteTopic("   ", true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
