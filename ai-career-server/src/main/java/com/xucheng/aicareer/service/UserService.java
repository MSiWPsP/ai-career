package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.UpdateUserDTO;
import com.xucheng.aicareer.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserVO getCurrentUser();

    UserVO updateCurrentUser(UpdateUserDTO updateUserDTO);

    UserVO updateAvatar(MultipartFile file);

    UserVO removeAvatar();
}
