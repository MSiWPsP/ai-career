package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.dto.UserProfileDTO;
import com.xucheng.aicareer.entity.UserProfile;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.UserProfileMapper;
import com.xucheng.aicareer.service.UserProfileService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.ProfileCompletionVO;
import com.xucheng.aicareer.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final int REQUIRED_FIELD_COUNT = 9;

    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfileVO getCurrentProfile() {
        return toUserProfileVO(getRequiredProfile(UserContext.getUserId()));
    }

    @Override
    @Transactional
    public UserProfileVO createProfile(UserProfileDTO userProfileDTO) {
        Long userId = UserContext.getUserId();
        if (findProfile(userId) != null) {
            throw new BusinessException(409, "职业画像已存在，请使用修改接口");
        }

        UserProfile userProfile = new UserProfile();
        BeanUtils.copyProperties(userProfileDTO, userProfile);
        userProfile.setUserId(userId);
        userProfileMapper.insert(userProfile);
        return toUserProfileVO(userProfile);
    }

    @Override
    @Transactional
    public UserProfileVO updateProfile(UserProfileDTO userProfileDTO) {
        Long userId = UserContext.getUserId();
        UserProfile current = getRequiredProfile(userId);
        BeanUtils.copyProperties(userProfileDTO, current);
        userProfileMapper.updateById(current);
        return toUserProfileVO(getRequiredProfile(userId));
    }

    @Override
    public ProfileCompletionVO getCompletion() {
        UserProfile profile = findProfile(UserContext.getUserId());
        List<String> missingFields = new ArrayList<>();
        if (profile == null) {
            return ProfileCompletionVO.builder()
                    .completed(false)
                    .score(0)
                    .missingFields(List.of("education", "major", "grade", "graduationYear",
                            "careerStage", "targetPosition", "targetTime",
                            "dailyStudyHours", "careerGoal"))
                    .build();
        }

        addIfBlank(missingFields, "education", profile.getEducation());
        addIfBlank(missingFields, "major", profile.getMajor());
        addIfBlank(missingFields, "grade", profile.getGrade());
        addIfNull(missingFields, "graduationYear", profile.getGraduationYear());
        addIfBlank(missingFields, "careerStage", profile.getCareerStage());
        addIfBlank(missingFields, "targetPosition", profile.getTargetPosition());
        addIfBlank(missingFields, "targetTime", profile.getTargetTime());
        if (profile.getDailyStudyHours() == null
                || profile.getDailyStudyHours().compareTo(BigDecimal.ZERO) <= 0) {
            missingFields.add("dailyStudyHours");
        }
        addIfBlank(missingFields, "careerGoal", profile.getCareerGoal());

        int completedFields = REQUIRED_FIELD_COUNT - missingFields.size();
        int score = completedFields * 100 / REQUIRED_FIELD_COUNT;
        return ProfileCompletionVO.builder()
                .completed(missingFields.isEmpty())
                .score(score)
                .missingFields(missingFields)
                .build();
    }

    private UserProfile findProfile(Long userId) {
        return userProfileMapper.selectOne(Wrappers.<UserProfile>lambdaQuery()
                .eq(UserProfile::getUserId, userId));
    }

    private UserProfile getRequiredProfile(Long userId) {
        UserProfile profile = findProfile(userId);
        if (profile == null) {
            throw new BusinessException(404, "职业画像不存在");
        }
        return profile;
    }

    private void addIfBlank(List<String> missingFields, String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            missingFields.add(fieldName);
        }
    }

    private void addIfNull(List<String> missingFields, String fieldName, Object value) {
        if (value == null) {
            missingFields.add(fieldName);
        }
    }

    private UserProfileVO toUserProfileVO(UserProfile profile) {
        return UserProfileVO.builder()
                .education(profile.getEducation())
                .major(profile.getMajor())
                .grade(profile.getGrade())
                .graduationYear(profile.getGraduationYear())
                .careerStage(profile.getCareerStage())
                .targetPosition(profile.getTargetPosition())
                .targetCity(profile.getTargetCity())
                .targetTime(profile.getTargetTime())
                .dailyStudyHours(profile.getDailyStudyHours())
                .careerGoal(profile.getCareerGoal())
                .interestDescription(profile.getInterestDescription())
                .build();
    }
}
