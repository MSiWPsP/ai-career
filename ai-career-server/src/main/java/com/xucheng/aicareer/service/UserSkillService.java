package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.UserSkillBatchDTO;
import com.xucheng.aicareer.vo.UserSkillVO;

import java.util.List;

public interface UserSkillService {

    List<UserSkillVO> getCurrentUserSkills();

    List<UserSkillVO> replaceCurrentUserSkills(UserSkillBatchDTO userSkillBatchDTO);
}
