package com.xucheng.aicareer.service;

import com.xucheng.aicareer.entity.User;
import com.xucheng.aicareer.mapper.UserMapper;
import com.xucheng.aicareer.oss.OssService;
import com.xucheng.aicareer.service.impl.UserServiceImpl;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAvatarTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private OssService ossService;

    @InjectMocks
    private UserServiceImpl userService;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void uploadsPersistsAndRemovesPreviousOwnedAvatar() {
        long userId = 10001L;
        String oldAvatar = "https://bucket.example/avatars/10001/old.png";
        String newAvatar = "https://bucket.example/avatars/10001/new.png";
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3}
        );
        UserContext.setUserId(userId);
        when(userMapper.selectById(userId))
                .thenReturn(user(userId, oldAvatar))
                .thenReturn(user(userId, newAvatar));
        when(ossService.uploadAvatar(file, userId)).thenReturn(newAvatar);
        when(userMapper.updateById(argThat((User user) -> newAvatar.equals(user.getAvatar())))).thenReturn(1);

        UserVO result = userService.updateAvatar(file);

        assertThat(result.getAvatar()).isEqualTo(newAvatar);
        var ordered = inOrder(ossService, userMapper);
        ordered.verify(ossService).uploadAvatar(file, userId);
        ordered.verify(userMapper).updateById(argThat((User user) -> newAvatar.equals(user.getAvatar())));
        ordered.verify(ossService).deleteOwnedObject(oldAvatar);
    }

    @Test
    void removesPersistedAvatarAndOwnedOssObject() {
        long userId = 10002L;
        String oldAvatar = "https://bucket.example/avatars/10002/old.png";
        UserContext.setUserId(userId);
        when(userMapper.selectById(userId))
                .thenReturn(user(userId, oldAvatar))
                .thenReturn(user(userId, ""));
        when(userMapper.updateById(argThat((User user) -> "".equals(user.getAvatar())))).thenReturn(1);

        UserVO result = userService.removeAvatar();

        assertThat(result.getAvatar()).isBlank();
        var ordered = inOrder(userMapper, ossService);
        ordered.verify(userMapper).selectById(userId);
        ordered.verify(userMapper).updateById(argThat((User user) -> "".equals(user.getAvatar())));
        ordered.verify(ossService).deleteOwnedObject(oldAvatar);
        ordered.verify(userMapper).selectById(userId);
    }

    private User user(long userId, String avatar) {
        User user = new User();
        user.setId(userId);
        user.setUsername("avatar-test");
        user.setNickname("头像测试");
        user.setAvatar(avatar);
        user.setRole("student");
        return user;
    }
}
