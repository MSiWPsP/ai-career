package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.agent.career.dto.GroundedCareerAnswer;
import com.xucheng.aicareer.service.model.KnowledgeReference;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroundedCareerAnswerComposerTests {

    private final KnowledgeReference source = new KnowledgeReference("doc", "项目实践", "验证", "知识库");
    private final String quote = "没有测量数据时，应说明改进点和验证方式，不制造性能指标。";
    private final KnowledgeRetrievalResult knowledge = new KnowledgeRetrievalResult(
            "[1] 《项目实践》· 验证\n" + quote + "\n\n", List.of(source));

    @Test
    void acceptsExactExcerptAndSeparatesOptionalAction() {
        var draft = new GroundedCareerAnswer(
                List.of(new GroundedCareerAnswer.SourceExcerpt(1, quote)),
                List.of("可以先记录相同负载下的测量方法，再验证改动是否有效。"));

        var composed = GroundedCareerAnswerComposer.compose(draft, knowledge);

        assertThat(composed.accepted()).isTrue();
        assertThat(composed.references()).containsExactly(source);
        assertThat(composed.content()).contains(quote, "可选行动（需结合个人情况验证）");
    }

    @Test
    void rejectsForgedQuoteOrMeasuredOutcomeBeforeDelivery() {
        var forged = new GroundedCareerAnswer(
                List.of(new GroundedCareerAnswer.SourceExcerpt(1, "这个片段并没有说性能提升百分之八十。")),
                List.of());
        var inventedResult = new GroundedCareerAnswer(
                List.of(new GroundedCareerAnswer.SourceExcerpt(1, quote)),
                List.of("建议写成缓存上线后 QPS 提升 80%。"));
        var unmeasuredClaim = new GroundedCareerAnswer(
                List.of(new GroundedCareerAnswer.SourceExcerpt(1, quote)),
                List.of("建议写成已经解决了接口超时问题。"));

        assertThat(GroundedCareerAnswerComposer.compose(forged, knowledge).references()).isEmpty();
        assertThat(GroundedCareerAnswerComposer.compose(inventedResult, knowledge).references())
                .containsExactly(source);
        assertThat(GroundedCareerAnswerComposer.compose(inventedResult, knowledge).content())
                .doesNotContain("80%");
        assertThat(GroundedCareerAnswerComposer.compose(unmeasuredClaim, knowledge).content())
                .doesNotContain("已经解决");
    }
}
