package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.dto.UpdateUserDTO;
import com.xucheng.aicareer.entity.User;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.UserMapper;
import com.xucheng.aicareer.service.UserService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserVO getCurrentUser() {
        return toUserVO(getRequiredUser(UserContext.getUserId()));
    }

    @Override
    @Transactional
    public UserVO updateCurrentUser(UpdateUserDTO updateUserDTO) {
        Long userId = UserContext.getUserId();
        if (updateUserDTO.getNickname() == null && updateUserDTO.getAvatar() == null) {
            throw new BusinessException("没有需要修改的用户信息");
        }
        if (updateUserDTO.getNickname() != null
                && !StringUtils.hasText(updateUserDTO.getNickname())) {
            throw new BusinessException("昵称不能为空");
        }

        User update = new User();
        update.setId(userId);
        if (updateUserDTO.getNickname() != null) {
            update.setNickname(updateUserDTO.getNickname().trim());
        }
        if (updateUserDTO.getAvatar() != null) {
            update.setAvatar(updateUserDTO.getAvatar().trim());
        }
        userMapper.updateById(update);
        return toUserVO(getRequiredUser(userId));
    }

    private User getRequiredUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private UserVO toUserVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }
}
