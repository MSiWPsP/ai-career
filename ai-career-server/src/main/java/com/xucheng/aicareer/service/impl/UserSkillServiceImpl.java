package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.dto.UserSkillBatchDTO;
import com.xucheng.aicareer.dto.UserSkillItemDTO;
import com.xucheng.aicareer.entity.UserSkill;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.UserSkillMapper;
import com.xucheng.aicareer.service.UserSkillService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.UserSkillVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserSkillServiceImpl implements UserSkillService {

    private static final int SCORE_PER_LEVEL = 20;

    private final UserSkillMapper userSkillMapper;

    @Override
    public List<UserSkillVO> getCurrentUserSkills() {
        return findSkills(UserContext.getUserId()).stream()
                .map(this::toUserSkillVO)
                .toList();
    }

    @Override
    @Transactional
    public List<UserSkillVO> replaceCurrentUserSkills(UserSkillBatchDTO userSkillBatchDTO) {
        Long userId = UserContext.getUserId();
        validateNoDuplicateSkillNames(userSkillBatchDTO.getSkills());

        userSkillMapper.delete(Wrappers.<UserSkill>lambdaQuery()
                .eq(UserSkill::getUserId, userId));
        for (UserSkillItemDTO item : userSkillBatchDTO.getSkills()) {
            UserSkill skill = new UserSkill();
            skill.setUserId(userId);
            skill.setSkillName(item.getSkillName().trim());
            skill.setSkillCategory(item.getSkillCategory().trim());
            skill.setLevel(item.getLevel());
            skill.setScore(item.getLevel() * SCORE_PER_LEVEL);
            skill.setSource("SELF");
            userSkillMapper.insert(skill);
        }
        return findSkills(userId).stream()
                .map(this::toUserSkillVO)
                .toList();
    }

    private List<UserSkill> findSkills(Long userId) {
        return userSkillMapper.selectList(Wrappers.<UserSkill>lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .orderByAsc(UserSkill::getSkillCategory, UserSkill::getSkillName));
    }

    private void validateNoDuplicateSkillNames(List<UserSkillItemDTO> skills) {
        Set<String> names = new HashSet<>();
        for (UserSkillItemDTO skill : skills) {
            String normalizedName = skill.getSkillName().trim().toLowerCase(Locale.ROOT);
            if (!names.add(normalizedName)) {
                throw new BusinessException(409, "技能名称不能重复：" + skill.getSkillName());
            }
        }
    }

    private UserSkillVO toUserSkillVO(UserSkill skill) {
        return UserSkillVO.builder()
                .id(skill.getId())
                .skillName(skill.getSkillName())
                .skillCategory(skill.getSkillCategory())
                .level(skill.getLevel())
                .score(skill.getScore())
                .source(skill.getSource())
                .build();
    }
}
