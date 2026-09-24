package com.xucheng.aicareer.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeQueryPlannerTests {

    private final KnowledgeQueryPlanner planner = new KnowledgeQueryPlanner();

    @Test
    void keepsFocusedQuestionAsSingleQuery() {
        assertThat(planner.plan("Redis 缓存失效怎么处理？"))
                .containsExactly("Redis 缓存失效怎么处理？");
    }

    @Test
    void expandsBroadBackendRoadmapAcrossChapters() {
        assertThat(planner.plan("我会一点 Java，接下来怎样分阶段学习后端？"))
                .hasSize(4)
                .anyMatch(query -> query.contains("可运行基础"))
                .anyMatch(query -> query.contains("业务闭环"))
                .anyMatch(query -> query.contains("日志") && query.contains("部署"));
    }

    @Test
    void expandsInternshipQuestionIntoResumeApplicationAndInterview() {
        assertThat(planner.plan("大三投实习，简历、投递和面试应该怎样安排？"))
                .hasSize(4)
                .anyMatch(query -> query.contains("简历") && query.contains("可验证材料"))
                .anyMatch(query -> query.contains("投递") && query.contains("复盘"))
                .anyMatch(query -> query.contains("面试") && query.contains("真实案例"));
    }

    @Test
    void neverExpandsBeyondThreeSupplementalQueries() {
        assertThat(planner.plan("Java 后端核心能力和分阶段学习路线怎么安排？"))
                .hasSize(4)
                .anyMatch(query -> query.contains("Spring Boot") && query.contains("业务分层"))
                .anyMatch(query -> query.contains("Redis") && query.contains("执行计划"));
    }
}
