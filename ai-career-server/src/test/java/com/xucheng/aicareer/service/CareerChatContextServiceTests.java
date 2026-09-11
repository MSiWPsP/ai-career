package com.xucheng.aicareer.service;

import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.service.impl.CareerChatContextServiceImpl;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.vo.CareerPlanVO;
import com.xucheng.aicareer.vo.CareerRoadmapStageVO;
import com.xucheng.aicareer.vo.UserProfileVO;
import com.xucheng.aicareer.vo.UserSkillVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CareerChatContextServiceTests {

    @Test
    void aggregatesOnlyCareerRelatedProfileSkillAndPlanFields() {
        UserProfileService profileService = mock(UserProfileService.class);
        UserSkillService skillService = mock(UserSkillService.class);
        CareerPlanService planService = mock(CareerPlanService.class);
        CareerChatContextService service = new CareerChatContextServiceImpl(
                profileService, skillService, planService);
        when(profileService.getCurrentProfile()).thenReturn(UserProfileVO.builder()
                .education("本科")
                .major("软件工程")
                .grade("大三")
                .graduationYear(2027)
                .careerStage("INTERNSHIP")
                .targetPosition("Java后端开发工程师")
                .targetCity("杭州")
                .targetTime("半年内")
                .dailyStudyHours(new BigDecimal("3.0"))
                .careerGoal("找到Java后端实习")
                .interestDescription("喜欢工程实践")
                .build());
        when(skillService.getCurrentUserSkills()).thenReturn(List.of(UserSkillVO.builder()
                .id(90001L)
                .skillName("Java")
                .skillCategory("PROGRAMMING")
                .level(3)
                .score(60)
                .source("SELF")
                .build()));
        when(planService.getCurrentPlan()).thenReturn(CareerPlanVO.builder()
                .id(80001L)
                .version(1)
                .targetPosition("Java后端开发工程师")
                .matchScore(68)
                .summary("需要补齐中间件和项目实践能力")
                .advantages(List.of("Java基础较好"))
                .weaknesses(List.of("Redis经验不足"))
                .roadmap(List.of(CareerRoadmapStageVO.builder()
                        .stage(1)
                        .name("中间件强化")
                        .goal("掌握Redis常用场景")
                        .duration("2周")
                        .topics(List.of("缓存", "持久化"))
                        .build()))
                .status(1)
                .build());

        CareerChatBusinessContext context = service.getCurrentContext();

        assertThat(context.hasProfile()).isTrue();
        assertThat(context.profile().major()).isEqualTo("软件工程");
        assertThat(context.skills()).singleElement().satisfies(skill -> {
            assertThat(skill.skillName()).isEqualTo("Java");
            assertThat(skill.score()).isEqualTo(60);
        });
        assertThat(context.currentPlan().roadmap()).singleElement()
                .extracting(CareerChatBusinessContext.RoadmapStage::name)
                .isEqualTo("中间件强化");
    }

    @Test
    void missingProfileAndPlanBecomeExplicitEmptyContext() {
        UserProfileService profileService = mock(UserProfileService.class);
        UserSkillService skillService = mock(UserSkillService.class);
        CareerPlanService planService = mock(CareerPlanService.class);
        CareerChatContextService service = new CareerChatContextServiceImpl(
                profileService, skillService, planService);
        when(profileService.getCurrentProfile()).thenThrow(new BusinessException(404, "职业画像不存在"));
        when(skillService.getCurrentUserSkills()).thenReturn(List.of());
        when(planService.getCurrentPlan()).thenThrow(new BusinessException(404, "当前职业规划不存在"));

        CareerChatBusinessContext context = service.getCurrentContext();

        assertThat(context.hasProfile()).isFalse();
        assertThat(context.hasSkills()).isFalse();
        assertThat(context.hasCurrentPlan()).isFalse();
    }

    @Test
    void readsLatestProfileOnEveryRequestAndDoesNotHideUnexpectedErrors() {
        UserProfileService profileService = mock(UserProfileService.class);
        UserSkillService skillService = mock(UserSkillService.class);
        CareerPlanService planService = mock(CareerPlanService.class);
        CareerChatContextService service = new CareerChatContextServiceImpl(
                profileService, skillService, planService);
        when(profileService.getCurrentProfile())
                .thenReturn(UserProfileVO.builder().targetPosition("Java后端").build())
                .thenReturn(UserProfileVO.builder().targetPosition("AI应用开发").build())
                .thenThrow(new BusinessException(500, "画像数据异常"));
        when(skillService.getCurrentUserSkills()).thenReturn(List.of());
        when(planService.getCurrentPlan()).thenThrow(new BusinessException(404, "当前职业规划不存在"));

        assertThat(service.getCurrentContext().profile().targetPosition()).isEqualTo("Java后端");
        assertThat(service.getCurrentContext().profile().targetPosition()).isEqualTo("AI应用开发");
        assertThatThrownBy(service::getCurrentContext)
                .isInstanceOf(BusinessException.class)
                .hasMessage("画像数据异常");
    }
}
