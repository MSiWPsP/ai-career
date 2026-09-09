package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.dto.UpdateUserDTO;
import com.xucheng.aicareer.entity.User;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.UserMapper;
import com.xucheng.aicareer.oss.OssService;
import com.xucheng.aicareer.service.UserService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final OssService ossService;

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

    @Override
    public UserVO updateAvatar(MultipartFile file) {
        Long userId = UserContext.getUserId();
        User currentUser = getRequiredUser(userId);
        String newAvatar = ossService.uploadAvatar(file, userId);

        User update = new User();
        update.setId(userId);
        update.setAvatar(newAvatar);
        try {
            if (userMapper.updateById(update) != 1) {
                throw new BusinessException(500, "头像保存失败");
            }
        } catch (RuntimeException exception) {
            ossService.deleteOwnedObject(newAvatar);
            throw exception;
        }

        if (StringUtils.hasText(currentUser.getAvatar()) && !currentUser.getAvatar().equals(newAvatar)) {
            ossService.deleteOwnedObject(currentUser.getAvatar());
        }
        return toUserVO(getRequiredUser(userId));
    }

    @Override
    public UserVO removeAvatar() {
        Long userId = UserContext.getUserId();
        User currentUser = getRequiredUser(userId);
        if (!StringUtils.hasText(currentUser.getAvatar())) {
            return toUserVO(currentUser);
        }

        User update = new User();
        update.setId(userId);
        update.setAvatar("");
        if (userMapper.updateById(update) != 1) {
            throw new BusinessException(500, "移除头像失败");
        }
        ossService.deleteOwnedObject(currentUser.getAvatar());
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
