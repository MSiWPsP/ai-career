package com.xucheng.aicareer.oss;

import com.xucheng.aicareer.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AliyunOssServiceTests {

    private AliyunOssService ossService;

    @BeforeEach
    void setUp() {
        AliyunOssProperties properties = new AliyunOssProperties();
        properties.setEndpoint("https://oss-cn-shanghai.aliyuncs.com");
        properties.setBucketName("test-bucket");
        ossService = new AliyunOssService(properties);
    }

    @Test
    void rejectsEmptyAvatarBeforeConnectingToOss() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> ossService.uploadAvatar(file, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请选择需要上传的头像");
    }

    @Test
    void rejectsUnsupportedAvatarTypeBeforeConnectingToOss() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.svg", "image/svg+xml", "<svg/>".getBytes()
        );

        assertThatThrownBy(() -> ossService.uploadAvatar(file, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("头像仅支持 JPG、PNG、WebP 或 GIF 格式");
    }

    @Test
    void rejectsAvatarLargerThanFiveMegabytesBeforeConnectingToOss() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[5 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> ossService.uploadAvatar(file, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("头像大小不能超过5MB");
    }
}
