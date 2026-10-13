package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.UserSkillBatchDTO;
import com.xucheng.aicareer.dto.UserSkillItemDTO;
import com.xucheng.aicareer.entity.UserSkill;
import com.xucheng.aicareer.mapper.UserSkillMapper;
import com.xucheng.aicareer.service.impl.UserSkillServiceImpl;
import com.xucheng.aicareer.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserSkillServiceTests {
    @AfterEach
    void clearContext() { UserContext.clear(); }

    @Test
    void replacingOneSkillKeepsUnchangedInterviewAssessment() {
        UserContext.setUserId(1L);
        var mapper = mock(UserSkillMapper.class);
        var assessed = new UserSkill();
        assessed.setSkillName("MySQL");
        assessed.setSkillCategory("DATABASE");
        assessed.setLevel(4);
        assessed.setScore(72);
        assessed.setSource("INTERVIEW");
        when(mapper.selectList(any())).thenReturn(List.of(assessed));
        var java = new UserSkillItemDTO();
        java.setSkillName("Java");
        java.setSkillCategory("PROGRAMMING");
        java.setLevel(3);
        var mysql = new UserSkillItemDTO();
        mysql.setSkillName("MySQL");
        mysql.setSkillCategory("DATABASE");
        mysql.setLevel(4);
        var request = new UserSkillBatchDTO();
        request.setSkills(List.of(java, mysql));
        new UserSkillServiceImpl(mapper).replaceCurrentUserSkills(request);
        var inserted = ArgumentCaptor.forClass(UserSkill.class);
        verify(mapper, times(2)).insert(inserted.capture());
        assertThat(inserted.getAllValues()).anySatisfy(skill -> {
            assertThat(skill.getSkillName()).isEqualTo("MySQL");
            assertThat(skill.getScore()).isEqualTo(72);
            assertThat(skill.getSource()).isEqualTo("INTERVIEW");
        }).anySatisfy(skill -> {
            assertThat(skill.getSkillName()).isEqualTo("Java");
            assertThat(skill.getScore()).isEqualTo(60);
            assertThat(skill.getSource()).isEqualTo("SELF");
        });
    }
}
