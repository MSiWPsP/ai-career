package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.dto.LoginDTO;
import com.xucheng.aicareer.dto.RegisterDTO;
import com.xucheng.aicareer.entity.User;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.UserMapper;
import com.xucheng.aicareer.service.AuthService;
import com.xucheng.aicareer.utils.JwtUtils;
import com.xucheng.aicareer.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername().trim();
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }

        return LoginVO.builder()
                .token(jwtUtils.generateToken(user.getId()))
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        String username = registerDTO.getUsername().trim();
        boolean exists = userMapper.exists(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username));
        if (exists) {
            throw new BusinessException(409, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname().trim());
        user.setRole("student");
        user.setStatus(1);
        user.setIsDeleted(0);
        userMapper.insert(user);
    }
}
