package com.xucheng.aicareer.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagAnswerFactGuardTests {

    @Test
    void rejectsInventedMeasuredResultsIncludingExamples() {
        assertThat(RagAnswerFactGuard.review("例如缓存上线后 QPS 提升 80%。").accepted()).isFalse();
        assertThat(RagAnswerFactGuard.review("查询由 3 次 IO 变为 1 次。").accepted()).isFalse();
        assertThat(RagAnswerFactGuard.review("显著降低响应时间。 ").accepted()).isFalse();
        assertThat(RagAnswerFactGuard.review("解决性能问题。 ").accepted()).isFalse();
        assertThat(RagAnswerFactGuard.review("例如：引入缓存后减少了数据库 IO 次数。")
                .accepted()).isFalse();
        assertThat(RagAnswerFactGuard.review("优化 SQL 执行计划，解决了接口偶发超时的问题。")
                .accepted()).isFalse();
    }

    @Test
    void permitsVerifiableActionPlanWithoutClaimingOutcome() {
        assertThat(RagAnswerFactGuard.review("可以尝试加入缓存，并通过相同负载下的压测验证响应时间是否改善。")
                .accepted()).isTrue();
    }
}
