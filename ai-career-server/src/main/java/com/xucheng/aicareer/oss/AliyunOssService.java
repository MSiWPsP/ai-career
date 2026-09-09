package com.xucheng.aicareer.oss;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.model.ObjectMetadata;
import com.xucheng.aicareer.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunOssService implements OssService {

    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final AliyunOssProperties properties;

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        String extension = validateAvatar(file);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String objectName = "avatars/%d/%s/%s.%s".formatted(
                userId,
                datePath,
                UUID.randomUUID().toString().replace("-", ""),
                extension
        );

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        metadata.setCacheControl("public, max-age=31536000");

        OSS ossClient = null;
        try (InputStream inputStream = file.getInputStream()) {
            ossClient = createClient();
            ossClient.putObject(properties.getBucketName(), objectName, inputStream, metadata);
            return buildPublicUrl(objectName);
        } catch (IOException exception) {
            throw new BusinessException(500, "读取头像文件失败");
        } catch (OSSException | ClientException exception) {
            log.error("上传头像到阿里云 OSS 失败，userId={}", userId, exception);
            throw new BusinessException(500, "头像上传失败，请稍后重试");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public void deleteOwnedObject(String fileUrl) {
        String objectName = extractOwnedObjectName(fileUrl);
        if (objectName == null) {
            return;
        }

        OSS ossClient = null;
        try {
            ossClient = createClient();
            ossClient.deleteObject(properties.getBucketName(), objectName);
        } catch (BusinessException | OSSException | ClientException exception) {
            log.warn("删除旧头像失败，objectName={}", objectName, exception);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private String validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择需要上传的头像");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException("头像大小不能超过5MB");
        }
        String contentType = file.getContentType();
        String extension = contentType == null
                ? null
                : ALLOWED_IMAGE_TYPES.get(contentType.toLowerCase(Locale.ROOT));
        if (extension == null) {
            throw new BusinessException("头像仅支持 JPG、PNG、WebP 或 GIF 格式");
        }
        return extension;
    }

    private OSS createClient() throws ClientException {
        try {
            EnvironmentVariableCredentialsProvider credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
            return new OSSClientBuilder().build(normalizedEndpoint(), credentialsProvider);
        } catch (com.aliyuncs.exceptions.ClientException exception) {
            log.error("读取阿里云 OSS 环境变量凭证失败", exception);
            throw new BusinessException(500, "OSS 凭证未配置或不可用");
        }
    }

    private String buildPublicUrl(String objectName) {
        URI endpointUri = URI.create(normalizedEndpoint());
        return "%s://%s.%s/%s".formatted(
                endpointUri.getScheme(),
                properties.getBucketName(),
                endpointUri.getRawAuthority(),
                objectName
        );
    }

    private String extractOwnedObjectName(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        try {
            URI fileUri = URI.create(fileUrl);
            URI endpointUri = URI.create(normalizedEndpoint());
            String ownedHost = properties.getBucketName() + "." + endpointUri.getHost();
            if (!ownedHost.equalsIgnoreCase(fileUri.getHost())) {
                return null;
            }
            String path = fileUri.getPath();
            return StringUtils.hasText(path) && path.length() > 1 ? path.substring(1) : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String normalizedEndpoint() {
        String endpoint = properties.getEndpoint().trim();
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }
}
