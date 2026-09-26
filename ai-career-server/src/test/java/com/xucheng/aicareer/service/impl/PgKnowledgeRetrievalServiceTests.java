package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PgKnowledgeRetrievalServiceTests {

    private final PgKnowledgeRepository repository = mock(PgKnowledgeRepository.class);
    private final EmbeddingModel embeddingClient = mock(EmbeddingModel.class);
    private final PgKnowledgeRetrievalService service = new PgKnowledgeRetrievalService(
            repository, embeddingClient, "text-embedding-v4", 0.55);
    private final CareerChatBusinessContext context = new CareerChatBusinessContext(null, List.of(), null);

    @Test
    void skipsGreetingsAndLocalOnlyRequests() {
        assertThat(service.retrieve("你好", context).hasKnowledge()).isFalse();
        assertThat(service.retrieve("我还有多少未完成任务？", context).hasKnowledge()).isFalse();
        verify(embeddingClient, never()).embed(anyList());
    }

    @Test
    void routesEngineeringTradeoffQuestionToKnowledge() {
        assertThat(service.shouldRetrieve("如何讲清选择数据模型、鉴权方式和缓存策略的取舍？")).isTrue();
        assertThat(service.shouldRetrieve("我的任务完成率是多少？")).isFalse();
        assertThat(service.shouldRetrieve("你知道我的年级和目标城市吗？据此给我职业建议。")).isFalse();
    }

    @Test
    void embeddingFailureFallsBackWithoutBreakingChat() {
        when(embeddingClient.embed(anyList())).thenThrow(new IllegalStateException("provider failed"));

        assertThat(service.retrieve("Java 后端需要什么技能？", context).hasKnowledge()).isFalse();
    }

    @Test
    void databaseFailureFallsBackWithoutInventingReferences() {
        when(embeddingClient.embed(anyList())).thenReturn(List.of(new float[1024]));
        when(repository.search(any(float[].class), nullable(String.class), eq("text-embedding-v4"), eq(12)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThat(service.retrieve("Java 后端需要什么技能？", context).references()).isEmpty();
    }

    @Test
    void removesContactDetailsFromEmbeddingQuery() {
        String safe = service.sanitizeQuery("我叫张三，邮箱是 test@example.com，电话 13812345678，想学 Java 后端");

        assertThat(safe).contains("Java 后端", "[姓名]", "[邮箱]", "[电话]")
                .doesNotContain("张三", "test@example.com", "13812345678");
    }

    @Test
    void selectedSourceSnapshotCarriesExactChunkProvenance() {
        when(embeddingClient.embed(anyList())).thenReturn(List.of(new float[1024]));
        when(repository.search(any(float[].class), nullable(String.class), eq("text-embedding-v4"), eq(12)))
                .thenReturn(List.of(new PgKnowledgeRepository.KnowledgeHit("project-evidence",
                        "项目实践", "保留工程证据", "知识库", "没有测量记录就不写性能结果。", 0.91, 2, 3)));

        var result = service.retrieve("项目性能结果如何写？", context);

        assertThat(result.references()).singleElement().satisfies(reference -> {
            assertThat(reference.documentVersion()).isEqualTo(2);
            assertThat(reference.chunkIndex()).isEqualTo(3);
            assertThat(reference.contentSha256()).matches("[0-9a-f]{64}");
        });
        assertThat(result.context()).contains("没有测量记录就不写性能结果。");
    }

    @Test
    void multiIntentPlanMergesDifferentSectionsFromSameDocument() {
        KnowledgeQueryPlanner planner = mock(KnowledgeQueryPlanner.class);
        when(planner.plan("简历、投递和面试怎么安排？")).thenReturn(List.of(
                "简历、投递和面试怎么安排？", "实习简历 可验证材料", "实习投递 复盘", "实习面试 真实案例"));
        when(embeddingClient.embed(anyList())).thenAnswer(invocation -> ((List<String>) invocation.getArgument(0))
                .stream().map(query -> {
                    float[] vector = new float[1024];
                    vector[0] = query.contains("简历") && query.contains("可验证") ? 1
                            : query.contains("投递") && query.contains("复盘") ? 2
                            : query.contains("面试") && query.contains("真实案例") ? 3 : 0;
                    return vector;
                }).toList());
        when(repository.search(any(float[].class), nullable(String.class), eq("text-embedding-v4"), eq(12)))
                .thenAnswer(invocation -> {
                    int queryIndex = Math.round(((float[]) invocation.getArgument(0))[0]);
                    return switch (queryIndex) {
                        case 1 -> List.of(hit("准备可验证材料", 1, 0.90));
                        case 2 -> List.of(hit("建立投递与复盘节奏", 2, 0.89));
                        case 3 -> List.of(hit("面试准备", 3, 0.88));
                        default -> List.of(hit("先明确目标与约束", 0, 0.91));
                    };
                });
        PgKnowledgeRetrievalService expanded = new PgKnowledgeRetrievalService(
                repository, embeddingClient, "text-embedding-v4", 0.55, planner);

        var result = expanded.retrieve("简历、投递和面试怎么安排？", context);

        assertThat(result.references()).extracting(reference -> reference.section())
                .containsExactly("先明确目标与约束", "准备可验证材料", "建立投递与复盘节奏", "面试准备");
    }

    @Test
    void supplementalDatabaseFailureKeepsOriginalAndOtherSupplementalResults() {
        KnowledgeQueryPlanner planner = mock(KnowledgeQueryPlanner.class);
        when(planner.plan("简历、投递和面试怎么安排？")).thenReturn(List.of(
                "简历、投递和面试怎么安排？", "实习简历 可验证材料", "实习投递 复盘", "实习面试 真实案例"));
        when(embeddingClient.embed(anyList())).thenAnswer(invocation -> ((List<String>) invocation.getArgument(0))
                .stream().map(query -> {
                    float[] vector = new float[1024];
                    vector[0] = query.contains("简历") && query.contains("可验证") ? 1
                            : query.contains("投递") && query.contains("复盘") ? 2
                            : query.contains("面试") && query.contains("真实案例") ? 3 : 0;
                    return vector;
                }).toList());
        when(repository.search(any(float[].class), nullable(String.class), eq("text-embedding-v4"), eq(12)))
                .thenAnswer(invocation -> {
                    int queryIndex = Math.round(((float[]) invocation.getArgument(0))[0]);
                    return switch (queryIndex) {
                        case 1 -> throw new IllegalStateException("supplement unavailable");
                        case 2 -> List.of(hit("建立投递与复盘节奏", 2, 0.89));
                        case 3 -> List.of(hit("面试准备", 3, 0.88));
                        default -> List.of(hit("先明确目标与约束", 0, 0.91));
                    };
                });
        PgKnowledgeRetrievalService expanded = new PgKnowledgeRetrievalService(
                repository, embeddingClient, "text-embedding-v4", 0.55, planner);

        var result = expanded.retrieve("简历、投递和面试怎么安排？", context);

        assertThat(result.references()).extracting(reference -> reference.section())
                .containsExactly("先明确目标与约束", "建立投递与复盘节奏", "面试准备");
    }

    private PgKnowledgeRepository.KnowledgeHit hit(String section, int chunkIndex, double score) {
        return new PgKnowledgeRepository.KnowledgeHit("internship-preparation", "实习与校招准备清单",
                section, "AI职途项目知识库", section + "的正文内容需要足够长。", score, 1, chunkIndex);
    }
}
