package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.LoginDTO;
import com.xucheng.aicareer.dto.RegisterDTO;
import com.xucheng.aicareer.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);
}
