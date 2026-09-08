package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.UpdateUserDTO;
import com.xucheng.aicareer.vo.UserVO;

public interface UserService {

    UserVO getCurrentUser();

    UserVO updateCurrentUser(UpdateUserDTO updateUserDTO);
}
