package com.xucheng.aicareer.oss;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {

    String uploadAvatar(MultipartFile file, Long userId);

    void deleteOwnedObject(String fileUrl);
}
