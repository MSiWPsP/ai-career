package com.xucheng.aicareer.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CareerToolIntentRouterTests {

    private final CareerToolIntentRouter router = new CareerToolIntentRouter(true);

    @Test
    void routesPersonalPlatformDataQuestionsToTools() {
        assertThat(router.requiresTools("我还有哪些任务没有完成？")).isTrue();
        assertThat(router.requiresTools("最近一次模拟面试的薄弱项是什么？")).isTrue();
        assertThat(router.requiresTools("我的能力评分最近有变化吗？")).isTrue();
    }

    @Test
    void keepsGeneralCareerQuestionsOutOfTools() {
        assertThat(router.requiresTools("Java后端一般需要哪些能力？")).isFalse();
        assertThat(router.requiresTools("如何安排一份通用学习任务？")).isFalse();
    }

    @Test
    void distinguishesPureLookupFromLookupAndAdvice() {
        assertThat(router.isPersonalDataOnly("我还有哪些任务没有完成？")).isTrue();
        assertThat(router.isPersonalDataOnly("结合我的任务进度给出下一步建议")).isFalse();
    }

    @Test
    void featureFlagCanDisableToolRouting() {
        CareerToolIntentRouter disabled = new CareerToolIntentRouter(false);

        assertThat(disabled.requiresTools("我还有哪些任务没有完成？")).isFalse();
    }
}
