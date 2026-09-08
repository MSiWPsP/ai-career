package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.UserProfileDTO;
import com.xucheng.aicareer.vo.ProfileCompletionVO;
import com.xucheng.aicareer.vo.UserProfileVO;

public interface UserProfileService {

    UserProfileVO getCurrentProfile();

    UserProfileVO createProfile(UserProfileDTO userProfileDTO);

    UserProfileVO updateProfile(UserProfileDTO userProfileDTO);

    ProfileCompletionVO getCompletion();
}
