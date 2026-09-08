package com.xucheng.aicareer;

import com.xucheng.aicareer.dto.LoginDTO;
import com.xucheng.aicareer.dto.RegisterDTO;
import com.xucheng.aicareer.dto.UpdateUserDTO;
import com.xucheng.aicareer.dto.UserProfileDTO;
import com.xucheng.aicareer.dto.UserSkillBatchDTO;
import com.xucheng.aicareer.dto.UserSkillItemDTO;
import com.xucheng.aicareer.service.AuthService;
import com.xucheng.aicareer.service.UserService;
import com.xucheng.aicareer.service.UserProfileService;
import com.xucheng.aicareer.service.UserSkillService;
import com.xucheng.aicareer.utils.JwtUtils;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CoreBusinessIntegrationTests {

    private final AuthService authService;
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final UserSkillService userSkillService;
    private final JwtUtils jwtUtils;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void registerLoginProfileAndSkillsWorkTogether() {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(username);
        registerDTO.setPassword("123456");
        registerDTO.setNickname("测试用户");
        authService.register(registerDTO);

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername(username);
        loginDTO.setPassword("123456");
        LoginVO loginVO = authService.login(loginDTO);
        assertThat(jwtUtils.parseUserId(loginVO.getToken())).isEqualTo(loginVO.getUserId());
        UserContext.setUserId(loginVO.getUserId());

        UpdateUserDTO updateUserDTO = new UpdateUserDTO();
        updateUserDTO.setNickname("更新后的昵称");
        assertThat(userService.updateCurrentUser(updateUserDTO).getNickname())
                .isEqualTo("更新后的昵称");

        UserProfileDTO profileDTO = new UserProfileDTO();
        profileDTO.setEducation("本科");
        profileDTO.setMajor("软件工程");
        profileDTO.setGrade("大三");
        profileDTO.setGraduationYear(2028);
        profileDTO.setCareerStage("LEARNING");
        profileDTO.setTargetPosition("Java后端开发工程师");
        profileDTO.setTargetTime("6个月");
        profileDTO.setDailyStudyHours(new BigDecimal("3.0"));
        profileDTO.setCareerGoal("半年后寻找Java后端实习");
        userProfileService.createProfile(profileDTO);
        assertThat(userProfileService.getCompletion().getCompleted()).isTrue();

        profileDTO.setGrade("大四");
        profileDTO.setCareerStage("INTERNSHIP");
        assertThat(userProfileService.updateProfile(profileDTO).getGrade()).isEqualTo("大四");

        UserSkillItemDTO javaSkill = new UserSkillItemDTO();
        javaSkill.setSkillName("Java");
        javaSkill.setSkillCategory("PROGRAMMING");
        javaSkill.setLevel(4);
        UserSkillBatchDTO skillBatchDTO = new UserSkillBatchDTO();
        skillBatchDTO.setSkills(List.of(javaSkill));

        assertThat(userSkillService.replaceCurrentUserSkills(skillBatchDTO))
                .singleElement()
                .satisfies(skill -> {
                    assertThat(skill.getScore()).isEqualTo(80);
                    assertThat(skill.getSource()).isEqualTo("SELF");
                });
    }
}
