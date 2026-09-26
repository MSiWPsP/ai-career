package com.xucheng.aicareer.agent.career.tool;

import com.xucheng.aicareer.service.CareerToolQueryService;
import com.xucheng.aicareer.service.model.CareerTaskSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CareerReadToolsTests {

    @Test
    void taskToolUsesAuthenticatedContextAndRecordsExactEvidence() {
        CareerToolQueryService queryService = mock(CareerToolQueryService.class);
        when(queryService.getCurrentTaskSnapshot(10001L)).thenReturn(new CareerTaskSnapshot(
                2, 0, 1, 1, 0,
                List.of(new CareerTaskSnapshot.TaskItem(
                        "项目实践", "完成接口测试", 3, 1, LocalDate.of(2026, 10, 1)))));
        CareerReadTools tools = new CareerReadTools(queryService);
        CareerToolExecutionTrace trace = new CareerToolExecutionTrace();

        CareerToolResult result = tools.getCurrentCareerTasks(context(10001L, trace));

        assertThat(result.available()).isTrue();
        assertThat(result.evidence()).hasSize(2);
        assertThat(result.evidence().getFirst().evidenceId())
                .isEqualTo("get_current_career_tasks-1-1");
        assertThat(result.evidence().get(1).text())
                .contains("完成接口测试", "进行中", "2026-10-01");
        assertThat(trace.snapshot()).containsEntry(
                result.evidence().getFirst().evidenceId(), result.evidence().getFirst());
        verify(queryService).getCurrentTaskSnapshot(10001L);
    }

    @Test
    void toolFailureReturnsNoEvidenceAndDoesNotExposeException() {
        CareerToolQueryService queryService = mock(CareerToolQueryService.class);
        when(queryService.getCurrentAbilitySnapshot(10001L))
                .thenThrow(new IllegalStateException("database password must not leak"));
        CareerToolExecutionTrace trace = new CareerToolExecutionTrace();

        CareerToolResult result = new CareerReadTools(queryService)
                .getCurrentAbilitySnapshot(context(10001L, trace));

        assertThat(result.available()).isFalse();
        assertThat(result.message()).isEqualTo("平台数据暂时不可用");
        assertThat(result.evidence()).isEmpty();
        assertThat(trace.snapshot()).isEmpty();
    }

    private ToolContext context(Long userId, CareerToolExecutionTrace trace) {
        return new ToolContext(Map.of(
                CareerReadTools.USER_ID_CONTEXT_KEY, userId,
                CareerReadTools.TRACE_CONTEXT_KEY, trace));
    }
}
